package com.sick.siq.mapper

import com.sick.model.Content
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/** Media files of a pack extracted to [root]: images in `Images`, audio in `Audio`, video in `Video`. */
internal class PackFiles(root: Path) {
    private val root = root.toAbsolutePath().normalize()

    /** The file [ref] names for media of [type]; null when it does not exist or lies outside its folder. */
    fun find(type: Content.Type, ref: String): Path? {
        val folder = root.resolve(folderOf(type))
        val file = try {
            folder.resolve(ref).normalize()
        } catch (e: InvalidPathException) {
            return null
        }
        return file.takeIf { it.startsWith(folder) && it.isRegularFile() }
    }

    companion object {
        fun folderOf(type: Content.Type) = when (type) {
            Content.Type.Image -> "Images"
            Content.Type.Audio -> "Audio"
            Content.Type.Video -> "Video"
        }
    }
}
