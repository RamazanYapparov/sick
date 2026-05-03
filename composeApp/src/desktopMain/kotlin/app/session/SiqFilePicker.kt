package app.session

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun pickSiqFile(): Path? {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> pickMac()
        os.contains("win") -> pickWindows()
        else -> pickLinux()
    }
}

private fun pickMac(): Path? {
    val dialog = FileDialog(null as Frame?, "Load SIQ pack", FileDialog.LOAD).apply {
        filenameFilter = FilenameFilter { _, name -> name.endsWith(".siq", ignoreCase = true) }
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return Path.of(dir, file)
}

private fun pickWindows(): Path? {
    val script = """
        Add-Type -AssemblyName System.Windows.Forms
        ${'$'}d = New-Object System.Windows.Forms.OpenFileDialog
        ${'$'}d.Filter = 'SIQ files (*.siq)|*.siq'
        ${'$'}d.Title = 'Load SIQ pack'
        if (${'$'}d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { Write-Output ${'$'}d.FileName }
    """.trimIndent()
    return try {
        val proc = ProcessBuilder("powershell", "-NonInteractive", "-Command", script)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        if (output.isBlank()) null else Path.of(output)
    } catch (_: Exception) {
        null
    }
}

private fun pickLinux(): Path? {
    return tryKdialog() ?: tryZenity() ?: pickSwing()
}

private fun tryKdialog(): Path? {
    return try {
        val proc = ProcessBuilder("kdialog", "--getopenfilename", ".", "*.siq")
            .redirectErrorStream(false)
            .start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        val exit = proc.waitFor()
        if (exit == 0 && output.isNotBlank()) Path.of(output) else null
    } catch (_: Exception) {
        null
    }
}

private fun tryZenity(): Path? {
    return try {
        val proc = ProcessBuilder("zenity", "--file-selection", "--file-filter=*.siq")
            .redirectErrorStream(false)
            .start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        val exit = proc.waitFor()
        if (exit == 0 && output.isNotBlank()) Path.of(output) else null
    } catch (_: Exception) {
        null
    }
}

private fun pickSwing(): Path? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Load SIQ pack"
        fileFilter = FileNameExtensionFilter("SIQ files (*.siq)", "siq")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFile?.toPath()
}
