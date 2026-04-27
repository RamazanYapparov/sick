import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.session.DesktopSessionController
import app.state.DesktopUiState
import app.ui.HostWindowContent
import app.ui.Palette
import app.ui.SharedDisplayScreen

@Composable
fun HostApp(controller: DesktopSessionController) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Palette.AccentBlue,
            secondary = Palette.AccentYellow,
            surface = Color(0xFFF6F1E8),
            background = Color(0xFFEDE3D1),
        )
    ) {
        HostWindowContent(controller, controller.uiState)
    }
}

@Composable
fun SharedDisplayApp(state: DesktopUiState, onMediaFinished: () -> Unit = {}) {
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
        SharedDisplayScreen(state = state, compact = false, onMediaFinished = onMediaFinished)
    }
}
