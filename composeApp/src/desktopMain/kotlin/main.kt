import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.session.DesktopSessionController

fun main() = application {
    val scope = rememberCoroutineScope()
    val controller = remember { DesktopSessionController(scope) }

    DisposableEffect(controller) {
        onDispose {
            controller.dispose()
        }
    }

    val uiState = controller.uiState

    if (uiState.displayWindowVisible) {
        Window(
            onCloseRequest = controller::hideDisplayWindow,
            title = "sick - display",
        ) {
            SharedDisplayApp(uiState, onMediaFinished = controller::mediaFinished)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "sick - host",
    ) {
        HostApp(controller)
    }
}
