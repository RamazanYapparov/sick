package app.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.ui.theme.Palette
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.oshai.kotlinlogging.KotlinLogging
import uk.co.caprica.vlcj.player.base.MediaPlayer
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

private fun formatMillis(ms: Double): String {
    val total = (ms / 1000).toInt()
    val min = total / 60
    val sec = total % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

@Composable
fun AudioPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    stopSignal: Int = 0,
    paused: Boolean = false,
    onFinished: () -> Unit = {},
) {
    val normalizedUri = remember(uri) { normalizeMediaUri(uri) }

    val playerRef = remember { AtomicReference<MediaPlayer?>(null) }
    val lastStopSignal = remember { mutableStateOf(stopSignal) }
    var playing by remember { mutableStateOf(true) }
    var totalMs by remember { mutableStateOf(0.0) }
    var currentMs by remember { mutableStateOf(0.0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(normalizedUri) {
        val factory = VlcSupport.get()
        // Use non-embedded MediaPlayer for audio-only playback
        val mediaPlayer = factory.mediaPlayers().newMediaPlayer()
        playerRef.set(mediaPlayer)

        // --- event listener ---
        val listener = createMediaListener(
            tag = "AudioPlayer",
            normalizedUri = normalizedUri,
            onTotalMs = { totalMs = it },
            onCurrentMs = { currentMs = it },
            onErrorMsg = { errorMessage = it },
            onFinished = { playing = false; onFinished() },
            onErrorExtra = { playing = false; onFinished() },
        )
        mediaPlayer.events().addMediaPlayerEventListener(listener)

        try {
            mediaPlayer.media().play(normalizedUri)
        } catch (t: Throwable) {
            logger.error(t) { "AudioPlayer: media().play($uri) failed" }
            errorMessage = "Could not start playback: ${t.message}"
            playing = false
        }

        onDispose {
            try {
                mediaPlayer.events().removeMediaPlayerEventListener(listener)
                mediaPlayer.controls().stop()
                mediaPlayer.release()
            } catch (t: Throwable) {
                logger.debug(t) { "AudioPlayer.dispose: cleanup threw" }
            } finally {
                playerRef.set(null)
            }
        }
    }

    StopSignalEffect(
        stopSignal = stopSignal,
        lastStopSignal = lastStopSignal,
        playerRef = { playerRef.get() },
        tag = "AudioPlayer",
        onStop = { playing = false },
    )

    PauseEffect(
        paused = paused,
        playerRef = { playerRef.get() },
        tag = "AudioPlayer",
    )

    val playerHeight = if (compact) 48.dp else 72.dp
    val labelSize = if (compact) 14.sp else 20.sp
    val timeSize = if (compact) 11.sp else 13.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(playerHeight)
            .background(Palette.MediaBg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = if (compact) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val label = errorMessage ?: if (playing) "\u266B  Playing audio..." else "\u266B  Done"
            Text(
                text = label,
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = if (errorMessage != null) Color.Red else Palette.AccentGold,
            )
            if (totalMs > 0) {
                Text(
                    text = "${formatMillis(currentMs)} / ${formatMillis(totalMs)}",
                    fontSize = timeSize,
                    color = Palette.AccentGold.copy(alpha = 0.7f),
                )
            }
        }
        val progressValue = if (totalMs > 0) {
            (currentMs / totalMs).toFloat().coerceIn(0f, 1f)
        } else 0f
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).background(Palette.MediaTrack),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progressValue).fillMaxHeight()
                    .background(Palette.AccentGold),
            )
        }
    }
}
