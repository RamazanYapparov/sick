package app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.session.PackScanner
import app.session.ScannedPackInfo
import app.state.AudioPlaybackState
import app.state.DesktopUiState
import app.state.HostSession
import app.state.audienceViews
import com.sick.engine.EngineTransition
import com.sick.engine.GameEngine
import com.sick.event.AdjustPlayerScore
import com.sick.event.AnswerShown
import com.sick.event.GameEvent
import com.sick.event.HostAccepted
import com.sick.event.HostRejected
import com.sick.event.HostSkippedAnswer
import com.sick.event.NextRound
import com.sick.event.PauseTimer
import com.sick.event.PlayerBuzzed
import com.sick.event.PlayerLeft
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
import com.sick.service.ScoreMultiplier
import com.sick.service.lowestScoreCandidates
import com.sick.siq.OpenedPack
import com.sick.siq.SiqPack
import com.sick.state.GamePhase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.EventQueue
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

class DesktopSessionController(
    private val scope: CoroutineScope,
    private val port: Int = 8080,
) {
    /** The loaded pack; its extracted media live until it is closed. */
    private var openedPack: OpenedPack? = null
    private var loadedPackPath: String? = null
    private var scannedPacks: List<ScannedPackInfo> = emptyList()
    private var isScanningPacks: Boolean = false

    private var engine: GameEngine = createEngine(emptyPack())
    private var server: GameServer = GameServer(engine, port, buzzAllowed = { !engine.state.isTimerPaused })
    private var playback: QuestionPlayback = QuestionPlayback(engine, scope)
    private var engineListener: (EngineTransition) -> Unit = bindEngine(engine, playback)

    private var showCompleted = false

    private var stateChangeCount = 0L

    var uiState by mutableStateOf(withGameViews(previous = null))
        private set

    /** Progress of the audio played on the Shared Display, mirrored on the host. */
    val audioPlayback = AudioPlaybackState()

    init {
        server.start()
    }

    fun dispose() {
        playback.close()
        engine.removeListener(engineListener)
        server.stop()
        openedPack?.close()
    }

    fun loadPackFromDialog() {
        val path = pickSiqFile() ?: return
        loadPack(path)
    }

    fun showPackBrowser() {
        updateSession { copy(showPackBrowser = true) }
        if (scannedPacks.isEmpty() && !isScanningPacks) {
            refreshLocalPacks()
        }
    }

    fun hidePackBrowser() {
        updateSession { copy(showPackBrowser = false) }
    }

    fun refreshLocalPacks() {
        if (isScanningPacks) return
        isScanningPacks = true
        updateSession { copy(isScanningPacks = true) }
        scope.launch {
            val packs = withContext(Dispatchers.IO) {
                PackScanner.scan()
            }
            scannedPacks = packs
            isScanningPacks = false
            updateSession { copy(scannedPacks = packs, isScanningPacks = false) }
        }
    }

    fun chooseLocalPack(path: Path) {
        hidePackBrowser()
        loadPack(path)
    }

    fun resetGame() {
        val pack = openedPack?.pack
        replaceSession(pack ?: emptyPack())
        setInfo(
            if (pack != null) "Game reset with current pack."
            else "Empty game created. Load a pack to start playing."
        )
    }

    fun startGame() = dispatch(StartGame)
    fun selectActivePlayer(playerId: UUID) = dispatch(SelectActivePlayer(playerId))
    fun selectQuestion(questionId: UUID) {
        showCompleted = false
        dispatch(QuestionSelected(questionId))
    }
    fun chooseAnsweringPlayer(playerId: UUID) = dispatch(PlayerBuzzed(playerId))
    fun markAnswerCorrect(multiplier: ScoreMultiplier) = dispatch(HostAccepted(multiplier))
    fun markAnswerWrong() = dispatch(HostRejected)
    fun skipAnswer() = dispatch(HostSkippedAnswer)
    fun skipQuestion() = dispatch(SkipQuestion)
    fun skipRound() = dispatch(SkipRound)
    fun showAnswer() = dispatch(AnswerShown)
    fun nextRound() = dispatch(NextRound)
    fun adjustScore(playerId: UUID, delta: Int) = dispatch(AdjustPlayerScore(playerId, delta))
    fun removePlayer(playerId: UUID) = dispatch(PlayerLeft(playerId))

    fun pauseTimer() = dispatch(PauseTimer)
    fun resumeTimer() = dispatch(ResumeTimer)

    fun toggleShowCompleted() {
        showCompleted = !showCompleted
        publishState()
    }

    /** Single host entry point. */
    private fun dispatch(event: GameEvent) {
        logger.debug { "dispatch: ${event::class.simpleName}, previousPhase=${engine.previousPhase}, thread=${Thread.currentThread().name}" }
        engine.process(event).fold(
            ifLeft = { error ->
                logger.warn { "dispatch: ${event::class.simpleName} rejected: ${error.message}" }
                setError(error.message)
            },
            ifRight = {
                logger.debug { "dispatch: ${event::class.simpleName} OK -> ${engine.phase.name}" }
                clearMessages()
                publishState()
                autoSelectIfSingleCandidate()
            },
        )
    }

    private fun autoSelectIfSingleCandidate() {
        if (engine.phase == GamePhase.ChoosingPlayer) {
            val candidates = engine.state.lowestScoreCandidates()
            if (candidates.size == 1) {
                selectActivePlayer(candidates.single().id)
            }
        }
    }

    /** A media player finished, failed, or could not start; vlcj reports this on its own event thread. */
    fun mediaEnded(generation: Int, index: Int) = onEdt {
        playback.onMediaEnded(generation, index)
        publishState()
    }

    fun skipMedia() {
        playback.skipMedia()
        publishState()
    }

    fun replayMedia() {
        playback.replayMedia()
        publishState()
    }

    private fun loadPack(path: Path) {
        runCatching { SiqPack.open(path) }
            .onSuccess { opened ->
                val previous = openedPack
                openedPack = opened
                loadedPackPath = path.toString()
                replaceSession(opened.pack)
                // A media player may still hold one of its files; what is left is deleted at exit.
                previous?.close()
                setInfo("Loaded pack: ${opened.pack.name}")
                logger.info { "Loaded pack: ${opened.pack.name} from $path" }
                logPackContents(opened.pack)
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
                            is Content.Media -> when (val source = it.source) {
                                is Content.Media.Source.Local -> "${it.type}(${source.path.fileName})"
                                is Content.Media.Source.Remote -> "${it.type}(${source.url})"
                            }
                        }
                    }
                    val answerSummary = when (val a = q.answer) {
                        is Answer.Simple -> {
                            val c = if (a.contents.isNotEmpty()) " | Contents: ${a.contents.size}" else ""
                            "Simple(right=${a.right}, wrong=${a.wrong}$c)"
                        }
                        is Answer.Select -> "Select(options=${a.options.map { "${it.name}=${it.answer}" }})"
                        is Answer.Point -> a.toString()
                    }
                    logger.info { "      [${q.price}] ${q.type::class.simpleName} | Content: $contentSummary | Answer: $answerSummary" }
                }
            }
        }
        logger.info { "=== End Pack ===" }
    }

    private fun replaceSession(pack: Package) {
        playback.close()
        engine.removeListener(engineListener)
        server.stop()

        engine = createEngine(pack)
        server = GameServer(engine, port, buzzAllowed = { !engine.state.isTimerPaused })
        playback = QuestionPlayback(engine, scope)
        engineListener = bindEngine(engine, playback)
        server.start()
        publishState()
        logger.info { "Session replaced with new pack: ${pack.name}" }
    }

    /**
     * Engine listeners run on whichever thread called `engine.process`: the EDT
     * for host actions and timers, a Ktor worker for phone requests. Playback and
     * Compose state are EDT-only, so the work hops there when needed. A
     * transition still queued when the session is replaced reaches only its own,
     * closed playback.
     */
    private fun bindEngine(target: GameEngine, targetPlayback: QuestionPlayback): (EngineTransition) -> Unit {
        val listener: (EngineTransition) -> Unit = { transition ->
            onEdt {
                targetPlayback.onTransition(transition)
                if (targetPlayback === playback) publishState()
            }
        }
        target.addListener(listener)
        return listener
    }

    private fun onEdt(block: () -> Unit) {
        if (EventQueue.isDispatchThread()) block() else EventQueue.invokeLater(block)
    }

    private fun publishState() {
        stateChangeCount++
        if (!EventQueue.isDispatchThread()) {
            logger.warn { "publishState() called from non-EDT thread: ${Thread.currentThread().name} (count=$stateChangeCount)" }
        }
        uiState = withGameViews(uiState)
    }

    /** [previous] with fresh audience views and session fields; the initial state when it is null. */
    private fun withGameViews(previous: DesktopUiState?): DesktopUiState {
        val url = "http://${resolveLanIp()}:$port"
        val (host, sharedDisplay) = audienceViews(
            state = engine.state,
            phase = engine.phase,
            playback = playback.view,
            joinUrl = url,
            showCompleted = showCompleted,
        )
        val session = (previous?.session ?: HostSession(url)).copy(
            serverUrl = url,
            loadedPackPath = loadedPackPath,
            scannedPacks = scannedPacks,
            isScanningPacks = isScanningPacks,
        )
        return previous?.copy(host = host, sharedDisplay = sharedDisplay, session = session)
            ?: DesktopUiState(host, sharedDisplay, session)
    }

    private fun updateSession(change: HostSession.() -> HostSession) {
        uiState = uiState.copy(session = uiState.session.change())
    }

    private fun setError(message: String) {
        updateSession { copy(errorMessage = message, infoMessage = null) }
    }

    private fun setInfo(message: String) {
        updateSession { copy(infoMessage = message, errorMessage = null) }
    }

    private fun clearMessages() {
        updateSession { copy(errorMessage = null, infoMessage = null) }
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
