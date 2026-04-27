package com.sick.engine

import com.sick.event.TimerExpired
import com.sick.event.TimerTick
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameTimer(
    private val engine: GameEngine,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start(seconds: Int, offsetSeconds: Int = 0) {
        stop()
        job = scope.launch {
            if (offsetSeconds > 0) delay(offsetSeconds * 1000L)
            repeat(seconds) {
                delay(1000)
                engine.process(TimerTick)
                if (engine.state.timerRemaining <= 0) {
                    engine.process(TimerExpired)
                    return@launch
                }
            }
            engine.process(TimerExpired)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
