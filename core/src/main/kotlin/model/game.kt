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
    val answerTimerSeconds: Int = 20,
    val answerTimerRemaining: Int = 0,
    val isTimerPaused: Boolean = false,
    val failedBuzzPlayerIds: Set<UUID> = emptySet(),
    val skipVotePlayerIds: Set<UUID> = emptySet(),
    /** Monotonic-clock nanos when the current Buzz Window opened; null before the first reveal of a question. */
    val buzzWindowOpenedAtNanos: Long? = null,
    /** Buzzes of the current Buzz Window in arrival order: the Answering Player's first, then Late Buzzes. */
    val buzzes: List<Buzz> = emptyList(),
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

/** A Buzz accepted in the current Buzz Window, with its Reaction Time. */
data class Buzz(val playerId: UUID, val reactionMillis: Long)

/**
 * Returns true if every player has either failed their buzz-in or skipped (voted
 * to skip) on the current question, meaning the round is exhausted and the
 * next event should advance the engine to ShowingAnswer.
 */
fun GameState.allPlayersAccountedFor(): Boolean {
    val allIds = players.map { it.id }.toSet()
    return (skipVotePlayerIds + failedBuzzPlayerIds).containsAll(allIds)
}
