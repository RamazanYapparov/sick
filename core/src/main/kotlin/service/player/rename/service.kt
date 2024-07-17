package com.sick.service.player.rename

import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.sick.model.Game
import com.sick.service.player.find.PlayerFindService

sealed class Error(val message: String) {
    class PlayerNotExist(name: String) : Error("Player $name not exist")
}

class PlayerRenameService(
    private val findService: PlayerFindService,
) {
    fun execute(game: Game, currentPlayerName: String, newName: String) = either {
        val player = findService.execute(game, currentPlayerName).bind()
        player.name = newName
        game
    }
}
