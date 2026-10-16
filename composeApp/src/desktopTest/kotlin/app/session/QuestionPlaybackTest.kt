package app.session

import app.state.QuestionDisplayItem
import com.sick.engine.GameEngine
import com.sick.event.AdjustPlayerScore
import com.sick.event.HostAccepted
import com.sick.event.HostRejected
import com.sick.event.PauseTimer
import com.sick.event.PlayerBuzzed
import com.sick.event.PlayerJoined
import com.sick.event.QuestionSelected
import com.sick.event.ResumeTimer
import com.sick.event.SelectActivePlayer
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import com.sick.state.GamePhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionPlaybackTest {

    @Test
    fun `question media holds the timer until it ends`() = runTest {
        val game = Game(this, question(audio("a")))
        game.reveal()
        val slot = assertNotNull(game.playback.view.media)
        assertEquals(0, slot.index)

        advanceSeconds(60)
        assertEquals(30, game.engine.state.timerRemaining)

        game.playback.onMediaEnded(slot.generation, 0)
        advanceSeconds(2)
        assertEquals(28, game.engine.state.timerRemaining)
    }

    @Test
    fun `a missing media file shows as text and does not hold the timer`() = runTest {
        val emptyPack = Files.createTempDirectory("pack")
        val game = Game(this, question(Content.Media.FileRef(Content.Type.Audio, "gone.mp3")), basePath = emptyPack)
        game.reveal()

        assertEquals(listOf(QuestionDisplayItem.Text("Audio not found: gone.mp3")), game.playback.view.items)
        assertNull(game.playback.view.media)
        advanceSeconds(4)
        assertEquals(29, game.engine.state.timerRemaining)
    }

    @Test
    fun `media play one after another and the hold ends after the last`() = runTest {
        val game = Game(this, question(audio("a"), video("b")))
        game.reveal()
        val generation = game.playback.view.media!!.generation

        game.playback.onMediaEnded(generation, 0)
        game.playback.onMediaEnded(generation, 0) // the other window reports the same end
        assertEquals(1, game.playback.view.media?.index)
        advanceSeconds(10)
        assertEquals(30, game.engine.state.timerRemaining)

        game.playback.onMediaEnded(generation, 1)
        advanceSeconds(1)
        assertEquals(29, game.engine.state.timerRemaining)
    }

    @Test
    fun `replay restarts the queue while the timer runs and its end starts nothing`() = runTest {
        val game = Game(this, question(audio("a"), audio("b")))
        game.reveal()
        val first = game.playback.view.media!!.generation
        game.playback.onMediaEnded(first, 0)
        game.playback.onMediaEnded(first, 1)
        assertTrue(game.playback.view.canReplayMedia)

        game.playback.replayMedia()
        val replay = assertNotNull(game.playback.view.media)
        assertEquals(0, replay.index)
        game.playback.onMediaEnded(first, 0) // late duplicate from the first pass
        assertEquals(0, game.playback.view.media?.index)

        advanceSeconds(5)
        game.playback.onMediaEnded(replay.generation, 0)
        game.playback.onMediaEnded(replay.generation, 1)
        advanceSeconds(5)
        assertEquals(20, game.engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, game.engine.phase)
    }

    @Test
    fun `skip media ends the hold and starts the timer`() = runTest {
        val game = Game(this, question(audio("a"), audio("b")))
        game.reveal()
        assertTrue(game.playback.view.canSkipMedia)

        game.playback.skipMedia()
        assertNull(game.playback.view.media)
        advanceSeconds(1)
        assertEquals(29, game.engine.state.timerRemaining)
    }

    @Test
    fun `a buzz pauses the media and a wrong answer resumes it with the hold kept`() = runTest {
        val game = Game(this, question(audio("a")))
        game.reveal()

        game.engine.process(PlayerBuzzed(game.players[0]))
        assertEquals(true, game.playback.view.media?.paused)

        game.engine.process(HostRejected)
        assertEquals(GamePhase.ShowingQuestion, game.engine.phase)
        assertEquals(false, game.playback.view.media?.paused)
        advanceSeconds(10)
        assertEquals(30, game.engine.state.timerRemaining)
    }

    @Test
    fun `host pause stops the answer timer and resume continues it`() = runTest {
        val game = Game(this, question())
        game.reveal()
        game.engine.process(PlayerBuzzed(game.players[0]))
        advanceSeconds(5)
        assertEquals(15, game.engine.state.answerTimerRemaining)

        game.engine.process(PauseTimer)
        advanceSeconds(10)
        assertEquals(15, game.engine.state.answerTimerRemaining)

        game.engine.process(ResumeTimer)
        advanceSeconds(2)
        assertEquals(13, game.engine.state.answerTimerRemaining)
    }

    @Test
    fun `a score adjustment during the reveal does not delay it`() = runTest {
        val game = Game(this, question())
        game.select()
        advanceTimeBy(2_000)
        runCurrent()
        game.engine.process(AdjustPlayerScore(game.players[0], 100))

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(GamePhase.ShowingQuestion, game.engine.phase)
    }

    @Test
    fun `answer media replaces the question's content and ends the answer, an answer without media ends after 4 s`() = runTest {
        val withMedia = Game(this, question(answerContents = listOf(video("v"))))
        withMedia.reveal()
        withMedia.engine.process(PlayerBuzzed(withMedia.players[0]))
        withMedia.engine.process(HostAccepted())
        assertEquals(listOf(QuestionDisplayItem.RemoteVideo(URL("http://media.test/v.mp4"))), withMedia.playback.view.items)
        val slot = assertNotNull(withMedia.playback.view.media)
        advanceSeconds(30)
        assertEquals(GamePhase.ShowingAnswer, withMedia.engine.phase)
        withMedia.playback.onMediaEnded(slot.generation, slot.index)
        assertEquals(GamePhase.ChoosingQuestion, withMedia.engine.phase)
        assertTrue(withMedia.playback.view.items.isEmpty())

        val plain = Game(this, question())
        plain.reveal()
        plain.engine.process(PlayerBuzzed(plain.players[0]))
        plain.engine.process(HostAccepted())
        advanceSeconds(4)
        assertEquals(GamePhase.ChoosingQuestion, plain.engine.phase)
    }

    @Test
    fun `a closed playback stops its timers and ignores reports`() = runTest {
        val game = Game(this, question(audio("a")))
        game.reveal()
        val generation = game.playback.view.media!!.generation

        game.playback.close()
        game.playback.onMediaEnded(generation, 0)
        advanceSeconds(10)
        assertEquals(30, game.engine.state.timerRemaining)
        assertFalse(game.playback.view.canReplayMedia)
    }

    private fun TestScope.advanceSeconds(seconds: Int) {
        advanceTimeBy(seconds * 1_000L)
        runCurrent()
    }

    /** Two players in a one-theme pack whose first question is [question]; playback runs on [test]'s virtual time. */
    private class Game(private val test: TestScope, question: Question<*>, basePath: Path? = null) {
        val engine = GameEngine(
            Package(
                name = "p",
                logo = "",
                tags = emptyList(),
                author = "",
                rounds = listOf(
                    Round("R", RoundType.Simple, listOf(Theme("T", listOf(question, question.copy(id = java.util.UUID.randomUUID()))))),
                ),
            ),
        )
        val playback = QuestionPlayback(engine, test.backgroundScope, basePath)
        private val questionId = question.id

        init {
            engine.addListener(playback::onTransition)
            engine.process(PlayerJoined("Alice"))
            engine.process(PlayerJoined("Bob"))
            engine.process(StartGame)
            engine.process(SelectActivePlayer(engine.state.players[0].id))
        }

        val players get() = engine.state.players.map { it.id }

        fun select() {
            engine.process(QuestionSelected(questionId))
        }

        /** Selects the question and lets the 3 s reveal pass. */
        fun reveal() {
            select()
            test.advanceTimeBy(3_000)
            test.runCurrent()
        }
    }

    private fun question(vararg contents: Content, answerContents: List<Content> = emptyList()) = Question(
        price = 100,
        type = Question.Type.Simple,
        contents = contents.toList(),
        answer = Answer.Simple(right = listOf("a"), wrong = emptyList(), contents = answerContents),
    )

    private fun audio(name: String) = Content.Media.FileUrl(Content.Type.Audio, URL("http://media.test/$name.mp3"))
    private fun video(name: String) = Content.Media.FileUrl(Content.Type.Video, URL("http://media.test/$name.mp4"))
}
