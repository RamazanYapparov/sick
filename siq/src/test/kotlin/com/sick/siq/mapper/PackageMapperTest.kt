package com.sick.com.sick.siq.mapper

import com.sick.model.Content
import com.sick.siq.xml.model.Authors
import com.sick.siq.xml.model.Info
import com.sick.siq.xml.model.Item
import com.sick.siq.xml.model.Package as XmlPackage
import com.sick.siq.xml.model.Param
import com.sick.siq.xml.model.Params
import com.sick.siq.xml.model.Question
import com.sick.siq.xml.model.Questions
import com.sick.siq.xml.model.Right
import com.sick.siq.xml.model.Round
import com.sick.siq.xml.model.Rounds
import com.sick.siq.xml.model.Scenario
import com.sick.siq.xml.model.Theme
import com.sick.siq.xml.model.Themes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PackageMapperTest {

    @Test
    fun `maps package without tags to empty tags list`() {
        val xml = XmlPackage().apply {
            name = "pack-without-tags"
            tags = null
            info = Info().apply {
                authors = Authors().apply { author = listOf("author") }
            }
            rounds = Rounds().apply {
                round = listOf(
                    Round().apply {
                        name = "Round 1"
                        type = null
                        themes = Themes().apply {
                            theme = listOf(
                                Theme().apply {
                                    name = "Theme 1"
                                    questions = Questions().apply {
                                        question = listOf(
                                            Question().apply {
                                                price = 100
                                                params = Params().apply {
                                                    param = listOf(
                                                        Param().apply {
                                                            name = "question"
                                                            item = listOf(
                                                                Item().apply {
                                                                    value = "Question text"
                                                                }
                                                            )
                                                        }
                                                    )
                                                }
                                                right = Right().apply {
                                                    answer = listOf("Answer")
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }

        val domain = xml.toDomain()

        assertEquals(emptyList(), domain.tags)
    }

    @Test
    fun `maps scenario atoms when question params are missing`() {
        val xml = XmlPackage().apply {
            name = "pack-with-scenario"
            tags = null
            info = Info().apply {
                authors = Authors().apply { author = listOf("author") }
            }
            rounds = Rounds().apply {
                round = listOf(
                    Round().apply {
                        name = "Round 1"
                        themes = Themes().apply {
                            theme = listOf(
                                Theme().apply {
                                    name = "Theme 1"
                                    questions = Questions().apply {
                                        question = listOf(
                                            Question().apply {
                                                price = 100
                                                params = null
                                                scenario = Scenario().apply {
                                                    atom = listOf(
                                                        Item().apply { value = "Question text" },
                                                        Item().apply {
                                                            type = "image"
                                                            value = "@pic.jpg"
                                                        },
                                                        Item().apply { type = "marker" }
                                                    )
                                                }
                                                right = Right().apply {
                                                    answer = listOf("Answer")
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }

        val domain = xml.toDomain()
        val contents = domain.rounds.single().themes.single().questions.single().contents

        assertEquals(2, contents.size)
        assertIs<Content.Text>(contents[0])
        assertIs<Content.Media.FileRef>(contents[1])
    }
}
