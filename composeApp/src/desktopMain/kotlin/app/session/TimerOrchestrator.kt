package app.session

import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.model.Content
import com.sick.model.Question
import com.sick.state.GamePhase

class TimerOrchestrator(
    private val timer: GameTimer,
    private val engine: GameEngine,
) {
    companion object {
        private const val TIMER_OFFSET_SECONDS = 3
    }

    private var mediaTimerPending = false

    fun onPhaseChange(previous: GamePhase, current: GamePhase, wasPaused: Boolean) {
        if (current != GamePhase.ShowingQuestion) {
            mediaTimerPending = false
            timer.stop()
            return
        }

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

    fun onMediaFinished() {
        if (!mediaTimerPending) return
        mediaTimerPending = false
        val state = engine.state
        if (state.timerRemaining > 0 && engine.phase == GamePhase.ShowingQuestion) {
            timer.start(state.timerRemaining)
        }
    }

    fun stop() {
        mediaTimerPending = false
        timer.stop()
    }
}

private fun Question<*>.hasMedia(): Boolean =
    contents.any { it is Content.Media && it.type in setOf(Content.Type.Video, Content.Type.Audio) }
