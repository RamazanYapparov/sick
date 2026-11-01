package com.sick.engine

import com.sick.event.*
import com.sick.model.Player
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import kotlin.test.*

class GameEnginePlayerRemovalTest {

    private fun startedGame(vararg names: String): Pair<GameEngine, List<Player>> {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        names.forEach { engine.process(PlayerJoined(it)) }
        engine.process(StartGame)
        return engine to engine.state.players
    }

    @Test
    fun `removing the chooser in ChoosingQuestion returns to ChoosingPlayer`() {
        val (engine, players) = startedGame("Alice", "Bob")
        val (alice, bob) = players
        engine.process(SelectActivePlayer(alice.id))

        assertTrue(engine.process(PlayerLeft(alice.id)).isRight())

        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
        assertNull(engine.state.activePlayerId)
        assertEquals(listOf(bob), engine.state.players)
    }

    @Test
    fun `removing another player in ChoosingQuestion keeps the chooser`() {
        val (engine, players) = startedGame("Alice", "Bob")
        val (alice, bob) = players
        engine.process(SelectActivePlayer(alice.id))

        assertTrue(engine.process(PlayerLeft(bob.id)).isRight())

        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
        assertEquals(alice.id, engine.state.activePlayerId)
    }

    @Test
    fun `removal is rejected while a question is live`() {
        val (engine, players) = startedGame("Alice", "Bob")
        val (alice, bob) = players
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)

        assertIs<GameError.InvalidEvent>(engine.process(PlayerLeft(bob.id)).leftOrNull())
        engine.process(PlayerBuzzed(bob.id))
        assertIs<GameError.InvalidEvent>(engine.process(PlayerLeft(bob.id)).leftOrNull())
        engine.process(HostAccepted())
        assertEquals(GamePhase.ShowingAnswer, engine.phase)
        assertIs<GameError.InvalidEvent>(engine.process(PlayerLeft(bob.id)).leftOrNull())

        assertEquals(2, engine.state.players.size)
    }

    @Test
    fun `the last player cannot be removed once the game started`() {
        val (engine, players) = startedGame("Alice")

        assertIs<GameError.InvalidEvent>(engine.process(PlayerLeft(players.single().id)).leftOrNull())
        assertEquals(players, engine.state.players)
    }

    @Test
    fun `a removed player cannot buzz or skip`() {
        val (engine, players) = startedGame("Alice", "Bob", "Carol")
        val (alice, bob, carol) = players
        engine.process(SelectActivePlayer(alice.id))
        engine.process(PlayerLeft(bob.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        engine.process(QuestionRevealed)

        assertIs<GameError.PlayerError>(engine.process(PlayerSkipped(bob.id)).leftOrNull())
        assertIs<GameError.PlayerError>(engine.process(PlayerBuzzed(bob.id)).leftOrNull())
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertTrue(engine.process(PlayerBuzzed(carol.id)).isRight())
    }
}
