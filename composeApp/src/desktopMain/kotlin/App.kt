import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.DesktopSessionController
import app.DesktopUiState
import app.HostWindowContent
import app.SharedDisplayScreen

@Composable
fun HostApp(controller: DesktopSessionController) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF235A73),
            secondary = Color(0xFFE8B23A),
            surface = Color(0xFFF6F1E8),
            background = Color(0xFFEDE3D1),
        )
    ) {
        HostWindowContent(controller, controller.uiState)
    }
}

@Composable
fun SharedDisplayApp(state: DesktopUiState, onVideoFinished: () -> Unit = {}) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF184A45),
            secondary = Color(0xFFE5B14C),
            surface = Color(0xFF162C36),
            background = Color(0xFF0E1A21),
            onSurface = Color(0xFFF7F4ED),
            onBackground = Color(0xFFF7F4ED),
        )
    ) {
        SharedDisplayScreen(state = state, compact = false, onVideoFinished = onVideoFinished)
    }
}
