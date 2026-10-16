import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.session.DesktopSessionController
import app.state.AudioPlaybackState
import app.state.SharedDisplayView
import app.ui.window.HostWindowContent
import app.ui.window.SharedDisplayScreen
import app.ui.theme.LocalPanelSizes
import app.ui.theme.PanelSizes
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
        val state = controller.uiState
        HostWindowContent(controller, state.host, state.session)
    }
}

@Composable
fun SharedDisplayApp(
    view: SharedDisplayView,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
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
        CompositionLocalProvider(LocalPanelSizes provides PanelSizes.SharedDisplay) {
            SharedDisplayScreen(
                view = view,
                audioPlayback = audioPlayback,
                onMediaEnded = onMediaEnded,
            )
        }
    }
}
