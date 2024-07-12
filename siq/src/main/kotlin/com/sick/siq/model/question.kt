package com.sick.siq.model

import java.net.URL

data class Question(
    val price: Int,
    val type: Type,
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
            val pricingRule: PricingRule
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

    sealed interface Media : Content {
        val type: Type

        data class FileRef(override val type: Type, val ref: String) : Media
        data class FileUrl(override val type: Type, val url: URL) : Media
    }
}

sealed interface Answer {

    data class Simple(
        val right: List<String>,
        val wrong: List<String>,
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
}