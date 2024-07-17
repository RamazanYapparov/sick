package com.sick.service.player.remove

import arrow.core.raise.either
import com.sick.model.Game

class PlayerRemoveService {
    fun execute(game: Game, playerName: String) = either<Unit, Game> {
        val players = game.players.filter { it.name != playerName }
        game.copy(players = players)
    }
}