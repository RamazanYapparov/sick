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

    @Test
    fun `lowestScoreCandidates returns empty when no players`() {
        assertEquals(emptyList(), state.lowestScoreCandidates())
    }

    @Test
    fun `lowestScoreCandidates returns all players when all scores are zero`() {
        val s = state.addPlayer("Alice").getOrNull()!!.addPlayer("Bob").getOrNull()!!
        assertEquals(2, s.lowestScoreCandidates().size)
    }

    @Test
    fun `lowestScoreCandidates returns single player with unique lowest score`() {
        val s = state.addPlayer("Alice").getOrNull()!!.addPlayer("Bob").getOrNull()!!
        val alice = s.players.first { it.name == "Alice" }
        val withScores = s.copy(players = s.players.map {
            if (it.id == alice.id) it.copy(score = -100) else it.copy(score = 200)
        })
        val candidates = withScores.lowestScoreCandidates()
        assertEquals(1, candidates.size)
        assertEquals("Alice", candidates.single().name)
    }

    @Test
    fun `lowestScoreCandidates returns all tied players when multiple share minimum score`() {
        val s = state.addPlayer("Alice").getOrNull()!!
            .addPlayer("Bob").getOrNull()!!
            .addPlayer("Carol").getOrNull()!!
        val withScores = s.copy(players = s.players.map {
            when (it.name) {
                "Alice" -> it.copy(score = 0)
                "Bob"   -> it.copy(score = 0)
                else    -> it.copy(score = 500)
            }
        })
        val candidates = withScores.lowestScoreCandidates()
        assertEquals(2, candidates.size)
        assertTrue(candidates.any { it.name == "Alice" })
        assertTrue(candidates.any { it.name == "Bob" })
    }
}
