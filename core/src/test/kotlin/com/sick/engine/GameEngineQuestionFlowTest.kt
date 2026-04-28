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

    private fun engineWithTwoPlayers(): Triple<GameEngine, Player, Player> {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        val alice = engine.state.players[0]
        val bob = engine.state.players[1]
        engine.process(StartGame)
        return Triple(engine, alice, bob)
    }

    // Helper: advance engine to ShowingAnswer for the first question
    private fun GameEngine.advanceToShowingAnswer(player: Player) {
        process(SelectActivePlayer(player.id))
        process(QuestionSelected(QUESTION_IDS[0][0]))
        process(QuestionRevealed)
        process(PlayerBuzzed(player.id))
        process(HostAccepted)
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
    fun `QuestionSelected sets currentQuestion marks it played and transitions to RevealingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        val questionId = QUESTION_IDS[0][0]

        val result = engine.process(QuestionSelected(questionId))

        assertTrue(result.isRight())
        assertEquals(questionId, engine.state.currentQuestion?.id)
        assertTrue(questionId in engine.state.playedQuestionIds)
        assertEquals(GamePhase.RevealingQuestion, engine.phase)
    }

    @Test
    fun `QuestionRevealed transitions from RevealingQuestion to ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        assertEquals(GamePhase.RevealingQuestion, engine.phase)

        val result = engine.process(QuestionRevealed)

        assertTrue(result.isRight())
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertNotNull(engine.state.currentQuestion)
    }

    @Test
    fun `SkipQuestion during RevealingQuestion transitions to ShowingAnswer`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        assertEquals(GamePhase.RevealingQuestion, engine.phase)

        val result = engine.process(SkipQuestion)

        assertTrue(result.isRight())
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
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
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(player.id))
        engine.process(HostRejected)  // all players failed → ShowingAnswer
        engine.process(AnswerShown)   // back to ChoosingQuestion
        engine.process(QuestionSelected(QUESTION_IDS[0][1]))

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
        engine.process(QuestionRevealed)

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
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(player.id))
        engine.process(HostRejected)  // player added to failedBuzzPlayerIds → ShowingAnswer (only player)

        val result = engine.process(PlayerBuzzed(player.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }

    @Test
    fun `PauseTimer marks state paused without leaving ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)

        val result = engine.process(PauseTimer)

        assertTrue(result.isRight())
        assertTrue(engine.state.isTimerPaused)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `PlayerBuzzed while paused returns InvalidEvent`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PauseTimer)

        val result = engine.process(PlayerBuzzed(player.id))

        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `ResumeTimer clears paused flag`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PauseTimer)

        val result = engine.process(ResumeTimer)

        assertTrue(result.isRight())
        assertFalse(engine.state.isTimerPaused)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `TimerTick decrements timerRemaining and phase stays ShowingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        val before = engine.state.timerRemaining

        val result = engine.process(TimerTick)

        assertTrue(result.isRight())
        assertEquals(before - 1, engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `TimerTick while paused leaves timer untouched`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PauseTimer)
        val before = engine.state.timerRemaining

        val result = engine.process(TimerTick)

        assertTrue(result.isRight())
        assertEquals(before, engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `TimerRemaining does not go below zero`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        repeat(engine.state.timerSeconds + 5) { engine.process(TimerTick) }
        assertEquals(0, engine.state.timerRemaining)
    }

    @Test
    fun `TimerExpired transitions to ShowingAnswer and keeps currentQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        assertNotNull(engine.state.currentQuestion)

        val result = engine.process(TimerExpired)

        assertTrue(result.isRight())
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }

    @Test
    fun `HostAccepted adds score keeps currentQuestion and transitions to ShowingAnswer`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))  // price = 100
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(player.id))

        val result = engine.process(HostAccepted)

        assertTrue(result.isRight())
        assertEquals(100, engine.state.findPlayer(player.id)!!.score)
        assertNull(engine.state.answeringPlayerId)
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }

    @Test
    fun `SkipQuestion transitions to ShowingAnswer and keeps currentQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        assertNotNull(engine.state.currentQuestion)

        val result = engine.process(SkipQuestion)

        assertTrue(result.isRight())
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }

    @Test
    fun `HostRejected with only player failed transitions to ShowingAnswer`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(player.id))

        val result = engine.process(HostRejected)

        assertTrue(result.isRight())
        assertTrue(player.id in engine.state.failedBuzzPlayerIds)
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
    }

    @Test
    fun `HostRejected when players remain goes back to ShowingQuestion`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(alice.id))

        val result = engine.process(HostRejected)

        assertTrue(result.isRight())
        assertTrue(alice.id in engine.state.failedBuzzPlayerIds)
        assertFalse(bob.id in engine.state.failedBuzzPlayerIds)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `AnswerShown clears question state and transitions to ChoosingQuestion`() {
        val (engine, player) = engineWithPlayer()
        engine.advanceToShowingAnswer(player)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)

        val result = engine.process(AnswerShown)

        assertTrue(result.isRight())
        assertNull(engine.state.currentQuestion)
        assertTrue(engine.state.failedBuzzPlayerIds.isEmpty())
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `AnswerShown on last question in round transitions to RoundEnd`() {
        val engine = GameEngine(minimalPackage(rounds = 2, questionsPerTheme = 1))
        engine.process(PlayerJoined("Alice"))
        val player = engine.state.players.first()
        engine.process(StartGame)
        engine.advanceToShowingAnswer(player)

        val result = engine.process(AnswerShown)

        assertTrue(result.isRight())
        assertEquals(GamePhase.RoundEnd, engine.phase)
    }

    @Test
    fun `after correct answer player picks next question after AnswerShown without re-selecting chooser`() {
        val (engine, player) = engineWithPlayer()
        engine.process(SelectActivePlayer(player.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(player.id))
        engine.process(HostAccepted)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)

        engine.process(AnswerShown)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
        assertEquals(player.id, engine.state.activePlayerId)

        // no SelectActivePlayer — answering player is already the chooser
        val result = engine.process(QuestionSelected(QUESTION_IDS[0][1]))

        assertTrue(result.isRight())
        assertEquals(GamePhase.RevealingQuestion, engine.phase)
    }

    @Test
    fun `HostRejected subtracts score adds player to failed list`() {
        val (engine, alice, bob) = engineWithTwoPlayers()
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))  // price = 100
        engine.process(QuestionRevealed)
        engine.process(PlayerBuzzed(alice.id))

        val result = engine.process(HostRejected)

        assertTrue(result.isRight())
        assertEquals(-100, engine.state.findPlayer(alice.id)!!.score)
        assertTrue(alice.id in engine.state.failedBuzzPlayerIds)
        assertNull(engine.state.answeringPlayerId)
        assertNotNull(engine.state.currentQuestion)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }
}
