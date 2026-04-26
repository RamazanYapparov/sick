package com.sick.test

import com.sick.model.*
import java.util.UUID

val QUESTION_IDS: Array<Array<UUID>> = Array(5) { r ->
    Array(5) { q -> UUID.nameUUIDFromBytes("r${r}q${q}".toByteArray()) }
}

fun minimalPackage(rounds: Int = 1, questionsPerTheme: Int = 1): Package {
    val prices = listOf(100, 200, 300, 400, 500)
    return Package(
        name = "Test Package",
        logo = "",
        tags = emptyList(),
        author = "Test Author",
        rounds = List(rounds) { r ->
            Round(
                name = "Round ${r + 1}",
                type = RoundType.Simple,
                themes = listOf(
                    Theme(
                        name = "Theme 1",
                        questions = List(questionsPerTheme) { q ->
                            Question(
                                id = QUESTION_IDS[r][q],
                                price = prices[q % prices.size],
                                type = Question.Type.Simple,
                                contents = listOf(Content.Text("Q${q + 1}")),
                                answer = Answer.Simple(right = listOf("A"), wrong = emptyList()),
                            )
                        }
                    )
                )
            )
        }
    )
}
