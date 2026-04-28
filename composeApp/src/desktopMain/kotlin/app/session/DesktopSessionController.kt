package app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.state.DesktopUiState
import app.state.withEngineSnapshot
import com.sick.engine.GameEngine
import com.sick.engine.GameTimer
import com.sick.event.AdjustPlayerScore
import com.sick.event.AnswerShown
import com.sick.event.GameEvent
import com.sick.event.HostAccepted
import com.sick.event.HostRejected
import com.sick.event.NextRound
import com.sick.event.PauseTimer
import com.sick.event.PlayerBuzzed
import com.sick.event.QuestionRevealed
import com.sick.event.QuestionSelected
import com.sick.event.ResumeTimer
import com.sick.event.SelectActivePlayer
import com.sick.event.SkipQuestion
import com.sick.event.StartGame
import com.sick.model.Package
import com.sick.server.GameServer
import com.sick.state.GamePhase
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import java.util.UUID

class DesktopSessionController(
    private val scope: CoroutineScope,
    private val port: Int = 8080,
) {
    private val packLoader = PackLoader()

    private var loadedPack: Package? = null
    private var loadedPackPath: String? = null
    private var extractedBasePath: Path? = null

    private var engine: GameEngine = createEngine(emptyPack())
    private var timer: GameTimer = GameTimer(engine, scope)
    private var server: GameServer = GameServer(engine, port, buzzAllowed = { !engine.state.isTimerPaused })
    private var timerOrchestrator: TimerOrchestrator = TimerOrchestrator(timer, engine, scope, ::showAnswer, ::revealQuestion)

    private var mediaStopSignal = 0

    var uiState by mutableStateOf(DesktopUiState.initial(port))
        private set

    init {
        bindEngine(engine)
        server.start()
        publishState()
    }

    fun dispose() {
        timerOrchestrator.stop()
        server.stop()
    }

    fun showDisplayWindow() {
        uiState = uiState.copy(displayWindowVisible = true)
    }

    fun hideDisplayWindow() {
        uiState = uiState.copy(displayWindowVisible = false)
    }

    fun loadPackFromDialog() {
        val path = pickSiqFile() ?: return
        loadPack(path)
    }

    fun resetGame() {
        replaceSession(loadedPack ?: emptyPack())
        setInfo(
            if (loadedPack != null) "Game reset with current pack."
            else "Empty game created. Load a pack to start playing."
        )
    }

    fun startGame() = process(StartGame)

    fun selectActivePlayer(playerId: UUID) = process(SelectActivePlayer(playerId))

    fun selectQuestion(questionId: UUID) = process(QuestionSelected(questionId))

    fun chooseAnsweringPlayer(playerId: UUID) {
        process(PlayerBuzzed(playerId))
    }

    fun pauseTimer() = process(PauseTimer)

    fun resumeTimer() = process(ResumeTimer)

    fun markAnswerCorrect() = process(HostAccepted)

    fun markAnswerWrong() = process(HostRejected)

    fun skipQuestion() = process(SkipQuestion)

    fun showAnswer() = process(AnswerShown)

    private fun revealQuestion() = process(QuestionRevealed)

    fun nextRound() = process(NextRound)

    fun adjustScore(playerId: UUID, delta: Int) = process(AdjustPlayerScore(playerId, delta))

    private fun process(event: GameEvent) {
        val previousPhase = engine.phase
        val wasTimerPaused = engine.state.isTimerPaused
        engine.process(event).fold(
            ifLeft = { error ->
                setError(error.message)
            },
            ifRight = {
                timerOrchestrator.onPhaseChange(previousPhase, engine.phase, wasTimerPaused)
                clearMessages()
                publishState()
                autoSelectIfSingleCandidate()
            },
        )
    }

    private fun autoSelectIfSingleCandidate() {
        if (engine.phase == GamePhase.ChoosingPlayer) {
            val candidates = uiState.lowestScoreCandidates
            if (candidates.size == 1) {
                selectActivePlayer(candidates.single().id)
            }
        }
    }

    fun mediaFinished() {
        timerOrchestrator.onMediaFinished()
        publishState()
    }

    fun skipMedia() {
        mediaStopSignal++
        timerOrchestrator.onMediaFinished()
        publishState()
    }

    private fun loadPack(path: Path) {
        packLoader.load(path)
            .onSuccess { loaded ->
                loadedPack = loaded.pack
                loadedPackPath = path.toString()
                extractedBasePath = loaded.extractedBasePath
                replaceSession(loaded.pack)
                setInfo("Loaded pack: ${loaded.pack.name}")
            }
            .onFailure { error ->
                error.printStackTrace()
                setError(error.message ?: "Failed to load pack")
            }
    }

    private fun replaceSession(pack: Package) {
        timerOrchestrator.stop()
        server.stop()

        engine = createEngine(pack)
        timer = GameTimer(engine, scope)
        server = GameServer(engine, port, buzzAllowed = { !engine.state.isTimerPaused })
        timerOrchestrator = TimerOrchestrator(timer, engine, scope, ::showAnswer, ::revealQuestion)
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
        uiState = uiState.withEngineSnapshot(
            engine = engine,
            loadedPackPath = loadedPackPath,
            extractedBasePath = extractedBasePath,
            serverUrl = "http://${resolveLanIp()}:$port",
        ).copy(
            mediaActive = timerOrchestrator.isMediaPending,
            mediaStopSignal = mediaStopSignal,
        )
    }

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

}
