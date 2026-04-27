package com.sick.com.sick.siq

import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.RoundType

fun Package.sanitizeForSimpleMode(): Package {
    val rounds = rounds
        .filter { it.type == RoundType.Simple }
        .mapNotNull { round ->
            val themes = round.themes.mapNotNull { theme ->
                val questions = theme.questions.filter { it.type is Question.Type.Simple }
                if (questions.isEmpty()) null else theme.copy(questions = questions)
            }
            if (themes.isEmpty()) null else round.copy(themes = themes)
        }

    require(rounds.isNotEmpty()) { "Pack does not contain supported regular rounds." }
    return copy(rounds = rounds)
}
