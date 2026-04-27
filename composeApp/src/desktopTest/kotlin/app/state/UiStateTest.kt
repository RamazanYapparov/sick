package app.state

import com.sick.engine.GameEngine
import com.sick.event.PlayerJoined
import com.sick.event.QuestionSelected
import com.sick.event.SelectActivePlayer
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import com.sick.state.GamePhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiStateTest {

    @Test
    fun `empty engine yields empty UI state with no pack`() {
        val engine = GameEngine(emptyPack())
        val ui = DesktopUiState.initial(8080).withEngineSnapshot(
            engine = engine,
            loadedPackPath = null,
            extractedBasePath = null,
            serverUrl = "http://1.2.3.4:8080",
        )

        assertEquals("No pack loaded", ui.packName)
        assertEquals(0, ui.currentRoundIndex)
        assertEquals(0, ui.totalRounds)
        assertTrue(ui.boardThemes.isEmpty())
        assertEquals(false, ui.hasPack)
        assertEquals(GamePhase.Lobby, ui.phase)
        assertEquals("http://1.2.3.4:8080", ui.serverUrl)
    }

    @Test
    fun `loaded pack populates board with played markers`() {
        val q1 = simple("Q1", 100)
        val q2 = simple("Q2", 200)
        val pack = pack(round("R", theme("Theme A", q1, q2)))

        val engine = GameEngine(pack)
        engine.process(PlayerJoined("Alice"))
        engine.process(StartGame)
        val alice = engine.state.players.first()
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(q1.id))

        val ui = DesktopUiState.initial(8080).withEngineSnapshot(
            engine = engine,
            loadedPackPath = "/tmp/pack.siq",
            extractedBasePath = null,
            serverUrl = "http://1.2.3.4:8080",
        )

        assertEquals("p", ui.packName)
        assertEquals(true, ui.hasPack)
        assertEquals(1, ui.currentRoundIndex)
        assertEquals(1, ui.totalRounds)
        assertEquals("R", ui.roundName)
        assertEquals("/tmp/pack.siq", ui.loadedPackPath)

        assertEquals(1, ui.boardThemes.size)
        val theme = ui.boardThemes[0]
        assertEquals("Theme A", theme.name)
        assertEquals(2, theme.questions.size)

        val playedQuestion = theme.questions.first { it.id == q1.id }
        val unplayedQuestion = theme.questions.first { it.id == q2.id }
        assertTrue(playedQuestion.played)
        assertEquals(false, unplayedQuestion.played)

        assertNotNull(ui.currentQuestion)
        assertEquals(q1.id, ui.currentQuestion!!.id)
        assertEquals("Theme A", ui.currentThemeName)
    }

    @Test
    fun `withEngineSnapshot preserves transient and window fields from receiver`() {
        val engine = GameEngine(emptyPack())
        val seed = DesktopUiState.initial(8080).copy(
            errorMessage = "boom",
            infoMessage = "hi",
            displayWindowVisible = false,
        )

        val ui = seed.withEngineSnapshot(
            engine = engine,
            loadedPackPath = null,
            extractedBasePath = null,
            serverUrl = "http://localhost:8080",
        )

        assertEquals("boom", ui.errorMessage)
        assertEquals("hi", ui.infoMessage)
        assertEquals(false, ui.displayWindowVisible)
    }

    @Test
    fun `currentThemeName is null when no question is selected`() {
        val pack = pack(round("R", theme("T", simple("Q", 100))))
        val engine = GameEngine(pack)

        val ui = DesktopUiState.initial(8080).withEngineSnapshot(
            engine = engine,
            loadedPackPath = null,
            extractedBasePath = null,
            serverUrl = "http://localhost:8080",
        )

        assertNull(ui.currentQuestion)
        assertNull(ui.currentThemeName)
    }

    private fun emptyPack() = Package(
        name = "Game",
        logo = "",
        tags = emptyList(),
        author = "",
        rounds = emptyList(),
    )

    private fun pack(vararg rounds: Round) = Package(
        name = "p",
        logo = "",
        tags = emptyList(),
        author = "",
        rounds = rounds.toList(),
    )

    private fun round(name: String, vararg themes: Theme) =
        Round(name = name, type = RoundType.Simple, themes = themes.toList())

    private fun theme(name: String, vararg questions: Question<*>) =
        Theme(name = name, questions = questions.toList())

    private fun simple(@Suppress("UNUSED_PARAMETER") label: String, price: Int) = Question(
        price = price,
        type = Question.Type.Simple,
        contents = emptyList(),
        answer = Answer.Simple(right = listOf("a"), wrong = emptyList()),
    )
}
