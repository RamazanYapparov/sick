package com.sick.engine

import arrow.core.Either
import com.sick.event.*
import com.sick.model.GameState
import com.sick.model.Buzz
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import kotlin.test.*

class GameEngineReactionTimeTest {

    private var nowMillis = 0L
    private val engine = GameEngine(minimalPackage(questionsPerTheme = 2)) { nowMillis * 1_000_000 }
    private lateinit var alice: Player
    private lateinit var bob: Player
    private lateinit var carol: Player

    @BeforeTest
    fun setUp() {
        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        engine.process(PlayerJoined("Carol"))
        alice = engine.state.players[0]
        bob = engine.state.players[1]
        carol = engine.state.players[2]
        engine.process(StartGame)
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
    }

    private fun revealAt(millis: Long) {
        nowMillis = millis
        engine.process(QuestionRevealed)
    }

    private fun buzzAt(millis: Long, player: Player): Either<GameError, GameState> {
        nowMillis = millis
        return engine.process(PlayerBuzzed(player.id))
    }

    @Test
    fun `first buzz answers and later buzzes are recorded as Late Buzzes from window opening`() {
        revealAt(1_000)
        buzzAt(1_010, alice)
        buzzAt(1_011, bob)
        buzzAt(1_012, carol)

        assertEquals(GamePhase.PlayerAnswering, engine.phase)
        assertEquals(alice.id, engine.state.answeringPlayerId)
        assertEquals(listOf(Buzz(alice.id, 10), Buzz(bob.id, 11), Buzz(carol.id, 12)), engine.state.buzzes)
    }

    @Test
    fun `second buzz from the same player in one window is rejected`() {
        revealAt(0)
        buzzAt(10, alice)
        buzzAt(20, bob)

        assertTrue(buzzAt(30, alice).isLeft())
        assertTrue(buzzAt(40, bob).isLeft())
        assertEquals(listOf(Buzz(alice.id, 10), Buzz(bob.id, 20)), engine.state.buzzes)
    }

    @Test
    fun `wrong answer reopens the window and clears previous times`() {
        revealAt(0)
        buzzAt(10, alice)
        buzzAt(11, bob)
        nowMillis = 5_000
        engine.process(HostRejected)

        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertEquals(emptyList(), engine.state.buzzes)

        buzzAt(5_300, bob)
        assertTrue(buzzAt(5_400, alice).isLeft(), "failed player cannot Late Buzz")
        buzzAt(5_500, carol)
        assertEquals(listOf(Buzz(bob.id, 300), Buzz(carol.id, 500)), engine.state.buzzes)
    }

    @Test
    fun `resume after pause opens a new window`() {
        revealAt(0)
        nowMillis = 2_000
        engine.process(PauseTimer)
        nowMillis = 60_000
        engine.process(ResumeTimer)

        buzzAt(60_250, carol)
        assertEquals(listOf(Buzz(carol.id, 250)), engine.state.buzzes)
    }

    @Test
    fun `pause during answering rejects Late Buzz and resume keeps the window`() {
        revealAt(0)
        buzzAt(100, alice)
        engine.process(PauseTimer)
        assertTrue(buzzAt(200, bob).isLeft())
        engine.process(ResumeTimer)

        buzzAt(300, bob)
        assertEquals(listOf(Buzz(alice.id, 100), Buzz(bob.id, 300)), engine.state.buzzes)
    }

    @Test
    fun `times stay through ShowingAnswer and clear on the next question`() {
        revealAt(0)
        buzzAt(10, alice)
        buzzAt(20, bob)
        engine.process(HostAccepted)
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
        assertEquals(listOf(Buzz(alice.id, 10), Buzz(bob.id, 20)), engine.state.buzzes)

        engine.process(AnswerShown)
        engine.process(QuestionSelected(QUESTION_IDS[0][1]))
        assertEquals(emptyList(), engine.state.buzzes)
    }
}
