package com.sick.siq

import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Content.Media.Source
import com.sick.model.Package
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SiqPackTest {
    private val opened = mutableListOf<OpenedPack>()

    @AfterTest
    fun closePacks() = opened.forEach(OpenedPack::close)

    @Test
    fun `media refs point at the extracted files in the folder of their type`() {
        val pack = open(
            question(100, image("pic one.png") + item("voice", "a+b.mp3") + item("video", "@clip.mp4")),
            files = mapOf("Images/pic%20one.png" to "image", "Audio/a+b.mp3" to "voice", "Video/clip.mp4" to "video"),
        )

        val media = pack.questions().single().contents.map { assertIs<Content.Media>(it) }

        assertEquals(listOf(Content.Type.Image, Content.Type.Audio, Content.Type.Video), media.map { it.type })
        val paths = media.map { assertIs<Source.Local>(it.source).path }
        assertEquals(listOf("Images", "Audio", "Video"), paths.map { it.parent.fileName.toString() })
        assertEquals(listOf("image", "voice", "video"), paths.map { it.readText() })
    }

    @Test
    fun `a question with a media file missing from the pack is left out`() {
        val pack = open(
            question(100, image("here.png"), answerItems = item("audio", "here.mp3")),
            question(200, image("gone.png")),
            question(300, text("Q"), answerItems = item("audio", "gone.mp3")),
            question(400, image("../content.xml")),
            files = mapOf("Images/here.png" to "i", "Audio/here.mp3" to "a"),
        )

        assertEquals(listOf(100), pack.questions().map { it.price })
    }

    @Test
    fun `remote media stay URLs and a malformed URL leaves its question out`() {
        val pack = open(
            question(100, item("image", "http://media.test/pic.png", isRef = false)),
            question(200, item("image", "not a url", isRef = false)),
        )

        val question = pack.questions().single()
        assertEquals(100, question.price)
        val media = assertIs<Content.Media>(question.contents.single())
        assertEquals(Content.Type.Image, media.type)
        // URL.equals resolves host names, so compare the text.
        assertEquals("http://media.test/pic.png", assertIs<Source.Remote>(media.source).url.toString())
    }

    @Test
    fun `a Point Answer is marked on the first image of the question`() {
        val pack = open(
            question(
                100,
                text("Where?") + image("first.png") + image("second.png"),
                answer = "0.66,0.5,1.79",
                params = point(deviation = "0.11"),
            ),
            files = mapOf("Images/first.png" to "1", "Images/second.png" to "2"),
        )

        val question = pack.questions().single()
        assertEquals(
            Answer.Point(image = question.contents[1] as Content.Media, x = 0.66, y = 0.5, aspectRatio = 1.79, deviation = 0.11),
            question.answer,
        )
    }

    @Test
    fun `a Point Answer without aspect ratio or deviation is square and exact`() {
        val pack = open(
            question(100, image("map.png"), answer = "0.2, 0.8", params = point(deviation = null)),
            files = mapOf("Images/map.png" to "m"),
        )

        val question = pack.questions().single()
        assertEquals(
            Answer.Point(image = question.contents.single() as Content.Media, x = 0.2, y = 0.8, aspectRatio = 1.0, deviation = 0.0),
            question.answer,
        )
    }

    @Test
    fun `a malformed Point Answer falls back to a simple text answer`() {
        listOf("somewhere left", "0.5", "0.1,0.2,1.5,9", "0,5;0,5").forEach { raw ->
            val pack = open(question(100, remoteImage("map"), answer = raw, params = point(deviation = "0.1")))

            val answer = assertIs<Answer.Simple>(pack.questions().single().answer, raw)
            assertEquals(listOf(raw), answer.right)
        }
    }

    @Test
    fun `a Point Answer on a question without an image leaves the question out`() {
        val pack = open(
            question(100, text("Q")),
            question(200, text("Where?"), answer = "0.5,0.5", params = point(deviation = "0.1")),
        )

        assertEquals(listOf(100), pack.questions().map { it.price })
    }

    @Test
    fun `a question of an unknown type is left out instead of failing the pack`() {
        val pack = open(question(100, text("Q")), question(1200, item(null, ""), type = "secretNoQuestion"))

        assertEquals(listOf(100), pack.questions().map { it.price })
    }

    @Test
    fun `a pack with nothing playable fails to open`() {
        assertFailsWith<IllegalArgumentException> { open(question(100, image("gone.png"))) }
    }

    @Test
    fun `closing the pack deletes its extracted files`() {
        val pack = SiqPack.open(zip(listOf(question(100, image("pic.png"))), mapOf("Images/pic.png" to "i")))
        val file = assertIs<Source.Local>(assertIs<Content.Media>(pack.pack.questions().single().contents.single()).source).path

        pack.close()

        assertFalse(file.exists())
        assertFalse(file.parent.parent.exists())
    }

    private fun open(vararg questions: String, files: Map<String, String> = emptyMap()) =
        SiqPack.open(zip(questions.toList(), files)).also(opened::add).pack

    private fun Package.questions() = rounds.flatMap { it.themes }.flatMap { it.questions }

    private fun zip(questions: List<String>, files: Map<String, String>): Path =
        Files.createTempFile(createTempDirectory("siq-source-"), "pack-", ".siq").also { zip ->
            ZipOutputStream(Files.newOutputStream(zip)).use { output ->
                (files + ("content.xml" to content(questions))).forEach { (name, contents) ->
                    output.putNextEntry(ZipEntry(name))
                    output.write(contents.toByteArray())
                    output.closeEntry()
                }
            }
        }

    private companion object {
        fun content(questions: List<String>) = """
            <?xml version="1.0" encoding="utf-8"?>
            <package name="p" version="5" xmlns="https://github.com/VladimirKhil/SI/blob/master/assets/siq_5.xsd">
              <info><authors><author>a</author></authors></info>
              <rounds><round name="R"><themes><theme name="T"><questions>
                ${questions.joinToString("\n")}
              </questions></theme></themes></round></rounds>
            </package>
        """.trimIndent()

        fun question(
            price: Int,
            items: String,
            answer: String = "A",
            type: String? = null,
            params: String = "",
            answerItems: String? = null,
        ) = """
            <question price="$price"${type?.let { " type=\"$it\"" }.orEmpty()}>
              <params>
                <param name="question" type="content">$items</param>
                ${answerItems?.let { "<param name=\"answer\" type=\"content\">$it</param>" }.orEmpty()}
                $params
              </params>
              <right><answer>$answer</answer></right>
            </question>
        """

        fun point(deviation: String?) = "<param name=\"answerType\">point</param>" +
            deviation?.let { "<param name=\"answerDeviation\">$it</param>" }.orEmpty()

        fun item(type: String?, value: String, isRef: Boolean = true) =
            "<item${type?.let { " type=\"$it\"" }.orEmpty()}${if (isRef) " isRef=\"True\"" else ""}>$value</item>"

        fun text(value: String) = item(null, value, isRef = false)
        fun image(ref: String) = item("image", ref)
        fun remoteImage(name: String) = item("image", "http://media.test/$name.png", isRef = false)
    }
}
