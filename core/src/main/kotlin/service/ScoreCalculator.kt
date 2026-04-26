package com.sick.service

import com.sick.model.Player

enum class ScoreMultiplier(val factor: Double) {
    FULL(1.0),
    HALF(0.5),
    DOUBLE(2.0),
}

fun Player.addScore(questionPrice: Int, multiplier: ScoreMultiplier = ScoreMultiplier.FULL): Player =
    copy(score = score + (questionPrice * multiplier.factor).toInt())

fun Player.subtractScore(questionPrice: Int, multiplier: ScoreMultiplier = ScoreMultiplier.FULL): Player =
    copy(score = score - (questionPrice * multiplier.factor).toInt())
