package com.sick.com.sick.siq.reader

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SiqExtractorLimitsTest {

    @Test
    fun `rejects archives with too many entries`() {
        val archive = createZip("one.txt" to "1", "two.txt" to "2")

        assertRejected(archive, SiqExtractionLimits(maxEntries = 1))
    }

    @Test
    fun `rejects an entry larger than configured limit`() {
        val archive = createZip("large.txt" to "12345")

        assertRejected(archive, SiqExtractionLimits(maxEntryBytes = 4))
    }

    @Test
    fun `rejects total extracted content larger than configured limit`() {
        val archive = createZip("one.txt" to "1234", "two.txt" to "5678")

        assertRejected(archive, SiqExtractionLimits(maxEntryBytes = 10, maxTotalBytes = 7))
    }

    @Test
    fun `rejects suspicious compression ratio`() {
        val archive = createZip("compressed.txt" to "A".repeat(8_192))

        assertRejected(archive, SiqExtractionLimits(maxCompressionRatio = 2.0))
    }

    private fun assertRejected(archive: java.nio.file.Path, limits: SiqExtractionLimits) {
        assertFailsWith<IllegalArgumentException> {
            SiqExtractor(archive.toString(), System.getProperty("java.io.tmpdir"), limits).extract()
        }
    }

    private fun createZip(vararg entries: Pair<String, String>) =
        Files.createTempFile(createTempDirectory("siq-limits-"), "pack-", ".siq").also { zip ->
            ZipOutputStream(Files.newOutputStream(zip)).use { output ->
                entries.forEach { (name, contents) ->
                    output.putNextEntry(ZipEntry(name))
                    output.write(contents.toByteArray())
                    output.closeEntry()
                }
            }
        }
}
