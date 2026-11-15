import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import app.session.DesktopSessionController
import app.ui.media.VlcSupport
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun main() {
    // Must run before any vlcj component is touched (including ones
    // indirectly referenced from Compose). Windows builds carry libVLC in the
    // app resources (composeApp/build.gradle.kts); elsewhere the installed VLC is used.
    val bundledVlc = System.getProperty("compose.application.resources.dir")
        ?.let { Path.of(it, "vlc") }
        ?.takeIf { it.isDirectory() }
    VlcSupport.initialize(bundledVlc)

    application {
        val scope = rememberCoroutineScope()
        val controller = remember { DesktopSessionController(scope) }

        DisposableEffect(controller) {
            onDispose {
                controller.dispose()
                VlcSupport.shutdown()
            }
        }

        val uiState = controller.uiState

        // The Shared Display stays open for the whole session: only its Audio Player
        // reports the end of audio, so closing it would stall every Media Hold.
        val displayState = rememberWindowState()
        Window(
            onCloseRequest = { displayState.isMinimized = true },
            state = displayState,
            title = "sick - display",
        ) {
            SharedDisplayApp(
                uiState.sharedDisplay,
                controller.audioPlayback,
                onMediaEnded = controller::mediaEnded,
            )
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "sick - host",
        ) {
            HostApp(controller)
        }
    }
}
