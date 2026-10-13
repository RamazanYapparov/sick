package com.sick.com.sick.siq.mapper

import com.sick.model.Answer
import com.sick.siq.xml.model.Param
import com.sick.siq.xml.model.Params
import com.sick.siq.xml.model.Question
import com.sick.siq.xml.model.Right
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QuestionMapperTest {

    @Test
    fun `maps point answer with aspect ratio and deviation`() {
        val answer = pointQuestion(answer = "0.66,0.5,1.79", deviation = "0.11").toAnswer()

        assertEquals(Answer.Point(x = 0.66, y = 0.5, aspectRatio = 1.79, deviation = 0.11), answer)
    }

    @Test
    fun `point answer without aspect ratio or deviation defaults to square and exact`() {
        val answer = pointQuestion(answer = "0.2, 0.8", deviation = null).toAnswer()

        assertEquals(Answer.Point(x = 0.2, y = 0.8, aspectRatio = 1.0, deviation = 0.0), answer)
    }

    @Test
    fun `malformed point answer falls back to simple text answer`() {
        listOf("somewhere left", "0.5", "0.1,0.2,1.5,9", "0,5;0,5").forEach { raw ->
            val answer = pointQuestion(answer = raw, deviation = "0.1").toAnswer()

            assertIs<Answer.Simple>(answer, raw)
            assertEquals(listOf(raw), answer.right)
        }
    }

    private fun pointQuestion(answer: String, deviation: String?) = Question().apply {
        price = 200
        params = Params().apply {
            param = listOfNotNull(
                Param().apply { name = "answerType"; value = "point" },
                deviation?.let { Param().apply { name = "answerDeviation"; value = it } },
            )
        }
        right = Right().apply { this.answer = listOf(answer) }
    }
}
