package app.ui.media

import app.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javafx.application.Platform as JfxPlatform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView

private fun formatMillis(ms: Double): String {
    val total = (ms / 1000).toInt()
    val min = total / 60
    val sec = total % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

private object JavaFxInit {
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
fun VideoPlayer(uri: String, modifier: Modifier = Modifier, stopSignal: Int = 0, paused: Boolean = false, onFinished: () -> Unit = {}) {
    val jfxPanel = remember { JFXPanel() }
    val playerRef = remember { arrayOfNulls<MediaPlayer>(1) }
    var progress by remember { mutableStateOf(0f) }
    var currentMs by remember { mutableStateOf(0.0) }
    var totalMs by remember { mutableStateOf(0.0) }

    LaunchedEffect(stopSignal) {
        if (stopSignal > 0) {
            JfxPlatform.runLater { playerRef[0]?.stop() }
        }
    }

    LaunchedEffect(paused) {
        if (paused) {
            JfxPlatform.runLater { playerRef[0]?.pause() }
        } else {
            JfxPlatform.runLater {
                val player = playerRef[0]
                if (player != null &&
                    player.status != MediaPlayer.Status.STOPPED &&
                    player.status != MediaPlayer.Status.DISPOSED
                ) {
                    player.play()
                }
            }
        }
    }

    DisposableEffect(uri) {
        JavaFxInit.ensureStarted()
        JfxPlatform.runLater {
            val media = runCatching { Media(uri) }.getOrElse { return@runLater }
            val player = MediaPlayer(media).also { playerRef[0] = it }
            player.setOnError { println("VideoPlayer error: ${player.error?.message}") }
            player.setOnEndOfMedia { JfxPlatform.runLater { onFinished() } }
            player.currentTimeProperty().addListener { _, _, newTime ->
                val total = player.totalDuration?.toMillis() ?: 0.0
                if (total > 0) {
                    totalMs = total
                    currentMs = newTime.toMillis()
                    progress = (currentMs / total).toFloat().coerceIn(0f, 1f)
                }
            }
            val view = MediaView(player).apply { isPreserveRatio = true }
            val root = StackPane(view)
            root.widthProperty().addListener  { _, _, w -> view.fitWidth  = w.toDouble() }
            root.heightProperty().addListener { _, _, h -> view.fitHeight = h.toDouble() }
            jfxPanel.scene = Scene(root)
            player.play()
        }
        onDispose {
            JfxPlatform.runLater { playerRef[0]?.dispose(); playerRef[0] = null }
        }
    }

    Box(modifier = modifier) {
        SwingPanel(factory = { jfxPanel }, modifier = Modifier.fillMaxSize())
        if (totalMs > 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0x44FFFFFF)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Palette.AccentGold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatMillis(currentMs),
                        color = Color.White,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatMillis(totalMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
