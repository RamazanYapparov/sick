package app.ui.media

import java.io.File

/**
 * Normalizes a media URI for vlcj/libVLC playback.
 *
 * - If [raw] is already an absolute URI (has a recognised scheme such as
 *   `file:`, `http:`, etc.) it is returned as-is, **except** `file:/`
 *   (single-slash) is converted to `file:///` (triple-slash) because
 *   libVLC on Windows does not recognise the single-slash form.
 * - Otherwise [raw] is treated as a local file path and converted to a
 *   properly percent-encoded `file:///` URI via [File.toURI].
 */
fun normalizeMediaUri(raw: String): String {
    val containsScheme = raw.contains("://") || raw.startsWith("file:")
    if (containsScheme) {
        // VLC on Windows expects file:///C:/... not file:/C:/...
        if (raw.startsWith("file:") && !raw.startsWith("file://")) {
            return "file:///" + raw.removePrefix("file:/")
        }
        return raw
    }
    // Convert to ASCII URI so non-ASCII chars (e.g. Cyrillic) are percent-encoded
    val fileUri = File(raw).toURI().toASCIIString()
    // File.toURI() produces file:/C:/... on Windows; fix to file:///C:/...
    return if (fileUri.startsWith("file:") && !fileUri.startsWith("file://")) {
        "file:///" + fileUri.removePrefix("file:/")
    } else fileUri
}
