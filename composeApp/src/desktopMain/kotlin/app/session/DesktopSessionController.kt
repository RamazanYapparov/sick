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
import com.sick.event.SkipRound
import com.sick.event.StartGame
import com.sick.model.Answer
import com.sick.model.Content
import com.sick.model.Package
import com.sick.server.GameServer
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.EventQueue
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

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
    private var mediaPaused = false
    private var showCompleted = false
    private var lastKnownPhase: GamePhase = GamePhase.Lobby

    private var stateChangeCount = 0L
    private var lastStateChangeMs = 0L
    private var watchdogJob: Job? = null

    var uiState by mutableStateOf(DesktopUiState.initial(port))
        private set

    init {
        bindEngine(engine)
        server.start()
        publishState()
        startWatchdog()
    }

    fun dispose() {
        watchdogJob?.cancel()
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

    fun toggleShowCompleted() {
        showCompleted = !showCompleted
        uiState = uiState.copy(showCompleted = showCompleted)
    }

    fun selectQuestion(questionId: UUID) {
        showCompleted = false
        process(QuestionSelected(questionId))
    }

    fun chooseAnsweringPlayer(playerId: UUID) {
        process(PlayerBuzzed(playerId))
    }

    fun pauseTimer() {
        mediaPaused = true
        val previousPhase = engine.phase
        val wasTimerPaused = engine.state.isTimerPaused
        engine.process(PauseTimer).fold(
            ifLeft = { error ->
                mediaPaused = false
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

    fun resumeTimer() {
        mediaPaused = false
        val previousPhase = engine.phase
        val wasTimerPaused = engine.state.isTimerPaused
        engine.process(ResumeTimer).fold(
            ifLeft = { error ->
                mediaPaused = true
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

    fun markAnswerCorrect() = process(HostAccepted)

    fun markAnswerWrong() = process(HostRejected)

    fun skipQuestion() = process(SkipQuestion)

    fun skipRound() = process(SkipRound)

    fun showAnswer() = process(AnswerShown)

    private fun revealQuestion() = process(QuestionRevealed)

    fun nextRound() = process(NextRound)

    fun adjustScore(playerId: UUID, delta: Int) = process(AdjustPlayerScore(playerId, delta))

    private fun process(event: GameEvent) {
        val previousPhase = engine.phase
        val wasTimerPaused = engine.state.isTimerPaused
        logger.debug { "process: ${event::class.simpleName} from phase=$previousPhase, thread=${Thread.currentThread().name}" }
        engine.process(event).fold(
            ifLeft = { error ->
                logger.warn { "process: ${event::class.simpleName} rejected: ${error.message}" }
                setError(error.message)
            },
            ifRight = {
                logger.debug { "process: ${event::class.simpleName} OK -> ${engine.phase.name}" }
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
                logger.info { "Loaded pack: ${loaded.pack.name} from $path" }
                logPackContents(loaded.pack)
            }
            .onFailure { error ->
                error.printStackTrace()
                setError(error.message ?: "Failed to load pack")
                logger.error(error) { "Failed to load pack from $path" }
            }
    }

    private fun logPackContents(pack: Package) {
        logger.info { "=== Pack: ${pack.name} ===" }
        logger.info { "Author: ${pack.author} | Logo: ${pack.logo} | Tags: ${pack.tags}" }
        pack.rounds.forEachIndexed { ri, round ->
            logger.info { "  Round ${ri + 1}: \"${round.name}\" (${round.type})" }
            round.themes.forEachIndexed { ti, theme ->
                logger.info { "    Theme ${ti + 1}: \"${theme.name}\" — ${theme.questions.size} questions" }
                theme.questions.forEach { q ->
                    val contentSummary = q.contents.joinToString(", ") {
                        when (it) {
                            is Content.Text -> "Text"
                            is Content.Media.FileRef -> "${it.type}(${it.ref})"
                            is Content.Media.FileUrl -> "${it.type}(${it.url})"
                        }
                    }
                    val answerSummary = when (val a = q.answer) {
                        is Answer.Simple -> {
                            val c = if (a.contents.isNotEmpty()) " | Contents: ${a.contents.size}" else ""
                            "Simple(right=${a.right}, wrong=${a.wrong}$c)"
                        }
                        is Answer.Select -> "Select(options=${a.options.map { "${it.name}=${it.answer}" }})"
                    }
                    logger.info { "      [${q.price}] ${q.type::class.simpleName} | Content: $contentSummary | Answer: $answerSummary" }
                }
            }
        }
        logger.info { "=== End Pack ===" }
    }

    private fun replaceSession(pack: Package) {
        timerOrchestrator.stop()
        server.stop()

        engine = createEngine(pack)
        timer = GameTimer(engine, scope)
        server = GameServer(engine, port, buzzAllowed = { !engine.state.isTimerPaused })
        timerOrchestrator = TimerOrchestrator(timer, engine, scope, ::showAnswer, ::revealQuestion)
        mediaPaused = false
        mediaStopSignal = 0
        bindEngine(engine)
        server.start()
        publishState()
        logger.info { "Session replaced with new pack: ${pack.name}" }
    }

    private fun bindEngine(target: GameEngine) {
        lastKnownPhase = target.phase
        target.addListener { _, newPhase ->
            val previousPhase = lastKnownPhase
            lastKnownPhase = newPhase
            val isMediaActive = timerOrchestrator.isMediaPending
            when {
                previousPhase == GamePhase.ShowingQuestion && newPhase == GamePhase.PlayerAnswering && isMediaActive ->
                    mediaPaused = true
                previousPhase == GamePhase.PlayerAnswering && newPhase == GamePhase.ShowingQuestion ->
                    mediaPaused = false
                previousPhase == GamePhase.PlayerAnswering && newPhase == GamePhase.ShowingAnswer -> {
                    mediaPaused = false
                    mediaStopSignal++
                }
                newPhase == GamePhase.ChoosingQuestion -> {
                    mediaStopSignal = 0
                    mediaPaused = false
                }
            }
            publishState()
        }
    }

    private fun publishState() {
        stateChangeCount++
        lastStateChangeMs = System.currentTimeMillis()
        if (!EventQueue.isDispatchThread()) {
            logger.warn { "publishState() called from non-EDT thread: ${Thread.currentThread().name} (count=$stateChangeCount)" }
        }
        uiState = uiState.withEngineSnapshot(
            engine = engine,
            loadedPackPath = loadedPackPath,
            extractedBasePath = extractedBasePath,
            serverUrl = "http://${resolveLanIp()}:$port",
        ).copy(
            mediaActive = timerOrchestrator.isMediaPending,
            mediaStopSignal = mediaStopSignal,
            mediaPaused = mediaPaused,
            showCompleted = showCompleted,
        )
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (true) {
                delay(10_000)
                val now = System.currentTimeMillis()
                val elapsed = now - lastStateChangeMs
                val phaseName = try { engine.phase.name } catch (e: Exception) { "UNKNOWN" }
                val thread = Thread.currentThread().name
                if (elapsed > 15_000) {
                    logger.warn {
                        "watchdog: engine alive phase=$phaseName, thread=$thread, " +
                        "lastStateChange=${elapsed}ms ago, stateChanges=$stateChangeCount"
                    }
                }
                if (elapsed > 60_000) {
                    logger.error {
                        "watchdog: SUSPECTED FREEZE — no state change for ${elapsed}ms. " +
                        "Dumping threads:\n${dumpThreads()}"
                    }
                }
            }
        }
    }

    private fun dumpThreads(): String {
        val sb = StringBuilder()
        val stackTraces = Thread.getAllStackTraces()
        stackTraces.forEach { (thread, stack) ->
            sb.append("\n--- ${thread.name} (${thread.state}) ---\n")
            stack.forEach { element ->
                sb.append("  at $element\n")
            }
        }
        return sb.toString()
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
