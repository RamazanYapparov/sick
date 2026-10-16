package app.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
