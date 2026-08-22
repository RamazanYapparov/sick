package com.sick.com.sick.siq.reader

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SiqExtractorSecurityTest {

    @Test
    fun `extracts nested entries inside the temporary directory`() {
        val source = createZip("Images/nested/picture.txt" to "safe")

        val extracted = SiqExtractor(source.toString(), System.getProperty("java.io.tmpdir")).extract()

        assertEquals("safe", extracted.resolve("Images/nested/picture.txt").readText())
    }

    @Test
    fun `rejects parent traversal entry`() {
        val source = createZip("../outside.txt" to "unsafe")

        assertFailsWith<IllegalArgumentException> {
            SiqExtractor(source.toString(), System.getProperty("java.io.tmpdir")).extract()
        }
    }

    @Test
    fun `rejects URL encoded parent traversal entry`() {
        val source = createZip("%2e%2e%2foutside.txt" to "unsafe")

        assertFailsWith<IllegalArgumentException> {
            SiqExtractor(source.toString(), System.getProperty("java.io.tmpdir")).extract()
        }
    }

    private fun createZip(vararg entries: Pair<String, String>) =
        Files.createTempFile(createTempDirectory("siq-source-"), "pack-", ".siq").also { zip ->
            ZipOutputStream(Files.newOutputStream(zip)).use { output ->
                entries.forEach { (name, contents) ->
                    output.putNextEntry(ZipEntry(name))
                    output.write(contents.toByteArray())
                    output.closeEntry()
                }
            }
        }
}
