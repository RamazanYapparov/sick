package com.sick.siq

import com.sick.model.Package
import com.sick.siq.mapper.PackFiles
import com.sick.siq.mapper.toDomain
import com.sick.siq.reader.extractSiq
import com.sick.siq.xml.parser.Parser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * A pack extracted to a temporary folder. The local media paths in [pack]
 * point into that folder and stay valid until [close] deletes it.
 */
class OpenedPack internal constructor(val pack: Package, private val folder: Path) : AutoCloseable {
    override fun close() = ExtractedFolders.delete(folder)
}

object SiqPack {
    /**
     * Extracts the `.siq` file at [source], maps it, finds every media file and keeps
     * only what the simple mode plays. Questions it cannot play are left out and logged.
     * Fails when nothing playable remains; the folder is then deleted right away.
     */
    fun open(source: Path): OpenedPack {
        val folder = ExtractedFolders.create()
        try {
            extractSiq(source, folder)
            val pack = Parser().parse(folder).toDomain(PackFiles(folder)).sanitizeForSimpleMode()
            return OpenedPack(pack, folder)
        } catch (e: Throwable) {
            ExtractedFolders.delete(folder)
            throw e
        }
    }
}

/**
 * Temporary folders packs are extracted to. A closed pack's folder is deleted at once;
 * whatever is still on disk at JVM exit (an unclosed pack, a file a media player held
 * open while its pack closed) is deleted by one shutdown hook.
 */
private object ExtractedFolders {
    private val logger = KotlinLogging.logger {}
    private val leftovers: MutableSet<Path> = ConcurrentHashMap.newKeySet()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { leftovers.forEach(::tryDelete) })
    }

    fun create(): Path = Files.createTempDirectory("sick-pack-").also(leftovers::add)

    fun delete(folder: Path) {
        if (tryDelete(folder)) leftovers.remove(folder)
        else logger.warn { "Could not delete $folder yet; retrying at exit" }
    }

    private fun tryDelete(folder: Path): Boolean = folder.toFile().deleteRecursively()
}
