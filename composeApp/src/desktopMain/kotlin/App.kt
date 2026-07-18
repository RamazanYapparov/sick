import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import app.session.DesktopSessionController
import app.state.DesktopUiState
import app.ui.window.HostWindowContent
import app.ui.window.SharedDisplayScreen
import app.ui.theme.Palette

@Composable
fun HostApp(controller: DesktopSessionController) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Palette.AccentBlue,
            secondary = Palette.AccentYellow,
            surface = Palette.ThemeSurface,
            background = Palette.ThemeBackground,
            onSurface = Palette.ThemeOnSurface,
            onBackground = Palette.ThemeOnBackground,
        )
    ) {
        HostWindowContent(controller, controller.uiState)
    }
}

@Composable
fun SharedDisplayApp(state: DesktopUiState, onMediaFinished: () -> Unit = {}) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Palette.SharedPrimary,
            secondary = Palette.SharedSecondary,
            surface = Palette.ThemeSurface,
            background = Palette.ThemeBackground,
            onSurface = Palette.ThemeOnSurface,
            onBackground = Palette.ThemeOnBackground,
        )
    ) {
        SharedDisplayScreen(state = state, compact = false, onMediaFinished = onMediaFinished)
    }
}
