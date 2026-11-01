package app.state

import app.session.MediaSlot
import app.session.PlaybackView
import app.session.QuestionPlayback
import kotlinx.coroutines.test.TestScope
import com.sick.engine.GameEngine
import com.sick.event.AnswerShown
import com.sick.event.GameEvent
import com.sick.event.HostRejected
import com.sick.event.PauseTimer
import com.sick.event.PlayerBuzzed
import com.sick.event.PlayerJoined
import com.sick.event.PlayerSkipped
import com.sick.event.QuestionRevealed
import com.sick.event.QuestionSelected
import com.sick.event.SelectActivePlayer
import com.sick.event.SkipQuestion
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Package
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudienceViewsTest {

    @Test
    fun `the Shared Display goes from no pack to lobby to board while the host has no panel`() {
        assertEquals(Panel.NoPack, Table(pack = null).views().sharedDisplay.panel)

        val table = Table(simple())
        assertEquals(Panel.Lobby("p", JOIN_URL), table.views().sharedDisplay.panel)
        assertEquals(Panel.Empty, table.views().host.panel)

        table.start()
        assertIs<Panel.Board>(table.views().sharedDisplay.panel)
        assertEquals(Panel.Empty, table.views().host.panel)
    }

    @Test
    fun `only the host sees the correct option before the answer is shown`() {
        val question = question(answer = Answer.Select(listOf(Answer.Select.Option("A", "yes", correct = true))))
        val table = Table(question).start()

        table.go(QuestionSelected(question.id))
        assertEquals(Panel.Revealing("T", 100), table.views().sharedDisplay.panel)
        assertEquals(Panel.Revealing("T", 100), table.views().host.panel)

        table.go(QuestionRevealed)
        val views = table.views()
        assertTrue(assertIs<Panel.Question>(views.host.panel).revealCorrectOption)
        assertFalse(assertIs<Panel.Question>(views.sharedDisplay.panel).revealCorrectOption)
    }

    @Test
    fun `a Point Answer is marked on the first image for the host, and for the room once shown`() {
        val first = image("first")
        val point = Answer.Point(image = first, x = 0.5, y = 0.5, aspectRatio = 1.0, deviation = 0.1)
        val question = question(Content.Text("Where?"), first, image("second"), answer = point)
        val table = Table(question).start()
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)

        val playing = table.views()
        assertEquals(listOf(null, point, null), assertIs<Panel.Question>(playing.host.panel).items.map { it.point })
        assertTrue(assertIs<Panel.Question>(playing.sharedDisplay.panel).items.all { it.point == null })

        table.go(SkipQuestion)
        val shown = assertIs<Panel.Answer>(table.views().sharedDisplay.panel)
        assertEquals(listOf(ShownItem(QuestionDisplayItem.RemoteImage(URL("http://media.test/first.png")), point = point)), shown.items)
    }

    @Test
    fun `only the host sees the pack's wrong answers when the answer is shown`() {
        val question = question(Content.Text("Q"), answer = Answer.Simple(right = listOf("yes"), wrong = listOf("no")))
        val table = Table(question).start()
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)

        table.go(SkipQuestion)
        val views = table.views()
        assertEquals(listOf("no"), assertIs<Panel.Answer>(views.host.panel).wrongAnswers)
        assertEquals(emptyList(), assertIs<Panel.Answer>(views.sharedDisplay.panel).wrongAnswers)
    }

    @Test
    fun `a Chooser who fails keeps the Chooser mark alongside the Failed role`() {
        val question = simple()
        val table = Table(question, "Alice", "Bob").start()
        val (alice, bob) = table.players
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)
        table.go(PlayerBuzzed(alice))
        table.go(HostRejected)
        table.go(PlayerBuzzed(bob))

        val views = table.views()
        assertEquals(views.host.players, views.sharedDisplay.players)
        val (aliceView, bobView) = views.sharedDisplay.players
        assertEquals(QuestionRole.Failed, aliceView.role)
        assertTrue(aliceView.isChooser)
        assertEquals(QuestionRole.Answering, bobView.role)
        assertFalse(bobView.isChooser)
    }

    @Test
    fun `Host Pick offers only players the engine accepts, and none while paused`() {
        val question = simple()
        val table = Table(question, "Alice", "Bob", "Carol").start()
        val (alice, bob, carol) = table.players
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)
        table.go(PlayerBuzzed(alice))
        table.go(HostRejected)
        table.go(PlayerSkipped(bob))

        val live = assertIs<HostControls.Question>(table.views().host.controls)
        assertEquals(listOf(carol), live.picks.map { it.id })
        assertTrue(live.canPick)

        table.go(PauseTimer)
        val paused = table.views()
        assertFalse(assertIs<HostControls.Question>(paused.host.controls).canPick)
        assertEquals(TimerView(30, paused = true), assertIs<Panel.Question>(paused.sharedDisplay.panel).timer)
    }

    @Test
    fun `both windows show the answer timer while a player answers`() {
        val question = simple()
        val table = Table(question).start()
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)
        table.go(PlayerBuzzed(table.players.first()))

        val views = table.views()
        val expected = TimerView(20, paused = false)
        assertEquals(expected, assertIs<Panel.Question>(views.sharedDisplay.panel).timer)
        assertEquals(expected, assertIs<Panel.Question>(views.host.panel).timer)
        assertEquals(expected, assertIs<HostControls.Answering>(views.host.controls).timer)
    }

    @Test
    fun `completed themes are hidden on both boards unless the host shows them`() {
        val played = simple()
        val open = simple()
        val table = Table(Theme("A", listOf(played)), Theme("B", listOf(open))).start()
        table.go(QuestionSelected(played.id))
        table.go(SkipQuestion)
        table.go(AnswerShown)

        val hidden = table.views()
        val board = assertIs<Panel.Board>(hidden.sharedDisplay.panel).board
        assertEquals(listOf("B"), board.themes.map { it.name })
        assertTrue(board.hasCompletedThemes)
        assertEquals(board, hidden.host.board)

        val shown = table.views(showCompleted = true)
        assertEquals(listOf("A", "B"), assertIs<Panel.Board>(shown.sharedDisplay.panel).board.themes.map { it.name })
    }

    @Test
    fun `the playing media is attached to its item and only the Shared Display owns the sound`() {
        val question = question(Content.Text("Listen"), audio("a"), audio("b"))
        val table = Table(question).start()
        table.go(QuestionSelected(question.id))
        table.go(QuestionRevealed)
        val slot = MediaSlot(index = 1, generation = 3, paused = false)

        val views = table.views(table.playback.view.copy(media = slot))
        val sharedDisplay = assertIs<Panel.Question>(views.sharedDisplay.panel)
        val host = assertIs<Panel.Question>(views.host.panel)
        assertEquals(listOf(null, null, slot), sharedDisplay.items.map { it.media })
        assertEquals(listOf(null, null, slot), host.items.map { it.media })
        assertTrue(sharedDisplay.ownsSound)
        assertFalse(host.ownsSound)
    }

    @Test
    fun `the host can remove players between questions but not the last one once the game started`() {
        val question = simple()
        val alone = Table(question)
        assertTrue(alone.views().host.canRemovePlayers)
        alone.start()
        assertFalse(alone.views().host.canRemovePlayers)

        val pair = Table(question, "Alice", "Bob").start()
        assertTrue(pair.views().host.canRemovePlayers)
        pair.go(QuestionSelected(question.id))
        assertFalse(pair.views().host.canRemovePlayers)
    }

    /**
     * A started-or-not game over a one-round pack; the first player joined is made Chooser on [start].
     * A real [QuestionPlayback] resolves the content on screen; its timers never run, because
     * nothing advances its scheduler, so tests drive every event themselves.
     */
    private class Table(pack: Package?, vararg names: String) {
        constructor(question: Question<*>, vararg names: String) :
            this(pack(Theme("T", listOf(question))), *names.ifEmpty { arrayOf("Alice") })

        constructor(vararg themes: Theme) : this(pack(*themes), "Alice")

        val engine = GameEngine(pack ?: Package(name = "p", logo = "", tags = emptyList(), author = "", rounds = emptyList()))
        val playback = QuestionPlayback(engine, TestScope())

        init {
            engine.addListener(playback::onTransition)
            names.forEach { go(PlayerJoined(it)) }
        }

        val players get() = engine.state.players.map { it.id }

        fun go(event: GameEvent) {
            assertTrue(engine.process(event).isRight(), "$event rejected in ${engine.phase}")
        }

        fun start() = apply {
            go(StartGame)
            go(SelectActivePlayer(players.first()))
        }

        fun views(playback: PlaybackView = this.playback.view, showCompleted: Boolean = false) =
            audienceViews(engine.state, engine.phase, playback, joinUrl = JOIN_URL, showCompleted = showCompleted)
    }

    private companion object {
        const val JOIN_URL = "http://host.test:8080"

        fun pack(vararg themes: Theme) = Package(
            name = "p",
            logo = "",
            tags = emptyList(),
            author = "",
            rounds = listOf(Round("R", RoundType.Simple, themes.toList())),
        )

        fun simple() = question(Content.Text("Q"))

        fun question(
            vararg contents: Content,
            answer: Answer = Answer.Simple(right = listOf("a"), wrong = emptyList()),
        ) = Question(price = 100, type = Question.Type.Simple, contents = contents.toList(), answer = answer)

        fun image(name: String) = Content.Media(Content.Type.Image, Content.Media.Source.Remote(URL("http://media.test/$name.png")))
        fun audio(name: String) = Content.Media(Content.Type.Audio, Content.Media.Source.Remote(URL("http://media.test/$name.mp3")))
    }
}
