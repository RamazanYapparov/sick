package com.sick.com.sick.siq.reader

import java.io.BufferedOutputStream
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
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
        tempDir = Files.createTempDirectory(Paths.get(destination), "tmp")
        ZipFile(source).use { zf ->
            zf.entries().asSequence().forEach { entry ->
                if (entry.hasDirectory) {
                    tempDir.resolve(entry.directoryName).createIfNotExists()
                }
                entry.write(zf, tempDir)
            }
        }
        return tempDir
    }

    private fun ZipEntry.write(file: ZipFile, destination: Path) {
        file.getInputStream(this).use { inputStream ->
            BufferedOutputStream(destination.resolve(name.decode()).outputStream()).use { outputStream ->
                val bytesIn = ByteArray(BUFFER_SIZE)
                var read: Int
                while (inputStream.read(bytesIn).also { read = it } != -1) {
                    outputStream.write(bytesIn, 0, read)
                }
            }
        }
    }


    private companion object {
        const val BUFFER_SIZE = 4096
        val ZipEntry.hasDirectory: Boolean get() = File.separator in name
        val ZipEntry.directoryName: String get() = name.split(File.separator).first()
        fun Path.createIfNotExists() { if (!exists()) createDirectory() }
        fun String.decode() = URLDecoder.decode(this, "UTF-8")
    }
}