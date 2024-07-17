package com.sick.service.player.add

import arrow.core.raise.either
import arrow.core.raise.ensure
import com.sick.model.Game
import com.sick.model.Player

sealed class Error(val message: String) {
    class PlayerAlreadyExists(name: String) : Error("Player $name already exists")
}

class PlayerAddService {
    fun execute(game: Game, playerName: String) = either {
        ensure(playerName !in game.players.map { it.name }) { Error.PlayerAlreadyExists(playerName) }
        val players = game.players + Player(playerName)
        game.copy(players = players)
    }

}