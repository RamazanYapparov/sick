package app.ui.media

import io.github.oshai.kotlinlogging.KotlinLogging
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

private val logger = KotlinLogging.logger {}

/**
 * Creates a [MediaPlayerEventAdapter] with shared media-playback handling.
 *
 * Both [VideoPlayer] and [AudioPlayer] use the same [playing] and
 * [timeChanged] logic.  This factory captures the common parts and lets
 * callers supply the variable behaviour via lambdas.
 *
 * @param tag           component name used in log output ("VideoPlayer" / "AudioPlayer")
 * @param normalizedUri the URI being played (included in error logs)
 * @param onTotalMs     called with the media duration when it becomes known
 * @param onCurrentMs   called with the current playback position on each tick
 * @param onErrorMsg    called to set the user-facing error message
 * @param onFinished    called when playback reaches the end
 * @param onErrorExtra  optional additional action when an error occurs
 *                      (AudioPlayer uses this to clear its `playing` flag)
 */
fun createMediaListener(
    tag: String,
    normalizedUri: String,
    onTotalMs: (Double) -> Unit,
    onCurrentMs: (Double) -> Unit,
    onErrorMsg: (String) -> Unit,
    onFinished: () -> Unit,
    onErrorExtra: ((MediaPlayer) -> Unit)? = null,
): MediaPlayerEventAdapter {
    return object : MediaPlayerEventAdapter() {
        override fun playing(mediaPlayer: MediaPlayer) {
            val len = mediaPlayer.status().length()
            if (len > 0) onTotalMs(len.toDouble())
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            onCurrentMs(newTime.toDouble())
            val len = mediaPlayer.status().length()
            if (len > 0) onTotalMs(len.toDouble())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            onFinished()
        }

        override fun error(mediaPlayer: MediaPlayer) {
            val state = mediaPlayer.status().state()
            val mrl = try {
                mediaPlayer.media().info().mrl()
            } catch (_: Throwable) {
                normalizedUri
            }
            logger.warn { "$tag: vlc error event (state=$state, uri=$mrl, requested=$normalizedUri)" }
            onErrorMsg("Playback error ($state)")
            onErrorExtra?.invoke(mediaPlayer)
        }
    }
}
