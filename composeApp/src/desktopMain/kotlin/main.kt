import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.session.DesktopSessionController
import app.ui.media.VlcSupport

fun main() {
    // Must run before any vlcj component is touched (including ones
    // indirectly referenced from Compose).
    VlcSupport.initialize()

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

        if (uiState.displayWindowVisible) {
            Window(
                onCloseRequest = controller::hideDisplayWindow,
                title = "sick - display",
            ) {
                SharedDisplayApp(uiState, controller.audioPlayback, onMediaFinished = controller::mediaFinished)
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "sick - host",
        ) {
            HostApp(controller)
        }
    }
}
