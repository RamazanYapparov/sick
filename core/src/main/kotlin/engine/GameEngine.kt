package com.sick.engine

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.sick.event.*
import com.sick.model.*
import com.sick.service.*
import com.sick.state.*
import java.util.UUID

sealed class GameError(val message: String) {
    class InvalidEvent(event: GameEvent, phase: GamePhase) :
        GameError("Cannot process ${event::class.simpleName} in phase ${phase.name}")

    class PlayerError(val inner: com.sick.service.PlayerError) : GameError(inner.message)
    class QuestionNotFound(id: UUID) : GameError("Question $id not found")
}

class GameEngine(pack: Package) {

    private var _state: GameState = GameState(pack = pack)
    val state: GameState get() = _state

    private var _phase: GamePhase = GamePhase.Lobby
    val phase: GamePhase get() = _phase

    private val listeners = mutableListOf<(GameState, GamePhase) -> Unit>()

    fun addListener(listener: (GameState, GamePhase) -> Unit) {
        listeners.add(listener)
    }

    fun process(event: GameEvent): Either<GameError, GameState> {
        validateEventForPhase(event)?.let { return it.left() }

        return applyEvent(event).map { newState ->
            _state = newState
            _phase = nextPhase(event)
            notifyListeners()
            _state
        }
    }

    private fun validateEventForPhase(event: GameEvent): GameError? {
        val allowed = when (_phase) {
            GamePhase.Lobby -> setOf(
                PlayerJoined::class,
                PlayerLeft::class,
                PlayerRenamed::class,
                StartGame::class,
                AdjustPlayerScore::class,
            )
            GamePhase.ChoosingPlayer -> setOf(SelectActivePlayer::class, AdjustPlayerScore::class)
            GamePhase.ChoosingQuestion -> setOf(QuestionSelected::class, AdjustPlayerScore::class)
            GamePhase.ShowingQuestion -> setOf(
                PlayerBuzzed::class, TimerTick::class, TimerExpired::class, SkipQuestion::class, AdjustPlayerScore::class
            )
            GamePhase.PlayerAnswering -> setOf(
                HostAccepted::class,
                HostRejected::class,
                SkipQuestion::class,
                AdjustPlayerScore::class,
            )
            GamePhase.RoundEnd -> setOf(NextRound::class, AdjustPlayerScore::class)
            GamePhase.GameOver -> setOf(AdjustPlayerScore::class)
        }
        return if (event::class !in allowed) GameError.InvalidEvent(event, _phase) else null
    }

    private fun applyEvent(event: GameEvent): Either<GameError, GameState> = either {
        when (event) {
            is PlayerJoined  -> _state.addPlayer(event.name).mapLeft { GameError.PlayerError(it) }.bind()
            is PlayerLeft    -> _state.removePlayer(event.playerId).mapLeft { GameError.PlayerError(it) }.bind()
            is PlayerRenamed -> _state.renamePlayer(event.playerId, event.newName).mapLeft { GameError.PlayerError(it) }.bind()
            is StartGame     -> _state

            is SelectActivePlayer -> _state.copy(activePlayerId = event.playerId)

            is QuestionSelected -> {
                val question = ensureNotNull(findQuestion(event.questionId)) { GameError.QuestionNotFound(event.questionId) }
                _state.copy(
                    currentQuestion = question,
                    playedQuestionIds = _state.playedQuestionIds + event.questionId,
                    timerRemaining = _state.timerSeconds,
                    failedBuzzPlayerIds = emptySet(),
                )
            }

            is PlayerBuzzed -> {
                ensure(event.playerId !in _state.failedBuzzPlayerIds) { GameError.InvalidEvent(event, _phase) }
                _state.copy(answeringPlayerId = event.playerId)
            }

            is TimerTick    -> _state.copy(timerRemaining = (_state.timerRemaining - 1).coerceAtLeast(0))
            is TimerExpired -> _state.copy(answeringPlayerId = null, currentQuestion = null)
            is SkipQuestion -> _state.copy(
                answeringPlayerId = null,
                currentQuestion = null,
                timerRemaining = 0,
                failedBuzzPlayerIds = emptySet(),
            )

            is HostAccepted -> {
                val playerId = ensureNotNull(_state.answeringPlayerId) { GameError.InvalidEvent(event, _phase) }
                val question  = ensureNotNull(_state.currentQuestion)  { GameError.InvalidEvent(event, _phase) }
                _state.updatePlayerScore(playerId) { it.addScore(question.price) }
                    .mapLeft { GameError.PlayerError(it) }
                    .bind()
                    .copy(
                        activePlayerId = playerId,
                        answeringPlayerId = null,
                        currentQuestion = null,
                    )
            }

            is HostRejected -> {
                val playerId = ensureNotNull(_state.answeringPlayerId) { GameError.InvalidEvent(event, _phase) }
                val question  = ensureNotNull(_state.currentQuestion)  { GameError.InvalidEvent(event, _phase) }
                _state.updatePlayerScore(playerId) { it.subtractScore(question.price) }
                    .mapLeft { GameError.PlayerError(it) }
                    .bind()
                    .copy(
                        answeringPlayerId = null,
                        failedBuzzPlayerIds = _state.failedBuzzPlayerIds + playerId,
                    )
            }

            is AdjustPlayerScore -> _state.updatePlayerScore(event.playerId) { player ->
                player.copy(score = player.score + event.delta)
            }.mapLeft { GameError.PlayerError(it) }.bind()

            is NextRound -> _state.copy(currentRoundIndex = _state.currentRoundIndex + 1)
        }
    }

    private fun nextPhase(event: GameEvent): GamePhase = when (event) {
        is StartGame -> GamePhase.ChoosingPlayer
        is SelectActivePlayer -> GamePhase.ChoosingQuestion
        is QuestionSelected -> GamePhase.ShowingQuestion
        is PlayerBuzzed -> GamePhase.PlayerAnswering
        is TimerExpired -> when {
            _state.isGameOver -> GamePhase.GameOver
            _state.isRoundComplete -> GamePhase.RoundEnd
            else -> GamePhase.ChoosingQuestion
        }
        is SkipQuestion -> when {
            _state.isGameOver -> GamePhase.GameOver
            _state.isRoundComplete -> GamePhase.RoundEnd
            else -> GamePhase.ChoosingQuestion
        }
        is HostAccepted -> when {
            _state.isGameOver -> GamePhase.GameOver
            _state.isRoundComplete -> GamePhase.RoundEnd
            else -> GamePhase.ChoosingQuestion
        }
        is HostRejected -> GamePhase.ShowingQuestion
        is NextRound -> if (_state.isGameOver) GamePhase.GameOver else GamePhase.ChoosingPlayer
        is PlayerJoined, is PlayerLeft, is PlayerRenamed -> _phase
        is TimerTick, is AdjustPlayerScore -> _phase
    }

    private fun findQuestion(id: UUID): Question<*>? =
        _state.pack.rounds
            .flatMap { it.themes }
            .flatMap { it.questions }
            .find { it.id == id }

    private fun notifyListeners() {
        listeners.forEach { it(_state, _phase) }
    }
}
