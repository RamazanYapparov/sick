package com.sick.service

import com.sick.model.Player
import kotlin.test.*

class ScoreCalculatorTest {

    private val player = Player(name = "Alice")

    @Test
    fun `addScore with FULL multiplier adds full price`() {
        assertEquals(100, player.addScore(100).score)
    }

    @Test
    fun `addScore with HALF multiplier adds half price`() {
        assertEquals(50, player.addScore(100, ScoreMultiplier.HALF).score)
    }

    @Test
    fun `addScore with DOUBLE multiplier adds double price`() {
        assertEquals(200, player.addScore(100, ScoreMultiplier.DOUBLE).score)
    }

    @Test
    fun `subtractScore with FULL multiplier subtracts full price`() {
        assertEquals(-100, player.subtractScore(100).score)
    }

    @Test
    fun `subtractScore with HALF multiplier subtracts half price`() {
        assertEquals(-50, player.subtractScore(100, ScoreMultiplier.HALF).score)
    }

    @Test
    fun `subtractScore with DOUBLE multiplier subtracts double price`() {
        assertEquals(-200, player.subtractScore(100, ScoreMultiplier.DOUBLE).score)
    }

    @Test
    fun `addScore with zero price leaves score unchanged`() {
        assertEquals(0, player.addScore(0).score)
    }

    @Test
    fun `score can go negative after subtraction`() {
        val withScore = player.addScore(100)
        assertEquals(-100, withScore.subtractScore(200).score)
    }

    @Test
    fun `operations are cumulative`() {
        val result = player.addScore(300).subtractScore(100, ScoreMultiplier.HALF)
        assertEquals(250, result.score)
    }
}
