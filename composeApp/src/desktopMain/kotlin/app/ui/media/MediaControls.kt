package app.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import io.github.oshai.kotlinlogging.KotlinLogging
import uk.co.caprica.vlcj.player.base.MediaPlayer

private val logger = KotlinLogging.logger {}

/**
 * Reacts to [paused] changes by pausing or resuming the media player.
 *
 * Both [VideoPlayer] and [AudioPlayer] share this exact logic.
 *
 * @param paused    the current paused state
 * @param playerRef supplier of the current [MediaPlayer] (may be null)
 * @param tag       component name used in debug logs
 */
@Composable
fun PauseEffect(
    paused: Boolean,
    playerRef: () -> MediaPlayer?,
    tag: String,
) {
    LaunchedEffect(paused) {
        val player = playerRef() ?: return@LaunchedEffect
        try {
            if (paused) {
                player.controls().pause()
            } else if (player.status().isPlayable()) {
                player.controls().play()
            }
        } catch (t: Throwable) {
            logger.debug(t) { "$tag paused handler threw" }
        }
    }
}

/**
 * Reacts to [stopSignal] increments by stopping the media player.
 *
 * When [stopSignal] increases relative to the previously handled value
 * (tracked in [lastStopSignal]), the player is stopped and [onStop] is
 * called so callers can clear any component-specific state (e.g. clearing
 * the current video frame or resetting a `playing` flag).
 *
 * @param stopSignal     the current stop-signal counter
 * @param lastStopSignal mutable slot that tracks the last handled value
 * @param playerRef      supplier of the current [MediaPlayer] (may be null)
 * @param tag            component name used in debug logs
 * @param onStop         called once when a new stop signal is detected
 */
@Composable
fun StopSignalEffect(
    stopSignal: Int,
    lastStopSignal: MutableState<Int>,
    playerRef: () -> MediaPlayer?,
    tag: String,
    onStop: () -> Unit = {},
) {
    LaunchedEffect(stopSignal) {
        val prev = lastStopSignal.value
        lastStopSignal.value = stopSignal
        if (stopSignal > prev) {
            onStop()
            try {
                playerRef()?.controls()?.stop()
            } catch (t: Throwable) {
                logger.debug(t) { "$tag stop-signal handler threw" }
            }
        }
    }
}
