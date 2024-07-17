package com.sick.service.player.updateScore.subtractFull

import arrow.core.raise.either
import com.sick.model.Game
import com.sick.model.Question
import com.sick.service.player.find.PlayerFindService

class PlayerUpdateScoreSubtractFullService(
    private val findService: PlayerFindService
) {
    fun execute(game: Game, playerName: String, question: Question) = either {
        val player = findService.execute(game, playerName).bind()
        player.score -= question.price
    }
}