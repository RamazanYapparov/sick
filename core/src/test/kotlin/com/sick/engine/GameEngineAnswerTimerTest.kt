package com.sick.engine

import com.sick.event.*
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import java.util.UUID
import kotlin.test.*

class GameEngineAnswerTimerTest {

    private fun engineWithTwoPlayers(): Triple<GameEngine, Player, Player> {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        val alice = engine.state.players[0]
        val bob = engine.state.players[1]
        engine.process(StartGame)
        return Triple(engine, alice, bob)
    }

    private fun GameEngine.advanceToPlayerAnswering(player: Player) {
        process(SelectActivePlayer(player.id))
        process(QuestionSelected(QUESTION_IDS[0][0]))
        process(QuestionRevealed)
        process(PlayerBuzzed(player.id))
    }

    @Test
    fun `GameState has answerTimerSeconds default of 15`() {
        val engine = GameEngine(minimalPackage())
        assertEquals(15, engine.state.answerTimerSeconds)
    }

    @Test
    fun `PlayerBuzzed resets answerTimerRemaining to 15`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        assertEquals(15, engine.state.answerTimerRemaining)
        assertEquals(GamePhase.PlayerAnswering, engine.phase)
    }

    @Test
    fun `AnswerTimerTick decrements answerTimerRemaining`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        val before = engine.state.answerTimerRemaining
        val result = engine.process(AnswerTimerTick)
        assertTrue(result.isRight())
        assertEquals(before - 1, engine.state.answerTimerRemaining)
        assertEquals(GamePhase.PlayerAnswering, engine.phase)
    }

    @Test
    fun `AnswerTimerTick while paused does not decrement`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        engine.process(PauseTimer)
        val before = engine.state.answerTimerRemaining
        val result = engine.process(AnswerTimerTick)
        assertTrue(result.isRight())
        assertEquals(before, engine.state.answerTimerRemaining)
        assertTrue(engine.state.isTimerPaused)
    }

    @Test
    fun `AnswerTimerRemaining does not go below zero`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        repeat(engine.state.answerTimerSeconds + 5) { engine.process(AnswerTimerTick) }
        assertEquals(0, engine.state.answerTimerRemaining)
    }

    @Test
    fun `AnswerTimerExpired with single player transitions to ShowingAnswer and flips active player`() {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        val alice = engine.state.players.first()
        engine.process(StartGame)
        engine.advanceToPlayerAnswering(alice)

        val result = engine.process(AnswerTimerExpired)

        assertTrue(result.isRight())
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
        assertEquals(-100, engine.state.findPlayer(alice.id)!!.score)
        assertTrue(engine.state.failedBuzzPlayerIds.isEmpty())  // reset on all-accounted
    }

    @Test
    fun `AnswerTimerExpired with other players returns to ShowingQuestion`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)

        val result = engine.process(AnswerTimerExpired)

        assertTrue(result.isRight())
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertEquals(-100, engine.state.findPlayer(alice.id)!!.score)
        assertTrue(alice.id in engine.state.failedBuzzPlayerIds)
        assertFalse(bob.id in engine.state.failedBuzzPlayerIds)
        assertEquals(15, engine.state.answerTimerRemaining)  // reset on partial transition
    }

    @Test
    fun `AnswerTimerExpired allows negative scores for NoRisk questions`() {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Solo"))
        val solo = engine.state.players.first()
        engine.process(StartGame)
        engine.advanceToPlayerAnswering(solo)
        // Solo starts at score 0
        assertEquals(0, solo.score)
        engine.process(AnswerTimerExpired)
        // Allow negative
        assertEquals(-100, engine.state.findPlayer(solo.id)!!.score)
    }

    @Test
    fun `PauseTimer valid in PlayerAnswering`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        val result = engine.process(PauseTimer)
        assertTrue(result.isRight())
        assertTrue(engine.state.isTimerPaused)
    }

    @Test
    fun `ResumeTimer valid in PlayerAnswering clears flag`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        engine.process(PauseTimer)
        val result = engine.process(ResumeTimer)
        assertTrue(result.isRight())
        assertFalse(engine.state.isTimerPaused)
    }

    @Test
    fun `Fresh buzz-in resets answerTimerRemaining after a Wrong`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        // Drop Alice's answer to 10
        repeat(5) { engine.process(AnswerTimerTick) }
        assertEquals(10, engine.state.answerTimerRemaining)
        engine.process(HostRejected)
        // Alice wronged -> ShowingQuestion (bob remains)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)

        engine.process(PlayerBuzzed(bob.id))
        // Fresh buzzer -> answer timer reset
        assertEquals(15, engine.state.answerTimerRemaining)
    }

    @Test
    fun `AnswerTimerExpired rejected when no answeringPlayerId set`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToPlayerAnswering(alice)
        engine.process(HostAccepted)  // clears answeringPlayerId
        // Now engine is in ShowingAnswer; replay AnswerTimerExpired — should be either
        // accepted (showingAnswer) or rejected (no answering player). Either way, the
        // answer timer state must NOT change in a way that breaks invariants.
        val result = engine.process(AnswerTimerExpired)
        // In ShowingAnswer phase; AnswerTimerExpired is not allowed there by allowlist
        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
    }

    @Test
    fun `QuestionSelected resets answerTimerRemaining to 15`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        // Drive the engine through a buzz cycle that decrements answerTimerRemaining
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(alice.id))
        repeat(5) { engine.process(AnswerTimerTick) }
        assertEquals(10, engine.state.answerTimerRemaining)
        // Now move off the question via AnswerShown, then re-select a different question.
        engine.process(HostAccepted)
        engine.process(AnswerShown)
        engine.process(QuestionSelected(QUESTION_IDS[0][1]))
        assertEquals(15, engine.state.answerTimerRemaining)
    }

    @Test
    fun `AnswerTimerExpired rejected outside PlayerAnswering allowlist`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.process(SelectActivePlayer(alice.id))
        // Phase is ChoosingQuestion — AnswerTimerExpired not in allowlist
        val result = engine.process(AnswerTimerExpired)
        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
    }

    @Test
    fun `third player auto-wrong makes all failed and goes to ShowingAnswer`() {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        engine.process(PlayerJoined("Carol"))
        engine.process(StartGame)
        engine.process(SelectActivePlayer(engine.state.players[0].id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)

        // Alice answers wrong manually
        engine.process(PlayerBuzzed(engine.state.players[0].id))
        engine.process(HostRejected)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)

        // Bob auto-wrong -> failedBuzz has {Alice}, remaining = Bob, Carol
        engine.process(PlayerBuzzed(engine.state.players[1].id))
        engine.process(AnswerTimerExpired)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertTrue(engine.state.players[1].id in engine.state.failedBuzzPlayerIds)

        // Carol auto-wrong -> all failed
        engine.process(PlayerBuzzed(engine.state.players[2].id))
        engine.process(AnswerTimerExpired)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }
}
