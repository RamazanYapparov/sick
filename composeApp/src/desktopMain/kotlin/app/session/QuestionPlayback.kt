package app.session

import app.state.QuestionDisplayItem
import app.state.displayContents
import app.state.isPlayable
import com.sick.engine.EngineTransition
import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.event.AnswerShown
import com.sick.event.AnswerTimerExpired
import com.sick.event.AnswerTimerTick
import com.sick.event.GameEvent
import com.sick.event.PauseTimer
import com.sick.event.QuestionRevealed
import com.sick.event.ResumeTimer
import com.sick.event.TimerExpired
import com.sick.event.TimerTick
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.GameState
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

/** What the windows render of the question's or answer's content and media. */
data class PlaybackView(
    /**
     * The content on screen, set once when its queue starts: the question's
     * in ShowingQuestion and PlayerAnswering, the answer's in ShowingAnswer,
     * otherwise empty.
     */
    val items: List<QuestionDisplayItem> = emptyList(),
    /** The one media shown right now, or null when none is; its index counts the playable [items]. */
    val media: MediaSlot? = null,
    val canSkipMedia: Boolean = false,
    val canReplayMedia: Boolean = false,
)

/**
 * The media at [index] among the playable items of the content on screen (the
 * question's, or the answer's in ShowingAnswer). A player is created per
 * ([generation], [index]) and reports its end with exactly those two values.
 */
data class MediaSlot(
    val index: Int,
    val generation: Int,
    val paused: Boolean,
)

/**
 * Owns the timing of a question: the question and answer timers, the reveal and
 * answer delays, and the media queue (Media Hold, Media Replay, pause on Buzz).
 *
 * Not thread-safe: every call, including [onTransition], must come from one
 * thread, the one [scope] runs on. [onTransition] never calls `engine.process`
 * synchronously, so it is safe to call from an engine listener.
 */
class QuestionPlayback(
    private val engine: GameEngine,
    private val scope: CoroutineScope,
) {
    private companion object {
        const val TIMER_OFFSET_SECONDS = 3
        const val REVEAL_DELAY_MS = 3_000L
        const val ANSWER_DELAY_MS = 4_000L
    }

    private data class Track(val index: Int, val ended: Boolean)

    private val questionTimer = GameTimer(
        onTick = { engine.process(TimerTick) },
        onExpired = { engine.process(TimerExpired) },
        scope = scope,
        name = "question-timer",
    )
    private val answerTimer = GameTimer(
        onTick = { engine.process(AnswerTimerTick) },
        onExpired = { engine.process(AnswerTimerExpired) },
        scope = scope,
        name = "answer-timer",
    )
    private var delayJob: Job? = null

    private var phase: GamePhase = engine.phase
    private var state: GameState = engine.state
    private var items: List<QuestionDisplayItem> = emptyList()
    private var queueSize = 0
    private var track: Track? = null
    private var generation = 0
    private var mediaHold = false
    private var closed = false

    val view: PlaybackView
        get() {
            val current = track
            val paused = phase == GamePhase.PlayerAnswering || state.isTimerPaused
            return PlaybackView(
                items = items,
                media = current?.let { MediaSlot(it.index, generation, paused = paused && !it.ended) },
                canSkipMedia = phase == GamePhase.ShowingQuestion && current?.ended == false,
                canReplayMedia = phase == GamePhase.ShowingQuestion && !state.isTimerPaused && !mediaHold && queueSize > 0,
            )
        }

    fun onTransition(transition: EngineTransition) {
        if (closed) return
        phase = transition.phase
        state = transition.state
        if (transition.phase != transition.previousPhase) {
            enter(transition.previousPhase)
            return
        }
        when (transition.event) {
            PauseTimer -> {
                questionTimer.stop()
                answerTimer.stop()
            }
            ResumeTimer ->
                if (phase == GamePhase.PlayerAnswering) answerTimer.start(state.answerTimerRemaining)
                else startQuestionTimerIfDue(offsetSeconds = 0)
            else -> Unit
        }
    }

    /** A player at ([generation], [index]) finished, failed, or could not start. Stale and duplicate reports are ignored. */
    fun onMediaEnded(generation: Int, index: Int) {
        val current = track ?: return
        if (closed || generation != this.generation || index != current.index || current.ended) return
        if (index + 1 < queueSize) {
            track = Track(index + 1, ended = false)
            return
        }
        track = current.copy(ended = true)
        if (phase == GamePhase.ShowingAnswer) {
            engine.process(AnswerShown)
        } else if (mediaHold) {
            endMediaHold()
        }
    }

    /** Stops the question's media; during the Media Hold this starts the question timer. */
    fun skipMedia() {
        if (closed || !view.canSkipMedia) return
        track = null
        if (mediaHold) endMediaHold()
    }

    /** Media Replay: plays the question's media again from the first one while the question timer runs. */
    fun replayMedia() {
        if (closed || !view.canReplayMedia) return
        generation++
        track = Track(0, ended = false)
    }

    /** Stops the timers and pending delays; every later call is ignored. */
    fun close() {
        closed = true
        delayJob?.cancel()
        questionTimer.stop()
        answerTimer.stop()
    }

    private fun enter(previousPhase: GamePhase) {
        logger.debug { "playback: $previousPhase -> $phase" }
        delayJob?.cancel()
        delayJob = null
        when (phase) {
            GamePhase.RevealingQuestion -> {
                stopTimers()
                clearMedia()
                after(REVEAL_DELAY_MS, QuestionRevealed)
            }
            GamePhase.ShowingQuestion -> {
                answerTimer.stop()
                if (previousPhase == GamePhase.RevealingQuestion) {
                    startQueue(state.currentQuestion?.contents.orEmpty())
                    mediaHold = queueSize > 0
                    startQuestionTimerIfDue(offsetSeconds = TIMER_OFFSET_SECONDS)
                } else {
                    startQuestionTimerIfDue(offsetSeconds = 0)
                }
            }
            GamePhase.PlayerAnswering -> {
                questionTimer.stop()
                if (!state.isTimerPaused) answerTimer.start(state.answerTimerRemaining)
            }
            GamePhase.ShowingAnswer -> {
                stopTimers()
                mediaHold = false
                startQueue((state.currentQuestion?.answer as? Answer.Simple)?.contents.orEmpty())
                if (queueSize == 0) after(ANSWER_DELAY_MS, AnswerShown)
            }
            else -> {
                stopTimers()
                clearMedia()
            }
        }
    }

    private fun startQueue(contents: List<Content>) {
        generation++
        items = displayContents(contents)
        queueSize = items.count { it.isPlayable() }
        track = if (queueSize > 0) Track(0, ended = false) else null
    }

    private fun clearMedia() {
        items = emptyList()
        queueSize = 0
        track = null
        mediaHold = false
    }

    private fun endMediaHold() {
        mediaHold = false
        startQuestionTimerIfDue(offsetSeconds = 0)
    }

    private fun startQuestionTimerIfDue(offsetSeconds: Int) {
        if (phase != GamePhase.ShowingQuestion || state.isTimerPaused || mediaHold || state.timerRemaining <= 0) return
        questionTimer.start(state.timerRemaining, offsetSeconds)
    }

    private fun stopTimers() {
        questionTimer.stop()
        answerTimer.stop()
    }

    /** The engine rejects [event] if the phase moved on meanwhile; entering a new phase also cancels this. */
    private fun after(delayMs: Long, event: GameEvent) {
        delayJob = scope.launch {
            delay(delayMs)
            engine.process(event)
        }
    }
}
