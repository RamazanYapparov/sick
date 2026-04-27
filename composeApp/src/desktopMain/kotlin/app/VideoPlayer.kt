package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform as JfxPlatform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView

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
fun VideoPlayer(uri: String, modifier: Modifier = Modifier, onFinished: () -> Unit = {}) {
    val jfxPanel = remember { JFXPanel() }
    val playerRef = remember { arrayOfNulls<MediaPlayer>(1) }

    DisposableEffect(uri) {
        JavaFxInit.ensureStarted()
        JfxPlatform.runLater {
            val media = runCatching { Media(uri) }.getOrElse { return@runLater }
            val player = MediaPlayer(media).also { playerRef[0] = it }
            player.setOnError { println("VideoPlayer error: ${player.error?.message}") }
            player.setOnEndOfMedia { JfxPlatform.runLater { onFinished() } }
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

    SwingPanel(factory = { jfxPanel }, modifier = modifier)
}
