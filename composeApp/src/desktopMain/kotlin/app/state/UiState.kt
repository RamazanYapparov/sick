package app.state

import com.sick.engine.GameEngine
import com.sick.model.Player
import com.sick.model.Question
import com.sick.model.Round
import com.sick.service.lowestScoreCandidates
import com.sick.state.GamePhase
import java.nio.file.Path
import java.util.UUID

data class DesktopUiState(
    val packName: String,
    val loadedPackPath: String?,
    val extractedBasePath: Path?,
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
    val lowestScoreCandidates: List<Player>,
    val timerRemaining: Int,
    val isTimerPaused: Boolean,
    val mediaActive: Boolean,
    val mediaStopSignal: Int,
    val mediaPaused: Boolean,
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
            extractedBasePath = null,
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
            lowestScoreCandidates = emptyList(),
            timerRemaining = 0,
            isTimerPaused = false,
            mediaActive = false,
            mediaStopSignal = 0,
            mediaPaused = false,
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

fun DesktopUiState.withEngineSnapshot(
    engine: GameEngine,
    loadedPackPath: String?,
    extractedBasePath: Path?,
    serverUrl: String,
): DesktopUiState {
    val state = engine.state
    val pack = state.pack
    val currentQuestion = state.currentQuestion
    val hasPack = pack.rounds.isNotEmpty()

    return copy(
        packName = if (hasPack) pack.name else "No pack loaded",
        loadedPackPath = loadedPackPath,
        extractedBasePath = extractedBasePath,
        phase = engine.phase,
        players = state.players,
        activePlayerId = state.activePlayerId,
        answeringPlayerId = state.answeringPlayerId,
        roundName = state.currentRound?.name,
        currentRoundIndex = if (hasPack) state.currentRoundIndex + 1 else 0,
        totalRounds = pack.rounds.size,
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
        lowestScoreCandidates = state.lowestScoreCandidates(),
        timerRemaining = state.timerRemaining,
        isTimerPaused = state.isTimerPaused,
        serverUrl = serverUrl,
        hasPack = hasPack,
    )
}

private fun findThemeName(round: Round?, questionId: UUID): String? =
    round?.themes?.firstOrNull { theme -> theme.questions.any { it.id == questionId } }?.name
