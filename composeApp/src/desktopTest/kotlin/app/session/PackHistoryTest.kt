package app.session

import com.sick.engine.GameEngine
import com.sick.event.GameEvent
import com.sick.event.NextRound
import com.sick.event.PlayerJoined
import com.sick.event.SkipRound
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackHistoryTest {
    private val dir = Files.createTempDirectory("pack-history")
    private val file = dir.resolve("pack-history.json")

    @Test
    fun `a game started is Played, one played to its end is Finished, and both survive a restart`() {
        val history = PackHistory(file)
        play(history, "a", at = 1, StartGame, SkipRound, NextRound)
        play(history, "a", at = 2, StartGame)
        play(history, "b", at = 3, StartGame, SkipRound)

        assertEquals(
            mapOf("a" to PackRecord(2, finished = true), "b" to PackRecord(3)),
            PackHistory(file).records,
        )
    }

    @Test
    fun `an unreadable file is moved aside and the history starts empty`() {
        Files.writeString(file, """{"a": {"lastPlayedAt": 1, "fin""")

        val history = PackHistory(file)

        assertEquals(emptyMap(), history.records)
        assertTrue(Files.exists(dir.resolve("pack-history.json.corrupt")))
        play(history, "b", at = 5, StartGame)
        assertEquals(mapOf("b" to PackRecord(5)), PackHistory(file).records)
    }

    @Test
    fun `an unreadable file that cannot be moved aside is never overwritten`() {
        val broken = """{"a": {"lastPlayedAt": 1, "fin"""
        Files.writeString(file, broken)
        // A non-empty directory in the way makes the move fail, as a locked file would.
        Files.createDirectories(dir.resolve("pack-history.json.corrupt/x"))

        play(PackHistory(file), "b", at = 5, StartGame)

        assertEquals(broken, Files.readString(file))
    }

    /** Runs [events] on a fresh one-round game and feeds every transition to [history] as Pack [key]. */
    private fun play(history: PackHistory, key: String, at: Long, vararg events: GameEvent) {
        val question = Question(price = 100, type = Question.Type.Simple, contents = emptyList(), answer = Answer.Simple(listOf("a"), emptyList()))
        val engine = GameEngine(Package("p", "", emptyList(), "", listOf(Round("R", RoundType.Simple, listOf(Theme("T", listOf(question)))))))
        engine.addListener { history.record(key, it, at) }
        engine.process(PlayerJoined("Alice"))
        events.forEach { event -> engine.process(event).onLeft { error(it.message) } }
    }
}
