package com.sick.engine

import com.sick.event.TimerExpired
import com.sick.event.TimerTick
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class GameTimer(
    private val engine: GameEngine,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start(seconds: Int, offsetSeconds: Int = 0) {
        stop()
        logger.info { "timer: start seconds=$seconds offset=$offsetSeconds" }
        job = scope.launch {
            if (offsetSeconds > 0) delay(offsetSeconds * 1000L)
            repeat(seconds) {
                delay(1000)
                val t0 = System.currentTimeMillis()
                engine.process(TimerTick)
                val elapsed = System.currentTimeMillis() - t0
                if (elapsed > 100) {
                    logger.warn { "timer: TimerTick took ${elapsed}ms to process" }
                }
                logger.trace { "timer: tick remaining=${engine.state.timerRemaining}" }
                if (engine.state.timerRemaining <= 0) {
                    engine.process(TimerExpired)
                    logger.info { "timer: expired (reached 0)" }
                    return@launch
                }
            }
            engine.process(TimerExpired)
            logger.info { "timer: expired (repeat done)" }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
