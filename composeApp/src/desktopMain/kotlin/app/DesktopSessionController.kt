package app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sick.com.sick.siq.reader.SiqExtractor
import com.sick.com.sick.siq.reader.SiqReader
import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.event.AdjustPlayerScore
import com.sick.event.GameEvent
import com.sick.event.HostAccepted
import com.sick.event.HostRejected
import com.sick.event.NextRound
import com.sick.event.PlayerBuzzed
import com.sick.event.PlayerJoined
import com.sick.event.PlayerLeft
import com.sick.event.PlayerRenamed
import com.sick.event.QuestionSelected
import com.sick.event.SelectActivePlayer
import com.sick.event.SkipQuestion
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Package
import com.sick.model.Player
import com.sick.model.Question
import com.sick.model.Round
import com.sick.model.RoundType
import com.sick.model.Theme
import com.sick.server.GameServer
import com.sick.state.GamePhase
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopSessionController(
    private val scope: CoroutineScope,
    private val port: Int = 8080,
) {
    private val reader = SiqReader()

    private var loadedPack: Package? = null
    private var loadedPackPath: String? = null

    private var engine: GameEngine = createEngine(emptyPack())
    private var timer: GameTimer = GameTimer(engine, scope)
    private var server: GameServer = GameServer(engine, port)

    var uiState by mutableStateOf(DesktopUiState.initial(port))
        private set

    init {
        bindEngine(engine)
        server.start()
        publishState()
    }

    fun dispose() {
        timer.stop()
        server.stop()
    }

    fun showDisplayWindow() {
        uiState = uiState.copy(displayWindowVisible = true)
    }

    fun hideDisplayWindow() {
        uiState = uiState.copy(displayWindowVisible = false)
    }

    fun loadPackFromDialog() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Load SIQ pack"
            fileFilter = FileNameExtensionFilter("SIQ packages", "siq")
            isAcceptAllFileFilterUsed = true
        }

        val result = chooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) {
            return
        }

        loadPack(chooser.selectedFile.toPath())
    }

    fun resetGame() {
        replaceSession(loadedPack ?: emptyPack())
        setInfo(
            if (loadedPack != null) "Game reset with current pack."
            else "Empty game created. Load a pack to start playing."
        )
    }

    fun addPlayer(name: String) = process(PlayerJoined(name.trim()))

    fun removePlayer(playerId: UUID) = process(PlayerLeft(playerId))

    fun renamePlayer(playerId: UUID, newName: String) = process(PlayerRenamed(playerId, newName.trim()))

    fun startGame() = process(StartGame)

    fun selectActivePlayer(playerId: UUID) = process(SelectActivePlayer(playerId))

    fun selectQuestion(questionId: UUID) = process(QuestionSelected(questionId))

    fun chooseAnsweringPlayer(playerId: UUID) = process(PlayerBuzzed(playerId))

    fun markAnswerCorrect() = process(HostAccepted)

    fun markAnswerWrong() = process(HostRejected)

    fun skipQuestion() = process(SkipQuestion)

    fun nextRound() = process(NextRound)

    fun adjustScore(playerId: UUID, delta: Int) = process(AdjustPlayerScore(playerId, delta))

    private fun process(event: GameEvent) {
        val previousPhase = engine.phase
        engine.process(event).fold(
            ifLeft = { error ->
                setError(error.message)
            },
            ifRight = {
                handlePhaseChange(previousPhase, engine.phase)
                clearMessages()
                publishState()
            },
        )
    }

    private fun handlePhaseChange(previousPhase: GamePhase, newPhase: GamePhase) {
        when (newPhase) {
            GamePhase.ShowingQuestion -> {
                if (previousPhase != GamePhase.ShowingQuestion && engine.state.timerRemaining > 0) {
                    timer.start(engine.state.timerRemaining)
                }
            }
            else -> timer.stop()
        }
    }

    private fun loadPack(path: Path) {
        runCatching {
            val extracted = SiqExtractor(
                source = path.toString(),
                destination = System.getProperty("java.io.tmpdir"),
            ).extract()
            val rawPack = reader.read(extracted)
            sanitizePack(rawPack)
        }.onSuccess { pack ->
            loadedPack = pack
            loadedPackPath = path.toString()
            replaceSession(pack)
            setInfo("Loaded pack: ${pack.name}")
        }.onFailure { error ->
            error.printStackTrace()
            setError(error.message ?: "Failed to load pack")
        }
    }

    private fun replaceSession(pack: Package) {
        timer.stop()
        server.stop()

        engine = createEngine(pack)
        timer = GameTimer(engine, scope)
        server = GameServer(engine, port)
        bindEngine(engine)
        server.start()
        publishState()
    }

    private fun bindEngine(target: GameEngine) {
        target.addListener { _, _ ->
            publishState()
        }
    }

    private fun publishState() {
        val state = engine.state
        val currentQuestion = state.currentQuestion

        uiState = uiState.copy(
            packName = if (state.pack.rounds.isEmpty()) "No pack loaded" else state.pack.name,
            loadedPackPath = loadedPackPath,
            phase = engine.phase,
            players = state.players,
            activePlayerId = state.activePlayerId,
            answeringPlayerId = state.answeringPlayerId,
            roundName = state.currentRound?.name,
            currentRoundIndex = if (state.pack.rounds.isEmpty()) 0 else state.currentRoundIndex + 1,
            totalRounds = state.pack.rounds.size,
            currentQuestion = currentQuestion,
            currentThemeName = currentQuestion?.let { findThemeName(state.currentRound, it.id) },
            boardThemes = state.currentRound?.themes.orEmpty().map { theme ->
                BoardThemeState(
                    name = theme.name,
                    questions = theme.questions.map { question ->
                        BoardQuestionState(
                            id = question.id,
                            price = question.price,
                            played = question.id in state.playedQuestionIds,
                        )
                    },
                )
            },
            timerRemaining = state.timerRemaining,
            serverUrl = "http://localhost:$port",
            hasPack = state.pack.rounds.isNotEmpty(),
        )
    }

    private fun findThemeName(round: Round?, questionId: UUID): String? =
        round?.themes?.firstOrNull { theme -> theme.questions.any { it.id == questionId } }?.name

    private fun setError(message: String) {
        uiState = uiState.copy(errorMessage = message, infoMessage = null)
    }

    private fun setInfo(message: String) {
        uiState = uiState.copy(infoMessage = message, errorMessage = null)
    }

    private fun clearMessages() {
        uiState = uiState.copy(errorMessage = null, infoMessage = null)
    }

    private fun createEngine(pack: Package): GameEngine = GameEngine(pack)

    private fun emptyPack() = Package(
        name = "Game",
        logo = "",
        tags = emptyList(),
        author = "",
        rounds = emptyList(),
    )

    private fun sanitizePack(pack: Package): Package {
        val rounds = pack.rounds
            .filter { it.type == RoundType.Simple }
            .mapNotNull { round ->
                val themes = round.themes.mapNotNull { theme ->
                    val questions = theme.questions.filter { it.type is Question.Type.Simple }
                    if (questions.isEmpty()) null else theme.copy(questions = questions)
                }

                if (themes.isEmpty()) null else round.copy(themes = themes)
            }

        require(rounds.isNotEmpty()) { "Pack does not contain supported regular rounds." }
        return pack.copy(rounds = rounds)
    }
}

data class DesktopUiState(
    val packName: String,
    val loadedPackPath: String?,
    val phase: GamePhase,
    val players: List<Player>,
    val activePlayerId: UUID?,
    val answeringPlayerId: UUID?,
    val roundName: String?,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val currentQuestion: Question<*>?,
    val currentThemeName: String?,
    val boardThemes: List<BoardThemeState>,
    val timerRemaining: Int,
    val errorMessage: String?,
    val infoMessage: String?,
    val serverUrl: String,
    val displayWindowVisible: Boolean,
    val hasPack: Boolean,
) {
    companion object {
        fun initial(port: Int) = DesktopUiState(
            packName = "No pack loaded",
            loadedPackPath = null,
            phase = GamePhase.Lobby,
            players = emptyList(),
            activePlayerId = null,
            answeringPlayerId = null,
            roundName = null,
            currentRoundIndex = 0,
            totalRounds = 0,
            currentQuestion = null,
            currentThemeName = null,
            boardThemes = emptyList(),
            timerRemaining = 0,
            errorMessage = null,
            infoMessage = null,
            serverUrl = "http://localhost:$port",
            displayWindowVisible = true,
            hasPack = false,
        )
    }
}

data class BoardThemeState(
    val name: String,
    val questions: List<BoardQuestionState>,
)

data class BoardQuestionState(
    val id: UUID,
    val price: Int,
    val played: Boolean,
)

fun Question<*>.displayLines(): List<String> =
    contents.map { content ->
        when (content) {
            is Content.Text -> content.text
            is Content.Media.FileRef -> "${content.type.name}: ${content.ref}"
            is Content.Media.FileUrl -> "${content.type.name}: ${content.url}"
        }
    }

fun Answer.hostSummary(): List<String> = when (this) {
    is Answer.Simple -> buildList {
        if (right.isNotEmpty()) add("Right: ${right.joinToString(", ")}")
        if (wrong.isNotEmpty()) add("Wrong: ${wrong.joinToString(", ")}")
    }
    is Answer.Select -> options.map { option ->
        "${if (option.correct) "[x]" else "[ ]"} ${option.name}: ${option.answer}"
    }
}
