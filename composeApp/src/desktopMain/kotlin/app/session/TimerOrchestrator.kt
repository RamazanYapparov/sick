package app.session

import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.model.Content
import com.sick.model.Question
import com.sick.state.GamePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerOrchestrator(
    private val timer: GameTimer,
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
    private var revealJob: Job? = null

    fun onPhaseChange(previous: GamePhase, current: GamePhase, wasPaused: Boolean) {
        revealJob?.cancel()
        revealJob = null

        when (current) {
            GamePhase.RevealingQuestion -> {
                revealJob = scope.launch {
                    delay(REVEAL_DELAY_MS)
                    if (engine.phase == GamePhase.RevealingQuestion) onQuestionRevealed()
                }
            }
            GamePhase.ShowingAnswer -> {
                mediaTimerPending = false
                timer.stop()
                revealJob = scope.launch {
                    delay(ANSWER_REVEAL_MS)
                    if (engine.phase == GamePhase.ShowingAnswer) onAnswerShown()
                }
            }
            GamePhase.ShowingQuestion -> {
                val state = engine.state
                if (state.isTimerPaused) {
                    timer.stop()
                    return
                }

                if (wasPaused && !mediaTimerPending) {
                    if (state.timerRemaining > 0) timer.start(state.timerRemaining)
                    return
                }

                if (previous != GamePhase.ShowingQuestion && state.timerRemaining > 0) {
                    if (state.currentQuestion?.hasMedia() == true) {
                        mediaTimerPending = true
                    } else {
                        mediaTimerPending = false
                        timer.start(state.timerRemaining, offsetSeconds = TIMER_OFFSET_SECONDS)
                    }
                }
            }
            else -> {
                mediaTimerPending = false
                timer.stop()
            }
        }
    }

    fun onMediaFinished() {
        if (!mediaTimerPending) return
        mediaTimerPending = false
        val state = engine.state
        if (state.timerRemaining > 0 && engine.phase == GamePhase.ShowingQuestion) {
            timer.start(state.timerRemaining)
        }
    }

    fun stop() {
        revealJob?.cancel()
        revealJob = null
        mediaTimerPending = false
        timer.stop()
    }
}

private fun Question<*>.hasMedia(): Boolean =
    contents.any { it is Content.Media && it.type in setOf(Content.Type.Video, Content.Type.Audio) }
