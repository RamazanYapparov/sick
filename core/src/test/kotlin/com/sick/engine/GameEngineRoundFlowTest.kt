package com.sick.engine

import com.sick.event.*
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import kotlin.test.*

class GameEngineRoundFlowTest {

    // 2 rounds, 1 question each — minimal setup for testing round/game transitions
    private fun engine(): GameEngine = GameEngine(minimalPackage(rounds = 2))

    private fun GameEngine.joinAndStart(name: String = "Alice"): Player {
        process(PlayerJoined(name))
        val player = state.players.first()
        process(StartGame)
        return player
    }

    private fun GameEngine.playAndAccept(player: Player, roundIdx: Int, questionIdx: Int = 0) {
        process(SelectActivePlayer(player.id))
        process(QuestionSelected(QUESTION_IDS[roundIdx][questionIdx]))
        process(QuestionRevealed)
        process(PlayerBuzzed(player.id))
        process(HostAccepted)
        process(AnswerShown)
    }

    @Test
    fun `completing last question in round transitions to RoundEnd`() {
        val engine = engine()
        val player = engine.joinAndStart()

        engine.playAndAccept(player, roundIdx = 0)

        assertEquals(GamePhase.RoundEnd, engine.phase)
    }

    @Test
    fun `NextRound advances to next round and transitions to ChoosingPlayer`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)

        val result = engine.process(NextRound)

        assertTrue(result.isRight())
        assertEquals(1, engine.state.currentRoundIndex)
        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
    }

    @Test
    fun `completing last question in last round transitions to RoundEnd then GameOver after NextRound`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)
        engine.process(NextRound)

        engine.playAndAccept(player, roundIdx = 1)
        assertEquals(GamePhase.RoundEnd, engine.phase)

        engine.process(NextRound)
        assertEquals(GamePhase.GameOver, engine.phase)
    }

    @Test
    fun `isGameOver is true in GameOver phase`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)
        engine.process(NextRound)
        engine.playAndAccept(player, roundIdx = 1)
        engine.process(NextRound)

        assertTrue(engine.state.isGameOver)
    }

    @Test
    fun `any event in GameOver returns InvalidEvent`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)
        engine.process(NextRound)
        engine.playAndAccept(player, roundIdx = 1)
        engine.process(NextRound)
        assertEquals(GamePhase.GameOver, engine.phase)

        val result = engine.process(StartGame)
        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
    }

    @Test
    fun `invalid event in RoundEnd returns InvalidEvent`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)
        assertEquals(GamePhase.RoundEnd, engine.phase)

        val result = engine.process(PlayerBuzzed(player.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
    }

    @Test
    fun `played questions from round 1 do not count for round 2 completion`() {
        val engine = engine()
        val player = engine.joinAndStart()
        engine.playAndAccept(player, roundIdx = 0)
        engine.process(NextRound)

        // After advancing to round 2, the round should NOT be complete
        assertFalse(engine.state.isRoundComplete)
    }

    @Test
    fun `SkipRound from ChoosingQuestion transitions to RoundEnd with all questions marked played`() {
        val engine = GameEngine(minimalPackage(rounds = 2, questionsPerTheme = 2))
        val player = engine.joinAndStart()
        engine.process(SelectActivePlayer(player.id))
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)

        engine.process(SkipRound)

        assertEquals(GamePhase.RoundEnd, engine.phase)
        assertTrue(engine.state.isRoundComplete)
    }

    @Test
    fun `SkipRound from ChoosingPlayer transitions to RoundEnd`() {
        val engine = engine()
        engine.joinAndStart()
        assertEquals(GamePhase.ChoosingPlayer, engine.phase)

        engine.process(SkipRound)

        assertEquals(GamePhase.RoundEnd, engine.phase)
    }

    @Test
    fun `NextRound after SkipRound advances to next round`() {
        val engine = engine()
        engine.joinAndStart()
        engine.process(SkipRound)

        engine.process(NextRound)

        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
        assertEquals(1, engine.state.currentRoundIndex)
    }

    @Test
    fun `NextRound after SkipRound on last round transitions to GameOver`() {
        val engine = engine()
        engine.joinAndStart()
        engine.process(SkipRound)
        engine.process(NextRound)
        engine.process(SkipRound)
        assertEquals(GamePhase.RoundEnd, engine.phase)

        engine.process(NextRound)

        assertEquals(GamePhase.GameOver, engine.phase)
    }
}
