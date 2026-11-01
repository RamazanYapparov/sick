package app.state

import app.session.MediaSlot
import app.session.PlaybackView
import com.sick.model.GameState
import com.sick.model.Question
import com.sick.model.canBuzz
import com.sick.service.lowestScoreCandidates
import com.sick.state.GamePhase
import java.util.UUID
import com.sick.model.Answer as PackAnswer

/** What each audience sees, decided once per published state; the windows only lay it out. */
data class AudienceViews(val host: HostView, val sharedDisplay: SharedDisplayView)

/** What the Shared Display shows the room: board, question and scores, never the answer before it is shown. */
data class SharedDisplayView(
    /** "Round 2 / 3", or "Lobby" when there is no round. */
    val header: String,
    val panel: Panel,
    val players: List<PlayerView>,
)

data class HostView(
    val packName: String,
    val phaseName: String,
    /** The clickable board; set only while the Chooser picks a question. */
    val board: BoardView?,
    /** The answer in words, or null when no question is in play. */
    val answerSummary: List<String>?,
    val controls: HostControls,
    val panelTitle: String,
    val panel: Panel,
    val players: List<PlayerView>,
    /** Mirrors GameEngine: anyone in the Lobby; once started, only between questions and never the last player. */
    val canRemovePlayers: Boolean,
)

/** The main area of a window. Both windows render it the same way; the builder decides what each one gets. */
sealed interface Panel {
    /** Display before a pack is loaded. */
    data object NoPack : Panel

    data class Lobby(val packName: String, val joinUrl: String) : Panel

    data class Board(val board: BoardView) : Panel

    /** Holds the content back during the reveal so both windows start the media together. */
    data class Revealing(val theme: String, val price: Int) : Panel

    data class Question(
        val theme: String,
        val price: Int,
        val items: List<ShownItem>,
        val options: List<PackAnswer.Select.Option>?,
        /** Marks the correct option; only the host sees it before the answer is shown. */
        val revealCorrectOption: Boolean,
        val timer: TimerView?,
        /** Only the Shared Display makes sound: the host shows video muted and audio as progress. */
        val ownsSound: Boolean,
        /** Grow the text to fill the panel instead of using the fixed body size. */
        val fitsText: Boolean,
    ) : Panel

    data class Answer(
        val answer: PackAnswer,
        /** Simple: the answer's own content. Point: the question image with the spot, or none if it has no image. */
        val items: List<ShownItem>,
        /** The pack's wrong answers; only the host sees them, so the room never takes them for accepted ones. */
        val wrongAnswers: List<String>,
        val ownsSound: Boolean,
    ) : Panel

    /** Host with no question in play. */
    data object Empty : Panel
}

/** A question or answer item as a window shows it. */
data class ShownItem(
    val item: QuestionDisplayItem,
    /** Set when this item is the media playing right now; other playable items are not shown. */
    val media: MediaSlot? = null,
    /** Point Answer to mark on this image. */
    val point: PackAnswer.Point? = null,
)

/** The running timer: the question timer in ShowingQuestion, the answer timer in PlayerAnswering. */
data class TimerView(val seconds: Int, val paused: Boolean)

/** A player's part in the current question; independent of being the Chooser. */
enum class QuestionRole { Answering, Failed, Skipped }

data class PlayerView(
    val id: UUID,
    val name: String,
    val score: Int,
    val role: QuestionRole?,
    val isChooser: Boolean,
    /** Reaction Time of this player's Buzz in the current Buzz Window, Late Buzz included. */
    val reactionMillis: Long?,
)

data class BoardView(
    /** Themes to show: those with an unplayed question, or all of them when [showCompleted]. */
    val themes: List<BoardThemeState>,
    val hasCompletedThemes: Boolean,
    val showCompleted: Boolean,
)

data class BoardThemeState(
    val name: String,
    val questions: List<BoardQuestionState>,
)

data class BoardQuestionState(
    val id: UUID,
    val price: Int,
    val played: Boolean,
)

/** The host's controls for the current phase, with availability already decided. */
sealed interface HostControls {
    data class Lobby(val canStart: Boolean) : HostControls

    /** Players the host may name Chooser: those with the lowest score. */
    data class ChoosingPlayer(val candidates: List<PlayerChoice>) : HostControls

    data class ChoosingQuestion(val chooserName: String?) : HostControls

    data object Revealing : HostControls

    data class Question(
        val timer: TimerView,
        val canSkipMedia: Boolean,
        val canReplayMedia: Boolean,
        /** Players the engine would accept as a Host Pick. */
        val picks: List<PlayerChoice>,
        val canPick: Boolean,
    ) : HostControls

    data class Answering(val playerName: String, val timer: TimerView) : HostControls

    data object ShowingAnswer : HostControls

    data object RoundEnd : HostControls

    data object GameOver : HostControls
}

data class PlayerChoice(val id: UUID, val name: String, val highlighted: Boolean = false)

/**
 * Decides what the host and the Shared Display show for [state] in [phase].
 * The question's and answer's content comes in [playback]; a Point Answer
 * carries its image. Nothing here touches the disk, so it is cheap to call on
 * every published state.
 */
fun audienceViews(
    state: GameState,
    phase: GamePhase,
    playback: PlaybackView,
    joinUrl: String,
    showCompleted: Boolean,
): AudienceViews {
    val hasPack = state.pack.rounds.isNotEmpty()
    val packName = if (hasPack) state.pack.name else "No pack loaded"
    val players = playerViews(state)
    val board = boardView(state, showCompleted)
    val question = state.currentQuestion

    fun panelFor(host: Boolean): Panel? = question?.let {
        questionPanel(state, phase, it, playback, host)
    }

    val hostPanel = panelFor(host = true) ?: Panel.Empty
    val sharedDisplayPanel = panelFor(host = false) ?: when {
        !hasPack -> Panel.NoPack
        phase == GamePhase.Lobby -> Panel.Lobby(packName, joinUrl)
        else -> Panel.Board(board)
    }

    return AudienceViews(
        host = HostView(
            packName = packName,
            phaseName = phase.name,
            board = board.takeIf { phase == GamePhase.ChoosingQuestion },
            answerSummary = question?.answer?.hostSummary(),
            controls = hostControls(state, phase, playback, hasPack),
            panelTitle = if (hostPanel is Panel.Answer) "Answer" else "Current Question",
            panel = hostPanel,
            players = players,
            canRemovePlayers = phase == GamePhase.Lobby || (phase in BETWEEN_QUESTIONS && state.players.size > 1),
        ),
        sharedDisplay = SharedDisplayView(
            header = state.currentRound?.let { "Round ${state.currentRoundIndex + 1} / ${state.pack.rounds.size}" } ?: "Lobby",
            panel = sharedDisplayPanel,
            players = players,
        ),
    )
}

private val BETWEEN_QUESTIONS = setOf(
    GamePhase.ChoosingPlayer,
    GamePhase.ChoosingQuestion,
    GamePhase.RoundEnd,
    GamePhase.GameOver,
)

private fun questionPanel(
    state: GameState,
    phase: GamePhase,
    question: Question<*>,
    playback: PlaybackView,
    host: Boolean,
): Panel {
    val theme = state.currentRound?.themes
        ?.firstOrNull { theme -> theme.questions.any { it.id == question.id } }
        ?.name ?: "Question"
    val items = playback.items
    return when (phase) {
        GamePhase.RevealingQuestion -> Panel.Revealing(theme, question.price)
        GamePhase.ShowingAnswer -> Panel.Answer(
            answer = question.answer,
            items = when (val answer = question.answer) {
                is PackAnswer.Simple -> items.shown(playback.media)
                is PackAnswer.Select -> emptyList()
                is PackAnswer.Point -> listOf(ShownItem(answer.image.toDisplayItem(), point = answer))
            },
            wrongAnswers = (question.answer as? PackAnswer.Simple)?.wrong?.takeIf { host }.orEmpty(),
            ownsSound = !host,
        )
        else -> {
            // The host sees the answer while the question plays; the room only once it is shown.
            val point = (question.answer as? PackAnswer.Point)?.takeIf { host }
            Panel.Question(
                theme = theme,
                price = question.price,
                items = items.shown(playback.media, point, pointAt = if (point != null) items.indexOfFirst { it.isImage() } else -1),
                options = (question.answer as? PackAnswer.Select)?.options,
                revealCorrectOption = host,
                timer = timerView(state, phase),
                ownsSound = !host,
                fitsText = !host,
            )
        }
    }
}

/** Attaches [media] to the playable item it plays, counted in play order, and [point] to the item at [pointAt]. */
private fun List<QuestionDisplayItem>.shown(
    media: MediaSlot?,
    point: PackAnswer.Point? = null,
    pointAt: Int = -1,
): List<ShownItem> {
    var mediaIndex = 0
    return mapIndexed { index, item ->
        val slot = if (item.isPlayable()) {
            val playOrder = mediaIndex++
            media?.takeIf { it.index == playOrder }
        } else {
            null
        }
        ShownItem(item, slot, point.takeIf { index == pointAt })
    }
}

private fun timerView(state: GameState, phase: GamePhase): TimerView? = when (phase) {
    GamePhase.ShowingQuestion -> questionTimer(state)
    GamePhase.PlayerAnswering -> answerTimer(state)
    else -> null
}

private fun questionTimer(state: GameState) = TimerView(state.timerRemaining, state.isTimerPaused)

private fun answerTimer(state: GameState) = TimerView(state.answerTimerRemaining, state.isTimerPaused)

private fun playerViews(state: GameState): List<PlayerView> {
    val reactions = state.buzzes.associate { it.playerId to it.reactionMillis }
    return state.players.map { player ->
        PlayerView(
            id = player.id,
            name = player.name,
            score = player.score,
            role = when (player.id) {
                state.answeringPlayerId -> QuestionRole.Answering
                in state.failedBuzzPlayerIds -> QuestionRole.Failed
                in state.skipVotePlayerIds -> QuestionRole.Skipped
                else -> null
            },
            isChooser = player.id == state.activePlayerId,
            reactionMillis = reactions[player.id],
        )
    }
}

private fun boardView(state: GameState, showCompleted: Boolean): BoardView {
    val themes = state.currentRound?.themes.orEmpty().map { theme ->
        BoardThemeState(
            name = theme.name,
            questions = theme.questions.map { question ->
                BoardQuestionState(question.id, question.price, played = question.id in state.playedQuestionIds)
            },
        )
    }
    val (completed, open) = themes.partition { theme -> theme.questions.all { it.played } }
    return BoardView(
        themes = if (showCompleted) themes else open,
        hasCompletedThemes = completed.isNotEmpty(),
        showCompleted = showCompleted,
    )
}

private fun hostControls(state: GameState, phase: GamePhase, playback: PlaybackView, hasPack: Boolean): HostControls {
    fun nameOf(id: UUID?) = id?.let(state::findPlayer)?.name
    return when (phase) {
        GamePhase.Lobby -> HostControls.Lobby(canStart = hasPack && state.players.isNotEmpty())
        GamePhase.ChoosingPlayer -> HostControls.ChoosingPlayer(
            state.lowestScoreCandidates().map { PlayerChoice(it.id, it.name, highlighted = it.id == state.activePlayerId) },
        )
        GamePhase.ChoosingQuestion -> HostControls.ChoosingQuestion(nameOf(state.activePlayerId))
        GamePhase.RevealingQuestion -> HostControls.Revealing
        GamePhase.ShowingQuestion -> HostControls.Question(
            timer = questionTimer(state),
            canSkipMedia = playback.canSkipMedia,
            canReplayMedia = playback.canReplayMedia,
            picks = state.players.filter { state.canBuzz(it.id) }.map { PlayerChoice(it.id, it.name) },
            canPick = !state.isTimerPaused,
        )
        GamePhase.PlayerAnswering -> HostControls.Answering(
            playerName = nameOf(state.answeringPlayerId) ?: "Unknown",
            timer = answerTimer(state),
        )
        GamePhase.ShowingAnswer -> HostControls.ShowingAnswer
        GamePhase.RoundEnd -> HostControls.RoundEnd
        GamePhase.GameOver -> HostControls.GameOver
    }
}
