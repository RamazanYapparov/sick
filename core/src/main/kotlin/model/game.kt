package com.sick.model

import java.util.UUID

data class GameState(
    val pack: Package,
    val players: List<Player> = emptyList(),
    val currentRoundIndex: Int = 0,
    val activePlayerId: UUID? = null,
    val currentQuestion: Question<*>? = null,
    val answeringPlayerId: UUID? = null,
    val playedQuestionIds: Set<UUID> = emptySet(),
    val timerSeconds: Int = 30,
    val timerRemaining: Int = 0,
    val isTimerPaused: Boolean = false,
    val failedBuzzPlayerIds: Set<UUID> = emptySet(),
    val skipVotePlayerIds: Set<UUID> = emptySet(),
) {
    val currentRound: Round? get() = pack.rounds.getOrNull(currentRoundIndex)

    val isRoundComplete: Boolean
        get() {
            val round = currentRound ?: return true
            val allQuestionIds = round.themes.flatMap { it.questions }.map { it.id }.toSet()
            return allQuestionIds.all { it in playedQuestionIds }
        }

    val isGameOver: Boolean get() = currentRoundIndex >= pack.rounds.size

    fun findPlayer(id: UUID): Player? = players.find { it.id == id }
}
