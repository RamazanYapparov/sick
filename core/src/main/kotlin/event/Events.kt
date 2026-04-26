package com.sick.event

import java.util.UUID

sealed interface GameEvent

// Lobby
data class PlayerJoined(val name: String) : GameEvent
data class PlayerLeft(val playerId: UUID) : GameEvent
data class PlayerRenamed(val playerId: UUID, val newName: String) : GameEvent
data object StartGame : GameEvent

// Player/question selection
data class SelectActivePlayer(val playerId: UUID) : GameEvent
data class QuestionSelected(val questionId: UUID) : GameEvent

// During question display
data class PlayerBuzzed(val playerId: UUID) : GameEvent
data object TimerTick : GameEvent
data object TimerExpired : GameEvent

// Host judging
data object HostAccepted : GameEvent
data object HostRejected : GameEvent

// Round/game flow
data object NextRound : GameEvent
