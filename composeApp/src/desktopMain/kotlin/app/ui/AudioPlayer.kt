package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javafx.application.Platform as JfxPlatform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

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
fun AudioPlayer(uri: String, modifier: Modifier = Modifier, onFinished: () -> Unit = {}) {
    val playerRef = remember { arrayOfNulls<MediaPlayer>(1) }
    var playing by remember { mutableStateOf(true) }

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
            player.play()
        }
        onDispose {
            JfxPlatform.runLater { playerRef[0]?.dispose(); playerRef[0] = null }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1A2A35)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (playing) "♫  Playing audio..." else "♫  Done",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Palette.AccentGold,
            modifier = Modifier.padding(16.dp),
        )
    }
}
