package com.sick.service

import com.sick.model.Player

/** Scale for a question price. Halving rounds up, so an odd price never loses its last point. */
enum class ScoreMultiplier {
    FULL,
    HALF,
    DOUBLE;

    fun applyTo(price: Int): Int = when (this) {
        FULL -> price
        HALF -> Math.ceilDiv(price, 2)
        DOUBLE -> price * 2
    }
}

fun Player.addScore(questionPrice: Int, multiplier: ScoreMultiplier = ScoreMultiplier.FULL): Player =
    copy(score = score + multiplier.applyTo(questionPrice))

fun Player.subtractScore(questionPrice: Int, multiplier: ScoreMultiplier = ScoreMultiplier.FULL): Player =
    copy(score = score - multiplier.applyTo(questionPrice))
