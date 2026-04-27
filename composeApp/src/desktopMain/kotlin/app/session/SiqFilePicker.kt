package app.session

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path

fun pickSiqFile(): Path? {
    val dialog = FileDialog(null as Frame?, "Load SIQ pack", FileDialog.LOAD).apply {
        filenameFilter = FilenameFilter { _, name -> name.endsWith(".siq", ignoreCase = true) }
    }
    dialog.isVisible = true

    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return Path.of(dir, file)
}
