package com.sick.siq.reader

import java.io.BufferedOutputStream
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.outputStream

/** Unzips the pack [source] into [destination]; an entry that would land outside [destination] fails the extraction. */
internal fun extractSiq(source: Path, destination: Path) {
    ZipFile(source.toFile()).use { zf ->
        zf.entries().asSequence().forEach { entry ->
            entry.write(zf, destination)
        }
    }
}

private fun ZipEntry.write(file: ZipFile, destination: Path) {
    val outputPath = safeOutputPath(destination)
    if (isDirectory) {
        Files.createDirectories(outputPath)
        return
    }

    outputPath.parent?.let(Files::createDirectories)
    file.getInputStream(this).use { inputStream ->
        BufferedOutputStream(outputPath.outputStream(StandardOpenOption.CREATE)).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

private fun ZipEntry.safeOutputPath(destination: Path): Path {
    val root = destination.toAbsolutePath().normalize()
    val output = root.resolve(name.decode()).normalize()
    require(output.startsWith(root)) { "ZIP entry escapes extraction directory: $name" }
    return output
}

/** SIQuester percent-encodes entry names but writes `+` as is, so `+` must not become a space. */
private fun String.decode(): String = URLDecoder.decode(replace("+", "%2B"), Charsets.UTF_8)
