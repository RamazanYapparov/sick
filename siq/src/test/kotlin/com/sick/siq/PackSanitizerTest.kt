package com.sick.com.sick.siq

import com.sick.model.Answer
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PackSanitizerTest {

    @Test
    fun `keeps simple rounds and simple questions`() {
        val pack = pack(
            round("R1", RoundType.Simple,
                theme("T1", simple(100), simple(200))
            )
        )

        val sanitized = pack.sanitizeForSimpleMode()

        assertEquals(1, sanitized.rounds.size)
        assertEquals(2, sanitized.rounds[0].themes[0].questions.size)
    }

    @Test
    fun `drops final rounds`() {
        val pack = pack(
            round("Regular", RoundType.Simple, theme("T", simple(100))),
            round("Final", RoundType.Final, theme("F", simple(500))),
        )

        val sanitized = pack.sanitizeForSimpleMode()

        assertEquals(1, sanitized.rounds.size)
        assertEquals("Regular", sanitized.rounds[0].name)
    }

    @Test
    fun `drops non-simple questions and empty themes`() {
        val pack = pack(
            round("R", RoundType.Simple,
                theme("MixedTheme", simple(100), stake(200)),
                theme("AllNonSimple", stake(300), noRisk(400)),
            )
        )

        val sanitized = pack.sanitizeForSimpleMode()

        assertEquals(1, sanitized.rounds[0].themes.size)
        assertEquals("MixedTheme", sanitized.rounds[0].themes[0].name)
        assertEquals(1, sanitized.rounds[0].themes[0].questions.size)
        assertTrue(sanitized.rounds[0].themes[0].questions[0].type is Question.Type.Simple)
    }

    @Test
    fun `drops rounds whose themes all become empty`() {
        val pack = pack(
            round("KeepMe", RoundType.Simple, theme("T", simple(100))),
            round("DropMe", RoundType.Simple, theme("T", stake(200))),
        )

        val sanitized = pack.sanitizeForSimpleMode()

        assertEquals(1, sanitized.rounds.size)
        assertEquals("KeepMe", sanitized.rounds[0].name)
    }

    @Test
    fun `throws when no supported rounds remain`() {
        val pack = pack(
            round("Final", RoundType.Final, theme("T", simple(100))),
            round("StakesOnly", RoundType.Simple, theme("T", stake(200))),
        )

        assertFailsWith<IllegalArgumentException> {
            pack.sanitizeForSimpleMode()
        }
    }

    private fun pack(vararg rounds: Round) = Package(
        name = "p",
        logo = "",
        tags = emptyList(),
        author = "",
        rounds = rounds.toList(),
    )

    private fun round(name: String, type: RoundType, vararg themes: Theme) =
        Round(name = name, type = type, themes = themes.toList())

    private fun theme(name: String, vararg questions: Question<*>) =
        Theme(name = name, questions = questions.toList())

    private fun simple(price: Int): Question<Question.Type.Simple> = Question(
        price = price,
        type = Question.Type.Simple,
        contents = emptyList(),
        answer = Answer.Simple(right = listOf("a"), wrong = emptyList()),
    )

    private fun stake(price: Int): Question<Question.Type.Stake> = Question(
        price = price,
        type = Question.Type.Stake,
        contents = emptyList(),
        answer = Answer.Simple(right = listOf("a"), wrong = emptyList()),
    )

    private fun noRisk(price: Int): Question<Question.Type.NoRisk> = Question(
        price = price,
        type = Question.Type.NoRisk,
        contents = emptyList(),
        answer = Answer.Simple(right = listOf("a"), wrong = emptyList()),
    )
}
