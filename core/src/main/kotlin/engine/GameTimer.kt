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
    private val onTick: () -> Unit,
    private val onExpired: () -> Unit,
    private val scope: CoroutineScope,
    /** Label used in every log line so multiple timers in the same JVM are distinguishable. */
    private val name: String = "timer",
) {
    private var job: Job? = null

    fun start(seconds: Int, offsetSeconds: Int = 0) {
        stop()
        logger.info { "$name: start seconds=$seconds offset=$offsetSeconds" }
        job = scope.launch {
            if (offsetSeconds > 0) delay(offsetSeconds * 1000L)
            repeat(seconds) { idx ->
                delay(1000)
                logger.trace { "$name: tick #${idx + 1}/$seconds" }
                val t0 = System.currentTimeMillis()
                onTick()
                val elapsed = System.currentTimeMillis() - t0
                if (elapsed > 100) {
                    logger.warn { "$name: tick #${idx + 1} took ${elapsed}ms to process" }
                }
            }
            onExpired()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

