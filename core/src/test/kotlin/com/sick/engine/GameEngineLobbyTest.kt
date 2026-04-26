package com.sick.engine

import com.sick.event.*
import com.sick.model.GameState
import com.sick.service.PlayerError
import com.sick.state.GamePhase
import com.sick.test.minimalPackage
import java.util.UUID
import kotlin.test.*

class GameEngineLobbyTest {

    private fun engine() = GameEngine(minimalPackage())

    @Test
    fun `PlayerJoined adds player and phase stays Lobby`() {
        val engine = engine()
        val result = engine.process(PlayerJoined("Alice"))
        assertTrue(result.isRight())
        assertEquals(1, engine.state.players.size)
        assertEquals("Alice", engine.state.players.first().name)
        assertEquals(GamePhase.Lobby, engine.phase)
    }

    @Test
    fun `PlayerLeft removes player and phase stays Lobby`() {
        val engine = engine()
        engine.process(PlayerJoined("Alice"))
        val alice = engine.state.players.first()
        val result = engine.process(PlayerLeft(alice.id))
        assertTrue(result.isRight())
        assertTrue(engine.state.players.isEmpty())
        assertEquals(GamePhase.Lobby, engine.phase)
    }

    @Test
    fun `PlayerLeft with unknown ID returns PlayerError NotFound`() {
        val engine = engine()
        val result = engine.process(PlayerLeft(UUID.randomUUID()))
        assertTrue(result.isLeft())
        val error = result.leftOrNull()!!
        assertIs<GameError.PlayerError>(error)
        assertIs<PlayerError.NotFound>((error as GameError.PlayerError).inner)
    }

    @Test
    fun `PlayerRenamed updates player name and phase stays Lobby`() {
        val engine = engine()
        engine.process(PlayerJoined("Alice"))
        val alice = engine.state.players.first()
        val result = engine.process(PlayerRenamed(alice.id, "Alicia"))
        assertTrue(result.isRight())
        assertEquals("Alicia", engine.state.players.first().name)
        assertEquals(GamePhase.Lobby, engine.phase)
    }

    @Test
    fun `invalid event in Lobby returns InvalidEvent error`() {
        val engine = engine()
        val result = engine.process(QuestionSelected(UUID.randomUUID()))
        assertTrue(result.isLeft())
        assertIs<GameError.InvalidEvent>(result.leftOrNull()!!)
    }

    @Test
    fun `StartGame transitions phase to ChoosingPlayer`() {
        val engine = engine()
        engine.process(PlayerJoined("Alice"))
        val result = engine.process(StartGame)
        assertTrue(result.isRight())
        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
    }

    @Test
    fun `listener is notified on each successful event`() {
        val engine = engine()
        val notifications = mutableListOf<Pair<GameState, GamePhase>>()
        engine.addListener { state, phase -> notifications.add(state to phase) }

        engine.process(PlayerJoined("Alice"))
        engine.process(PlayerJoined("Bob"))
        engine.process(StartGame)

        assertEquals(3, notifications.size)
        assertEquals(GamePhase.ChoosingPlayer, notifications.last().second)
    }

    @Test
    fun `StartGame with no players is accepted by the engine`() {
        val engine = engine()
        val result = engine.process(StartGame)
        assertTrue(result.isRight())
        assertEquals(GamePhase.ChoosingPlayer, engine.phase)
    }

    @Test
    fun `failed events do not trigger listener`() {
        val engine = engine()
        var callCount = 0
        engine.addListener { _, _ -> callCount++ }

        engine.process(QuestionSelected(UUID.randomUUID()))  // invalid in Lobby

        assertEquals(0, callCount)
    }
}
