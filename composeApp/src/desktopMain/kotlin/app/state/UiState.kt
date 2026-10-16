package app.state

import app.session.ScannedPackInfo

/**
 * The snapshot the windows render. The host window reads [host] and [session],
 * the Shared Display only [sharedDisplay].
 */
data class DesktopUiState(
    val host: HostView,
    val sharedDisplay: SharedDisplayView,
    val session: HostSession,
    val displayWindowVisible: Boolean = true,
)

/** The host's session fields that do not come from the game: pack, buzzer URL, messages, pack browser. */
data class HostSession(
    val serverUrl: String,
    val loadedPackPath: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showPackBrowser: Boolean = false,
    val scannedPacks: List<ScannedPackInfo> = emptyList(),
    val isScanningPacks: Boolean = false,
)

/** Reaction Time as seconds with millisecond precision, e.g. `2.340 s`. */
fun formatReactionTime(millis: Long): String =
    "${millis / 1000}.${(millis % 1000).toString().padStart(3, '0')} s"
