package com.sick.service.player.updateScore

import arrow.core.raise.either
import com.sick.model.Game
import com.sick.service.player.find.PlayerFindService

class UpdateScoreService(
    private val findService: PlayerFindService,
) {
    fun execute(game: Game, playerName: String, score: Int) = either {
        val player = findService.execute(game, playerName).bind()
        player.score = score
        game
    }
}