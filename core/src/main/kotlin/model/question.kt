package com.sick.model

import java.net.URL
import java.nio.file.Path
import java.util.UUID

data class Question<T: Question.Type>(
    val id: UUID = UUID.randomUUID(),
    val price: Int,
    val type: T,
    val contents: List<Content>,
    val answer: Answer,
) {

    sealed interface Type {
        data object Simple : Type
        data object Stake : Type
        data object NoRisk : Type

        data class Secret(
            val themeOverride: String?,
            val exceptCurrent: Boolean,
            val pricingRule: PricingRule?
        ) : Type {
            data class PricingRule(
                val minimum: Int,
                val maximum: Int,
                val step: Int,
            )
        }
    }
}


sealed interface Content {

    enum class Type {
        Image,
        Video,
        Audio,
    }

    data class Text(val text: String) : Content

    /** Image, audio or video; a pack only yields media whose file it found. */
    data class Media(val type: Type, val source: Source) : Content {
        sealed interface Source {
            /** A file inside the extracted pack. */
            data class Local(val path: Path) : Source
            data class Remote(val url: URL) : Source
        }
    }
}

sealed interface Answer {

    data class Simple(
        val right: List<String>,
        val wrong: List<String>,
        val contents: List<Content> = emptyList(),
    ) : Answer

    data class Select(
        val options: List<Option>,
    ): Answer {
        data class Option(
            val name: String,
            val answer: String,
            val correct: Boolean,
        )
    }

    /**
     * A spot on [image], the first image of the question. [x] and [y] are fractions
     * (0..1) of the image width and height from its top-left corner.
     * [aspectRatio] is the image width / height recorded by the pack editor.
     * [deviation] is the accepted radius as a fraction of the image's shorter side.
     */
    data class Point(
        val image: Content.Media,
        val x: Double,
        val y: Double,
        val aspectRatio: Double,
        val deviation: Double,
    ) : Answer
}