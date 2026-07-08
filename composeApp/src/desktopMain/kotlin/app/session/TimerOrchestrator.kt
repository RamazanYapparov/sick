package app.session

import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.GameState
import com.sick.model.Question
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class TimerOrchestrator(
    private val timer: GameTimer,
    private val answerTimer: GameTimer,
    private val engine: GameEngine,
    private val scope: CoroutineScope,
    private val onAnswerShown: () -> Unit,
    private val onQuestionRevealed: () -> Unit,
) {
    companion object {
        private const val TIMER_OFFSET_SECONDS = 3
        private const val ANSWER_REVEAL_MS = 4_000L
        private const val REVEAL_DELAY_MS = 3_000L
    }

    private var mediaTimerPending = false
    val isMediaPending: Boolean get() = mediaTimerPending
    private var answerMediaPending = false
    private var revealJob: Job? = null

    init {
        // Self-driving. The engine notifies this orchestrator on every successful
        // process(event), passing (prevState, currState, prevPhase, currPhase).
        // That covers phase transitions triggered by timer expiry — which used to
        // bypass the orchestrator entirely and leave ShowingAnswer without a
        // scheduled auto-reveal.
        engine.addListener { prev: GameState, _: GameState, prevPhase: GamePhase, newPhase: GamePhase ->
            logger.trace {
                "orchestrator: listener fired $prevPhase -> $newPhase, wasPaused=${prev.isTimerPaused}"
            }
            onPhaseChange(prevPhase, newPhase, prev.isTimerPaused)
        }
    }

    /**
     * React to a phase transition. Driven automatically by the init-block
     * listener above; direct callers should be aware the listener is the
     * source of truth for transition ordering.
     */
    fun onPhaseChange(previous: GamePhase, current: GamePhase, wasPaused: Boolean) {
        logger.debug { "orchestrator: onPhaseChange $previous -> $current, wasPaused=$wasPaused, mediaPending=$mediaTimerPending" }
        if (revealJob?.isActive == true) {
            logger.debug { "orchestrator: cancelling revealJob (was scheduled for $previous)" }
        }
        revealJob?.cancel()
        revealJob = null

        when (current) {
            GamePhase.RevealingQuestion -> {
                scheduleReveal(REVEAL_DELAY_MS, GamePhase.RevealingQuestion, onQuestionRevealed)
                answerTimer.stop()
            }
            GamePhase.ShowingAnswer -> {
                mediaTimerPending = false
                answerMediaPending = false
                timer.stop()
                answerTimer.stop()
                val answer = engine.state.currentQuestion?.answer
                if (answer?.hasMedia() == true) {
                    logger.info { "orchestrator: answer has media, waiting for media to finish before advancing" }
                    answerMediaPending = true
                } else {
                    scheduleReveal(ANSWER_REVEAL_MS, GamePhase.ShowingAnswer, onAnswerShown)
                }
            }
            GamePhase.PlayerAnswering -> {
                // Buzz-in to PlayerAnswering: stop the question timer (it otherwise
                // keeps firing TimerTick events that the engine would reject while
                // in PlayerAnswering) and start the answer timer fresh. start()
                // calls stop() internally, so answerTimer.stop() is implicit.
                timer.stop()
                if (previous == GamePhase.ShowingQuestion) {
                    val state = engine.state
                    val seconds = state.answerTimerRemaining.takeIf { it > 0 } ?: state.answerTimerSeconds
                    logger.info { "orchestrator: starting answer timer with ${seconds}s" }
                    answerTimer.start(seconds, offsetSeconds = 0)
                }
            }
            GamePhase.ShowingQuestion -> {
                val state = engine.state
                answerTimer.stop()
                if (state.isTimerPaused) {
                    timer.stop()
                    return
                }

                if (wasPaused && !mediaTimerPending) {
                    if (state.timerRemaining > 0) timer.start(state.timerRemaining)
                    return
                }

                if (previous != GamePhase.ShowingQuestion && state.timerRemaining > 0) {
                    val fromFreshQuestion = previous == GamePhase.RevealingQuestion
                    val questionHasMedia = state.currentQuestion?.hasMedia() == true
                    val offsetSeconds = if (fromFreshQuestion) TIMER_OFFSET_SECONDS else 0
                    // The question timer must NOT start ticking while the question's media
                    // is still being played. Two paths can set that up:
                    //   (a) fresh question entering from RevealingQuestion with media;
                    //   (b) buzz-in/wrong sent us back to ShowingQuestion while the
                    //       media is still pending (the player answered before the media
                    //       finished). `mediaTimerPending` survives the buzz-in detour
                    //       because the PlayerAnswering arm does not touch it.
                    if (questionHasMedia && (fromFreshQuestion || mediaTimerPending)) {
                        if (!mediaTimerPending) {
                            logger.info { "orchestrator: media pending, timer will start after media finishes" }
                        } else {
                            logger.debug { "orchestrator: media still pending after buzz-in; question timer continues to wait" }
                        }
                        mediaTimerPending = true
                    } else {
                        mediaTimerPending = false
                        // Wrong-with-players-remaining or AnswerTimerExpired-partial:
                        // resume the question timer immediately from its preserved value (offset 0).
                        timer.start(state.timerRemaining, offsetSeconds = offsetSeconds)
                    }
                }
            }
            else -> {
                logger.debug { "orchestrator: phase=$current -- stopping both timers (no scheduled revealJob)" }
                mediaTimerPending = false
                timer.stop()
                answerTimer.stop()
            }
        }
    }

    /**
     * Schedule a coroutine that waits `delayMs` then invokes [action] if the
     * engine is still in [expectedPhase]. Cancelling any previous revealJob is
     * the caller's responsibility (handled in `onPhaseChange`).
     */
    private fun scheduleReveal(delayMs: Long, expectedPhase: GamePhase, action: () -> Unit) {
        logger.debug { "orchestrator: scheduling revealJob (${delayMs}ms) to fire for $expectedPhase" }
        revealJob = scope.launch {
            delay(delayMs)
            if (engine.phase == expectedPhase) {
                logger.info { "orchestrator: revealJob fired for $expectedPhase -- calling action" }
                action()
            } else {
                logger.debug { "orchestrator: revealJob skipped (phase moved to ${engine.phase})" }
            }
        }
    }

    fun onMediaFinished() {
        logger.debug { "orchestrator: onMediaFinished, mediaPending=$mediaTimerPending, answerMediaPending=$answerMediaPending" }
        if (answerMediaPending) {
            answerMediaPending = false
            logger.info { "orchestrator: answer media finished, calling onAnswerShown" }
            onAnswerShown()
            return
        }
        if (!mediaTimerPending) return
        mediaTimerPending = false
        val state = engine.state
        if (state.timerRemaining > 0 && engine.phase == GamePhase.ShowingQuestion) {
            logger.info { "orchestrator: starting timer with ${state.timerRemaining}s remaining" }
            timer.start(state.timerRemaining)
        }
    }

    fun stop() {
        logger.debug { "orchestrator: stopping -- cancelling revealJob and timers" }
        revealJob?.cancel()
        revealJob = null
        mediaTimerPending = false
        answerMediaPending = false
        timer.stop()
        answerTimer.stop()
    }
}

private fun Question<*>.hasMedia(): Boolean =
    contents.any { it is Content.Media && it.type in setOf(Content.Type.Video, Content.Type.Audio) }

private fun Answer.hasMedia(): Boolean =
    this is Answer.Simple && contents.any { it is Content.Media && it.type in setOf(Content.Type.Video, Content.Type.Audio) }
