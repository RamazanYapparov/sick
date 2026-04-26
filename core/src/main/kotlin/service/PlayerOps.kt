package com.sick.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.sick.model.GameState
import com.sick.model.Player
import java.util.UUID

sealed class PlayerError(val message: String) {
    class AlreadyExists(name: String) : PlayerError("Player '$name' already exists")
    class NotFound(id: UUID) : PlayerError("Player with id $id not found")
    class InvalidName(name: String) : PlayerError("Invalid player name: '$name'")
}

fun GameState.addPlayer(name: String): Either<PlayerError, GameState> = either {
    ensure(!name.isBlank()) { PlayerError.InvalidName(name) }
    ensure(players.none { it.name == name }) { PlayerError.AlreadyExists(name) }
    copy(players = players + Player(name = name))
}

fun GameState.removePlayer(playerId: UUID): Either<PlayerError, GameState> = either {
    ensure(players.any { it.id == playerId }) { PlayerError.NotFound(playerId) }
    copy(players = players.filter { it.id != playerId })
}

fun GameState.renamePlayer(playerId: UUID, newName: String): Either<PlayerError, GameState> = either {
    ensure(!newName.isBlank()) { PlayerError.InvalidName(newName) }
    ensure(players.none { it.name == newName && it.id != playerId }) { PlayerError.AlreadyExists(newName) }
    ensure(players.any { it.id == playerId }) { PlayerError.NotFound(playerId) }
    copy(players = players.map { if (it.id == playerId) it.copy(name = newName) else it })
}

fun GameState.updatePlayerScore(playerId: UUID, transform: (Player) -> Player): Either<PlayerError, GameState> = either {
    ensure(players.any { it.id == playerId }) { PlayerError.NotFound(playerId) }
    copy(players = players.map { if (it.id == playerId) transform(it) else it })
}
