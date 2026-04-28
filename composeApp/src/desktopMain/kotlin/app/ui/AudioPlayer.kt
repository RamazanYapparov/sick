package app.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javafx.application.Platform as JfxPlatform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

private fun formatMillis(ms: Double): String {
    val total = (ms / 1000).toInt()
    val min = total / 60
    val sec = total % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

private object AudioJavaFxInit {
    @Volatile private var started = false

    fun ensureStarted() {
        if (started) return
        synchronized(this) {
            if (started) return
            try { JfxPlatform.startup {} } catch (_: IllegalStateException) {}
            started = true
        }
    }
}

@Composable
fun AudioPlayer(uri: String, modifier: Modifier = Modifier, stopSignal: Int = 0, onFinished: () -> Unit = {}) {
    val playerRef = remember { arrayOfNulls<MediaPlayer>(1) }
    var playing by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var currentMs by remember { mutableStateOf(0.0) }
    var totalMs by remember { mutableStateOf(0.0) }

    LaunchedEffect(stopSignal) {
        if (stopSignal > 0) {
            playing = false
            JfxPlatform.runLater { playerRef[0]?.stop() }
        }
    }

    DisposableEffect(uri) {
        AudioJavaFxInit.ensureStarted()
        // JFXPanel must be instantiated to keep the JavaFX toolkit alive for audio-only playback
        @Suppress("UNUSED_VARIABLE")
        val panel = JFXPanel()
        JfxPlatform.runLater {
            val media = runCatching { Media(uri) }.getOrElse { return@runLater }
            val player = MediaPlayer(media).also { playerRef[0] = it }
            player.setOnError { println("AudioPlayer error: ${player.error?.message}") }
            player.setOnEndOfMedia {
                JfxPlatform.runLater {
                    playing = false
                    onFinished()
                }
            }
            player.currentTimeProperty().addListener { _, _, newTime ->
                val total = player.totalDuration?.toMillis() ?: 0.0
                if (total > 0) {
                    totalMs = total
                    currentMs = newTime.toMillis()
                    progress = (currentMs / total).toFloat().coerceIn(0f, 1f)
                }
            }
            player.play()
        }
        onDispose {
            JfxPlatform.runLater { playerRef[0]?.dispose(); playerRef[0] = null }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1A2A35)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (playing) "♫  Playing audio..." else "♫  Done",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Palette.AccentGold,
            )
            if (totalMs > 0) {
                Text(
                    text = "${formatMillis(currentMs)} / ${formatMillis(totalMs)}",
                    fontSize = 13.sp,
                    color = Palette.AccentGold.copy(alpha = 0.7f),
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF0D1C24)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Palette.AccentGold),
            )
        }
    }
}
