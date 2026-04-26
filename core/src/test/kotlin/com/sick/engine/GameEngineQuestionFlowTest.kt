package com.sick.engine

import com.sick.event.*
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import java.util.UUID
import kotlin.test.*

class GameEngineQuestionFlowTest {

    // 2 questions per theme so playing 1 question does NOT complete the round
    private fun engineWithPlayer(): Pair<GameEngine, Player> {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        val player = engine.state.players.first()
        engine.process(StartGame)
        return engine to player
    }

    @Test
    fun `SelectActivePlayer sets activePlayerId and transitions to ChoosingQuestion`() {
        val (engine, player) = engineWithPlayer()
        val result = engine.process(SelectActivePlayer(player.id))
        assertTrue(result.isRight())
        assertEquals(player.id, engine.state.activePlayerId)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `QuestionSelected sets currentQuestion marks it played and transitions to ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        val questionId = QUESTION_IDS[0][0]

        val result = engine.process(QuestionSelected(questionId))

        assertTrue(result.isRight())
        assertEquals(questionId, engine.state.currentQuestion?.id)
        assertTrue(questionId in engine.state.playedQuestionIds)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `QuestionSelected resets timerRemaining to 30`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        assertEquals(30, engine.state.timerRemaining)
    }

    @Test
    fun `QuestionSelected clears failedBuzzPlayerIds from previous question`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(PlayerBuzzed(player.id))
        engine.process(HostRejected)  // player now in failedBuzzPlayerIds
        engine.process(TimerExpired)  // back to ChoosingPlayer
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][1]))  // new question

        assertTrue(engine.state.failedBuzzPlayerIds.isEmpty())
    }

    @Test
    fun `QuestionSelected with unknown ID returns QuestionNotFound`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        val result = engine.process(QuestionSelected(UUID.randomUUID()))
        assertTrue(result.isLeft())
        assertIs<GameError.QuestionNotFound>(result.leftOrNull()!!)
    }

    @Test
    fun `PlayerBuzzed sets answeringPlayerId and transitions to PlayerAnswering`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))

        val result = engine.process(PlayerBuzzed(player.id))

        assertTrue(result.isRight())
        assertEquals(player.id, engine.state.answeringPlayerId)
        assertEquals(GamePhase.PlayerAnswering, engine.phase)
    }

    @Test
    fun `PlayerBuzzed by player already failed returns InvalidEvent`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(PlayerBuzzed(player.id))
        engine.process(HostRejected)  // player added to failedBuzzPlayerIds, back to ShowingQuestion

        val result = engine.process(PlayerBuzzed(player.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `TimerTick decrements timerRemaining and phase stays ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        val before = engine.state.timerRemaining

        val result = engine.process(TimerTick)

        assertTrue(result.isRight())
        assertEquals(before - 1, engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `TimerRemaining does not go below zero`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        repeat(engine.state.timerSeconds + 5) { engine.process(TimerTick) }
        assertEquals(0, engine.state.timerRemaining)
    }

    @Test
    fun `TimerExpired clears currentQuestion and transitions to ChoosingPlayer`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        assertNotNull(engine.state.currentQuestion)

        val result = engine.process(TimerExpired)

        assertTrue(result.isRight())
        assertNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
    }

    @Test
    fun `HostAccepted adds score clears answering state and transitions to ChoosingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))  // price = 100
        engine.process(PlayerBuzzed(player.id))

        val result = engine.process(HostAccepted)

        assertTrue(result.isRight())
        assertEquals(100, engine.state.findPlayer(player.id)!!.score)
        assertNull(engine.state.answeringPlayerId)
        assertNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `HostRejected subtracts score adds player to failed list and goes back to ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))  // price = 100
        engine.process(PlayerBuzzed(player.id))

        val result = engine.process(HostRejected)

        assertTrue(result.isRight())
        assertEquals(-100, engine.state.findPlayer(player.id)!!.score)
        assertTrue(player.id in engine.state.failedBuzzPlayerIds)
        assertNull(engine.state.answeringPlayerId)
        assertNotNull(engine.state.currentQuestion)  // question still active
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }
}
