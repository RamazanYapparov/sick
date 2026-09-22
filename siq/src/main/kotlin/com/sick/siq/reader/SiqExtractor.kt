package com.sick.com.sick.siq.reader

import java.io.BufferedOutputStream
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.outputStream

class SiqExtractor(private val source: String, private val destination: String) {
    private lateinit var tempDir: Path

    init {
        val deleteOnShutdown = true
        if (deleteOnShutdown) {
            Runtime.getRuntime().addShutdownHook(Thread {
                println("Deleting $tempDir and Shutting down...")
                if (!tempDir.toFile().deleteRecursively()) {
                    System.err.println("Could not delete temp directory $tempDir")
                }
            })
        }
    }

    fun extract(): Path {
        tempDir = Files.createTempDirectory("tmp")
        println("Created temp directory $tempDir")
        ZipFile(source).use { zf ->
            zf.entries().asSequence().forEach { entry ->
                entry.write(zf, tempDir)
            }
        }
        return tempDir
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
                val bytesIn = ByteArray(BUFFER_SIZE)
                var read: Int
                while (inputStream.read(bytesIn).also { read = it } != -1) {
                    outputStream.write(bytesIn, 0, read)
                }
            }
        }
    }

    private fun ZipEntry.safeOutputPath(destination: Path): Path {
        val root = destination.toAbsolutePath().normalize()
        val output = root.resolve(name.decode()).normalize()
        require(output.startsWith(root)) { "ZIP entry escapes extraction directory: $name" }
        return output
    }


    private companion object {
        const val BUFFER_SIZE = 4096
        fun String.decode() = URLDecoder.decode(this, "UTF-8")
    }
}
