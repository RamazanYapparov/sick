package com.sick.engine

import com.sick.event.*
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import kotlin.test.*

class GameEnginePlayerSkipTest {

    private fun engineWithTwoPlayers(): Triple<GameEngine, Player, Player> {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        val alice = engine.state.players[0]
        val bob = engine.state.players[1]
        engine.process(StartGame)
        return Triple(engine, alice, bob)
    }

    private data class ThreePlayerGame(
        val engine: GameEngine,
        val alice: Player,
        val bob: Player,
        val carol: Player,
    )

    private fun engineWithThreePlayers(): ThreePlayerGame {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        engine.process(PlayerJoined("Carol"))
        engine.process(StartGame)
        val (a, b, c) = engine.state.players
        return ThreePlayerGame(engine, a, b, c)
    }

    private fun GameEngine.advanceToShowingQuestion(activeId: java.util.UUID, questionId: java.util.UUID) {
        process(SelectActivePlayer(activeId))
        process(QuestionSelected(questionId))
        process(QuestionRevealed)
    }

    @Test
    fun `eligible player vote is recorded and phase stays ShowingQuestion`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])

        val result = engine.process(PlayerSkipped(alice.id))

        assertTrue(result.isRight())
        assertTrue(alice.id in engine.state.skipVotePlayerIds)
        assertFalse(bob.id in engine.state.skipVotePlayerIds)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `player in failedBuzzPlayerIds cannot vote to skip`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])
        engine.process(PlayerBuzzed(alice.id))
        engine.process(HostRejected)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertTrue(alice.id in engine.state.failedBuzzPlayerIds)

        val result = engine.process(PlayerSkipped(alice.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertFalse(alice.id in engine.state.skipVotePlayerIds)
    }

    @Test
    fun `player cannot vote to skip twice`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])
        engine.process(PlayerSkipped(alice.id))

        val result = engine.process(PlayerSkipped(alice.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertEquals(setOf(alice.id), engine.state.skipVotePlayerIds)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `skip vote rejected outside ShowingQuestion phase`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        // Phase is RevealingQuestion (not ShowingQuestion yet)
        assertEquals(GamePhase.RevealingQuestion, engine.phase)

        val result = engine.process(PlayerSkipped(alice.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertTrue(engine.state.skipVotePlayerIds.isEmpty())
    }

    @Test
    fun `last eligible player vote triggers auto-skip`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])
        engine.process(PlayerSkipped(alice.id))
        assertEquals(GamePhase.ShowingQuestion, engine.phase)

        val result = engine.process(PlayerSkipped(bob.id))

        assertTrue(result.isRight())
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
        assertTrue(engine.state.skipVotePlayerIds.isEmpty())
        assertTrue(engine.state.failedBuzzPlayerIds.isEmpty())
        assertNull(engine.state.answeringPlayerId)
        assertEquals(0, engine.state.timerRemaining)
        assertNotNull(engine.state.currentQuestion)
    }

    @Test
    fun `auto-skip threshold accounts for failed players`() {
        val (engine, alice, bob, carol) = engineWithThreePlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])
        // Alice and Bob each fail a buzz; Carol remains eligible
        engine.process(PlayerBuzzed(alice.id))
        engine.process(HostRejected)
        engine.process(PlayerBuzzed(bob.id))
        engine.process(HostRejected)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertEquals(setOf(alice.id, bob.id), engine.state.failedBuzzPlayerIds)

        val result = engine.process(PlayerSkipped(carol.id))

        assertTrue(result.isRight())
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
        assertTrue(engine.state.skipVotePlayerIds.isEmpty())
        assertTrue(engine.state.failedBuzzPlayerIds.isEmpty())
    }

    @Test
    fun `skipVotePlayerIds is empty after QuestionSelected`() {
        val (engine, alice, _) = engineWithTwoPlayers()
        engine.advanceToShowingQuestion(alice.id, QUESTION_IDS[0][0])
        engine.process(PlayerSkipped(alice.id))
        assertTrue(alice.id in engine.state.skipVotePlayerIds)

        // Host skips current question, then a new question is selected
        engine.process(SkipQuestion)
        engine.process(AnswerShown)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)

        engine.process(QuestionSelected(QUESTION_IDS[0][1]))

        assertTrue(engine.state.skipVotePlayerIds.isEmpty())
    }
}
