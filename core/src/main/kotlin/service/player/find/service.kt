package com.sick.service.player.find

import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.sick.model.Game
import com.sick.model.Player

sealed class Error(val message: String) {
    class PlayerNotExist(name: String) : Error("Player $name not exist")
}

class PlayerFindService {
    fun execute(game: Game, playerName: String) = either<Error, Player> {
        ensureNotNull(game.players.find { it.name == playerName }) { Error.PlayerNotExist(playerName) }
    }
}