package com.sick.com.sick.siq.reader

import java.io.BufferedOutputStream
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.outputStream

data class SiqExtractionLimits(
    val maxEntries: Int = 10_000,
    val maxEntryBytes: Long = 512L * 1024 * 1024,
    val maxTotalBytes: Long = 2L * 1024 * 1024 * 1024,
    val maxCompressionRatio: Double = 200.0,
)

class SiqExtractor(
    private val source: String,
    private val destination: String,
    private val limits: SiqExtractionLimits = SiqExtractionLimits(),
) {
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
            var entryCount = 0
            var totalBytes = 0L
            zf.entries().asSequence().forEach { entry ->
                entryCount++
                require(entryCount <= limits.maxEntries) {
                    "SIQ archive contains too many entries"
                }
                entry.validateDeclaredSize()
                if (entry.hasDirectory) {
                    tempDir.resolve(entry.directoryName).createIfNotExists()
                }
                totalBytes = entry.write(zf, tempDir, totalBytes)
            }
        }
        return tempDir
    }

    private fun ZipEntry.validateDeclaredSize() {
        if (size >= 0) {
            require(size <= limits.maxEntryBytes) { "SIQ entry exceeds size limit: $name" }
        }
        if (size > 0 && compressedSize > 0) {
            require(size.toDouble() / compressedSize <= limits.maxCompressionRatio) {
                "SIQ entry exceeds compression ratio limit: $name"
            }
        }
    }

    private fun ZipEntry.write(file: ZipFile, destination: Path, totalBytesBeforeEntry: Long): Long {
        var entryBytes = 0L
        file.getInputStream(this).use { inputStream ->
            name.takeIf { "/" in name }
                ?.split("/")?.first()
                ?.let {
                    destination.resolve(it).createIfNotExists()
                }
            BufferedOutputStream(destination.resolve(name.decode()).outputStream(StandardOpenOption.CREATE)).use { outputStream ->
                val bytesIn = ByteArray(BUFFER_SIZE)
                var read: Int
                while (inputStream.read(bytesIn).also { read = it } != -1) {
                    entryBytes += read
                    require(entryBytes <= limits.maxEntryBytes) {
                        "SIQ entry exceeds size limit: $name"
                    }
                    require(totalBytesBeforeEntry + entryBytes <= limits.maxTotalBytes) {
                        "SIQ archive exceeds total extraction size limit"
                    }
                    outputStream.write(bytesIn, 0, read)
                }
            }
        }
        return totalBytesBeforeEntry + entryBytes
    }


    private companion object {
        const val BUFFER_SIZE = 4096
        val ZipEntry.hasDirectory: Boolean get() = File.separator in name
        val ZipEntry.directoryName: String get() = name.split(File.separator).first()
        fun Path.createIfNotExists() { if (!exists()) createDirectory() }
        fun String.decode() = URLDecoder.decode(this, "UTF-8")
    }
}
