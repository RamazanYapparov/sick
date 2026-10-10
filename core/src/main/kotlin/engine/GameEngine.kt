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
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

sealed class GameError(val message: String) {
    class InvalidEvent(event: GameEvent, phase: GamePhase) :
        GameError("Cannot process ${event::class.simpleName} in phase ${phase.name}")

    class PlayerError(val inner: com.sick.service.PlayerError) : GameError(inner.message)
    class QuestionNotFound(id: UUID) : GameError("Question $id not found")
}

/**
 * [clock] is a monotonic nanosecond source used only to measure Reaction Time
 * (see docs/adr/0001-reaction-time-measured-at-host.md); tests inject a fake one.
 */
class GameEngine(pack: Package, private val clock: () -> Long = System::nanoTime) {

    private val lock = Any()

    private var _state: GameState = GameState(pack = pack)
    val state: GameState get() = synchronized(lock) { _state }

    private var _phase: GamePhase = GamePhase.Lobby
    val phase: GamePhase get() = synchronized(lock) { _phase }

    private var _previousState: GameState = GameState(pack = pack)
    val previousState: GameState get() = synchronized(lock) { _previousState }

    private var _previousPhase: GamePhase = GamePhase.Lobby
    val previousPhase: GamePhase get() = synchronized(lock) { _previousPhase }

    // Listeners run under `lock` so addListener / removeListener / notifyListeners
    // are all serialised. The listener type therefore does not need to be
    // CopyOnWriteArrayList; we keep a regular ArrayList.
    private val listeners =
        mutableListOf<(GameState, GameState, GamePhase, GamePhase) -> Unit>()

    /**
     * Subscribe to phase transitions. Each successful `process(event)` triggers
     * every registered listener once with the (previousState, currentState,
     * previousPhase, currentPhase) snapshot. Listeners are invoked UNDER the
     * engine's monitor lock so they observe a consistent view, but because the
     * lock is reentrant another goroutine / thread could attempt synchronous
     * re-entry by calling `engine.process` from a listener — DO NOT do that.
     * Dispatch from listeners via an external coroutine, or via the engine's
     * own queue if one is added later.
     */
    fun addListener(listener: (GameState, GameState, GamePhase, GamePhase) -> Unit) {
        synchronized(lock) { listeners.add(listener) }
    }

    fun removeListener(listener: (GameState, GameState, GamePhase, GamePhase) -> Unit) {
        synchronized(lock) { listeners.remove(listener) }
    }

    fun process(event: GameEvent): Either<GameError, GameState> = synchronized(lock) {
        logger.trace { "process: event=${event::class.simpleName}, phase=$_phase, thread=${Thread.currentThread().name}" }

        validateEventForPhase(event)?.let {
            logger.warn { "process: rejected ${event::class.simpleName} in phase $_phase: ${it.message}" }
            return@synchronized it.left()
        }

        return@synchronized applyEvent(event).map { newState ->
            val oldState = _state
            val oldPhase = _phase
            _previousState = oldState
            _previousPhase = oldPhase
            _state = newState
            _phase = nextPhase(event, oldState)
            logger.debug { "process: ${event::class.simpleName} $_previousPhase -> $_phase (${_state.timerRemaining}s)" }
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
            GamePhase.ChoosingPlayer -> setOf(SelectActivePlayer::class, SkipRound::class, AdjustPlayerScore::class)
            GamePhase.ChoosingQuestion -> setOf(QuestionSelected::class, SkipRound::class, AdjustPlayerScore::class)
            GamePhase.RevealingQuestion -> setOf(QuestionRevealed::class, SkipQuestion::class, AdjustPlayerScore::class)
            GamePhase.ShowingQuestion -> setOf(
                PlayerBuzzed::class,
                PlayerSkipped::class,
                PauseTimer::class,
                ResumeTimer::class,
                TimerTick::class,
                TimerExpired::class,
                SkipQuestion::class,
                AdjustPlayerScore::class,
            )
            GamePhase.PlayerAnswering -> setOf(
                PlayerBuzzed::class,
                HostAccepted::class,
                HostRejected::class,
                SkipQuestion::class,
                AdjustPlayerScore::class,
                PauseTimer::class,
                ResumeTimer::class,
                AnswerTimerTick::class,
                AnswerTimerExpired::class,
            )
            GamePhase.ShowingAnswer -> setOf(AnswerShown::class, AdjustPlayerScore::class)
            GamePhase.RoundEnd -> setOf(NextRound::class, AdjustPlayerScore::class)
            GamePhase.GameOver -> setOf(AdjustPlayerScore::class)
        }
        if (event::class !in allowed) {
            return GameError.InvalidEvent(event, _phase)
        }

        return when (event) {
            is PauseTimer -> if (_state.isTimerPaused) GameError.InvalidEvent(event, _phase) else null
            is ResumeTimer -> if (!_state.isTimerPaused) GameError.InvalidEvent(event, _phase) else null
            is PlayerBuzzed -> if (_state.isTimerPaused) GameError.InvalidEvent(event, _phase) else null
            else -> null
        }
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
                    answerTimerRemaining = _state.answerTimerSeconds,
                    isTimerPaused = false,
                    failedBuzzPlayerIds = emptySet(),
                    skipVotePlayerIds = emptySet(),
                    buzzWindowOpenedAtNanos = null,
                    buzzes = emptyList(),
                )
            }

            is QuestionRevealed -> _state.withOpenBuzzWindow()

            // Both a phone Buzz and a Host Pick arrive here. In ShowingQuestion the
            // first one makes the Answering Player; in PlayerAnswering it is a Late Buzz.
            is PlayerBuzzed -> {
                ensure(event.playerId !in _state.failedBuzzPlayerIds) { GameError.InvalidEvent(event, _phase) }
                ensure(event.playerId !in _state.skipVotePlayerIds) { GameError.InvalidEvent(event, _phase) }
                ensure(_state.buzzes.none { it.playerId == event.playerId }) { GameError.InvalidEvent(event, _phase) }
                val openedAt = ensureNotNull(_state.buzzWindowOpenedAtNanos) { GameError.InvalidEvent(event, _phase) }
                val buzzes = _state.buzzes + Buzz(event.playerId, (clock() - openedAt) / 1_000_000)
                if (_phase == GamePhase.ShowingQuestion) {
                    _state.copy(
                        answeringPlayerId = event.playerId,
                        answerTimerRemaining = _state.answerTimerSeconds,
                        buzzes = buzzes,
                    )
                } else {
                    _state.copy(buzzes = buzzes)
                }
            }

            is PlayerSkipped -> {
                ensure(event.playerId !in _state.failedBuzzPlayerIds) { GameError.InvalidEvent(event, _phase) }
                ensure(event.playerId !in _state.skipVotePlayerIds) { GameError.InvalidEvent(event, _phase) }
                val newSkipVotes = _state.skipVotePlayerIds + event.playerId
                if (_state.copy(skipVotePlayerIds = newSkipVotes).allPlayersAccountedFor()) {
                    _state.copy(
                        skipVotePlayerIds = emptySet(),
                        failedBuzzPlayerIds = emptySet(),
                        answeringPlayerId = null,
                        timerRemaining = 0,
                        isTimerPaused = false,
                    )
                } else {
                    _state.copy(skipVotePlayerIds = newSkipVotes)
                }
            }

            is PauseTimer -> _state.copy(isTimerPaused = true)
            // Resuming a paused question opens a fresh Buzz Window; resuming while
            // a player answers keeps the current one.
            is ResumeTimer -> if (_phase == GamePhase.ShowingQuestion) {
                _state.copy(isTimerPaused = false).withOpenBuzzWindow()
            } else {
                _state.copy(isTimerPaused = false)
            }
            is TimerTick -> if (_state.isTimerPaused) {
                _state
            } else {
                _state.copy(timerRemaining = (_state.timerRemaining - 1).coerceAtLeast(0))
            }
            is AnswerTimerTick -> if (_state.isTimerPaused) {
                _state
            } else {
                _state.copy(answerTimerRemaining = (_state.answerTimerRemaining - 1).coerceAtLeast(0))
            }
            is AnswerTimerExpired -> finalizePlayerFailure(event, _state.answeringPlayerId).bind()
            is TimerExpired -> if (_state.isTimerPaused) {
                _state
            } else {
                _state.copy(answeringPlayerId = null, isTimerPaused = false)
            }
            is SkipQuestion -> _state.copy(
                answeringPlayerId = null,
                timerRemaining = 0,
                isTimerPaused = false,
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
                        isTimerPaused = false,
                    )
            }

            is HostRejected -> finalizePlayerFailure(event, _state.answeringPlayerId).bind()

            is AnswerShown -> _state.copy(
                currentQuestion = null,
                answeringPlayerId = null,
                failedBuzzPlayerIds = emptySet(),
                skipVotePlayerIds = emptySet(),
                timerRemaining = 0,
                isTimerPaused = false,
            )

            is SkipRound -> {
                val allRoundIds = _state.currentRound
                    ?.themes?.flatMap { it.questions }?.map { it.id }?.toSet()
                    ?: emptySet()
                _state.copy(
                    playedQuestionIds = _state.playedQuestionIds + allRoundIds,
                    currentQuestion = null,
                    answeringPlayerId = null,
                    timerRemaining = 0,
                    isTimerPaused = false,
                    failedBuzzPlayerIds = emptySet(),
                    buzzWindowOpenedAtNanos = null,
                    buzzes = emptyList(),
                )
            }

            is AdjustPlayerScore -> _state.updatePlayerScore(event.playerId) { player ->
                player.copy(score = player.score + event.delta)
            }.mapLeft { GameError.PlayerError(it) }.bind()

            is NextRound -> _state.copy(
                currentRoundIndex = _state.currentRoundIndex + 1,
                buzzWindowOpenedAtNanos = null,
                buzzes = emptyList(),
            )
        }
    }

    /**
     * Shared handler for AnswerTimerExpired and HostRejected: subtract the
     * question price from the answering player, flag their buzz as failed,
     * and either reset per-question bookkeeping (if that was the last player
     * accounted for) or leave the question open for the rest.
     */
    private fun finalizePlayerFailure(
        event: GameEvent,
        playerId: UUID?,
    ): Either<GameError, GameState> = either {
        val pid = ensureNotNull(playerId) { GameError.InvalidEvent(event, _phase) }
        val question = ensureNotNull(_state.currentQuestion) { GameError.InvalidEvent(event, _phase) }
        val newFailedIds = _state.failedBuzzPlayerIds + pid
        val allAccounted = _state.copy(failedBuzzPlayerIds = newFailedIds).allPlayersAccountedFor()
        val afterDeduction = _state.updatePlayerScore(pid) { it.subtractScore(question.price) }
            .mapLeft { GameError.PlayerError(it) }
            .bind()
        if (allAccounted) {
            afterDeduction.copy(
                answeringPlayerId = null,
                skipVotePlayerIds = emptySet(),
                failedBuzzPlayerIds = emptySet(),
                timerRemaining = 0,
                answerTimerRemaining = _state.answerTimerSeconds,
                isTimerPaused = false,
            )
        } else {
            afterDeduction.copy(
                answeringPlayerId = null,
                failedBuzzPlayerIds = newFailedIds,
                answerTimerRemaining = _state.answerTimerSeconds,
            ).withOpenBuzzWindow()
        }
    }

    private fun GameState.withOpenBuzzWindow(): GameState =
        copy(buzzWindowOpenedAtNanos = clock(), buzzes = emptyList())

    private fun nextPhase(event: GameEvent, oldState: GameState): GamePhase = when (event) {
        is StartGame -> GamePhase.ChoosingPlayer
        is SelectActivePlayer -> GamePhase.ChoosingQuestion
        is QuestionSelected -> GamePhase.RevealingQuestion
        is QuestionRevealed -> GamePhase.ShowingQuestion
        is PlayerBuzzed -> GamePhase.PlayerAnswering
        is PlayerSkipped -> {
            val newSkipVotes = oldState.skipVotePlayerIds + event.playerId
            if (oldState.copy(skipVotePlayerIds = newSkipVotes).allPlayersAccountedFor()) {
                GamePhase.ShowingAnswer
            } else {
                GamePhase.ShowingQuestion
            }
        }
        is TimerExpired -> if (_state.isTimerPaused) GamePhase.ShowingQuestion else GamePhase.ShowingAnswer
        is SkipQuestion -> GamePhase.ShowingAnswer
        is HostAccepted -> GamePhase.ShowingAnswer
        is HostRejected -> nextFailurePhase(oldState, oldState.answeringPlayerId)
        is AnswerTimerExpired -> nextFailurePhase(oldState, oldState.answeringPlayerId)
        is SkipRound -> GamePhase.RoundEnd
        is AnswerShown -> when {
            _state.isGameOver -> GamePhase.GameOver
            _state.isRoundComplete -> GamePhase.RoundEnd
            else -> GamePhase.ChoosingQuestion
        }
        is NextRound -> if (_state.isGameOver) GamePhase.GameOver else GamePhase.ChoosingPlayer
        // Phase-preserving events: every arm keeps the FSM in `_phase`.
        is PauseTimer, is ResumeTimer,
        is PlayerJoined, is PlayerLeft, is PlayerRenamed,
        is TimerTick, is AdjustPlayerScore, is AnswerTimerTick -> _phase
    }

    private fun nextFailurePhase(oldState: GameState, playerId: UUID?): GamePhase {
        val newFailedIds =
            if (playerId != null) oldState.failedBuzzPlayerIds + playerId
            else oldState.failedBuzzPlayerIds
        val candidate = oldState.copy(failedBuzzPlayerIds = newFailedIds)
        return if (candidate.allPlayersAccountedFor()) GamePhase.ShowingAnswer else GamePhase.ShowingQuestion
    }

    private fun findQuestion(id: UUID): Question<*>? =
        _state.pack.rounds
            .flatMap { it.themes }
            .flatMap { it.questions }
            .find { it.id == id }

    private fun notifyListeners() {
        listeners.forEach { it(_previousState, _state, _previousPhase, _phase) }
    }
}
