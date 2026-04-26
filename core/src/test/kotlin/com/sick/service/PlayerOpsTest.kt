package com.sick.service

import com.sick.model.GameState
import com.sick.test.minimalPackage
import java.util.UUID
import kotlin.test.*

class PlayerOpsTest {

    private val state = GameState(pack = minimalPackage())

    @Test
    fun `addPlayer with valid name adds player to state`() {
        val result = state.addPlayer("Alice")
        assertTrue(result.isRight())
        val newState = result.getOrNull()!!
        assertEquals(1, newState.players.size)
        assertEquals("Alice", newState.players.first().name)
    }

    @Test
    fun `addPlayer with duplicate name returns AlreadyExists`() {
        val withAlice = state.addPlayer("Alice").getOrNull()!!
        val result = withAlice.addPlayer("Alice")
        assertTrue(result.isLeft())
        assertIs<PlayerError.AlreadyExists>(result.leftOrNull()!!)
    }

    @Test
    fun `addPlayer with blank name returns InvalidName`() {
        val result = state.addPlayer("  ")
        assertTrue(result.isLeft())
        assertIs<PlayerError.InvalidName>(result.leftOrNull()!!)
    }

    @Test
    fun `addPlayer with empty string returns InvalidName`() {
        val result = state.addPlayer("")
        assertTrue(result.isLeft())
        assertIs<PlayerError.InvalidName>(result.leftOrNull()!!)
    }

    @Test
    fun `removePlayer removes existing player`() {
        val withAlice = state.addPlayer("Alice").getOrNull()!!
        val alice = withAlice.players.first()
        val result = withAlice.removePlayer(alice.id)
        assertTrue(result.isRight())
        assertTrue(result.getOrNull()!!.players.isEmpty())
    }

    @Test
    fun `removePlayer with unknown ID returns NotFound`() {
        val result = state.removePlayer(UUID.randomUUID())
        assertTrue(result.isLeft())
        assertIs<PlayerError.NotFound>(result.leftOrNull()!!)
    }

    @Test
    fun `renamePlayer changes player name`() {
        val withAlice = state.addPlayer("Alice").getOrNull()!!
        val alice = withAlice.players.first()
        val result = withAlice.renamePlayer(alice.id, "Alicia")
        assertTrue(result.isRight())
        assertEquals("Alicia", result.getOrNull()!!.findPlayer(alice.id)!!.name)
    }

    @Test
    fun `renamePlayer with unknown ID returns NotFound`() {
        val result = state.renamePlayer(UUID.randomUUID(), "Bob")
        assertTrue(result.isLeft())
        assertIs<PlayerError.NotFound>(result.leftOrNull()!!)
    }

    @Test
    fun `renamePlayer to duplicate name returns AlreadyExists`() {
        val withBoth = state.addPlayer("Alice").getOrNull()!!.addPlayer("Bob").getOrNull()!!
        val alice = withBoth.players.first { it.name == "Alice" }
        val result = withBoth.renamePlayer(alice.id, "Bob")
        assertTrue(result.isLeft())
        assertIs<PlayerError.AlreadyExists>(result.leftOrNull()!!)
    }

    @Test
    fun `renamePlayer to blank name returns InvalidName`() {
        val withAlice = state.addPlayer("Alice").getOrNull()!!
        val alice = withAlice.players.first()
        val result = withAlice.renamePlayer(alice.id, "  ")
        assertTrue(result.isLeft())
        assertIs<PlayerError.InvalidName>(result.leftOrNull()!!)
    }

    @Test
    fun `renamePlayer to the same name succeeds`() {
        val withAlice = state.addPlayer("Alice").getOrNull()!!
        val alice = withAlice.players.first()
        val result = withAlice.renamePlayer(alice.id, "Alice")
        assertTrue(result.isRight())
        assertEquals("Alice", result.getOrNull()!!.findPlayer(alice.id)!!.name)
    }
}
