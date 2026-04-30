package app.ui.components

import app.ui.theme.Palette
import app.session.DesktopSessionController
import app.state.DesktopUiState
import app.state.hostSummary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sick.model.Answer
import com.sick.state.GamePhase

@Composable
internal fun PhaseControls(state: DesktopUiState, controller: DesktopSessionController) {
    SectionCard("Controls") {
        when (state.phase) {
            GamePhase.Lobby -> {
                Text("Load a pack, add players, then start the game.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::startGame, enabled = state.hasPack && state.players.isNotEmpty()) {
                    Text("Start Game")
                }
            }
            GamePhase.ChoosingPlayer -> {
                Text("Choose who controls the board — showing lowest score players.")
                Spacer(Modifier.height(8.dp))
                PlayerChipRow(
                    players = state.lowestScoreCandidates,
                    activePlayerId = state.activePlayerId,
                    onClick = controller::selectActivePlayer,
                )
            }
            GamePhase.ChoosingQuestion -> {
                val chooser = state.players.firstOrNull { it.id == state.activePlayerId }?.name ?: "No chooser"
                Text("Chooser: $chooser")
                Spacer(Modifier.height(8.dp))
                QuestionBoard(
                    themes = state.boardThemes,
                    enabled = true,
                    onQuestionClick = controller::selectQuestion,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::skipRound) {
                    Text("Skip Round")
                }
            }
            GamePhase.RevealingQuestion -> {
                Text("Revealing question…")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::skipQuestion) {
                    Text("Skip")
                }
            }
            GamePhase.ShowingQuestion -> {
                val timerLabel = if (state.isTimerPaused) "Timer: ${state.timerRemaining}s (paused)" else "Timer: ${state.timerRemaining}s"
                Text("Question is live. $timerLabel")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isTimerPaused) {
                        Button(onClick = controller::resumeTimer) {
                            Text("Resume")
                        }
                    } else {
                        Button(onClick = controller::pauseTimer) {
                            Text("Pause")
                        }
                    }
                    if (state.mediaActive) {
                        Button(onClick = controller::skipMedia) {
                            Text("Skip Media")
                        }
                    }
                    Button(onClick = controller::skipQuestion) {
                        Text("Skip Question")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Choose answering player manually or wait for a phone buzz.")
                Spacer(Modifier.height(8.dp))
                PlayerChipRow(
                    players = state.players.filter { it.id != state.answeringPlayerId },
                    activePlayerId = state.answeringPlayerId,
                    onClick = controller::chooseAnsweringPlayer,
                    enabled = !state.isTimerPaused,
                )
            }
            GamePhase.PlayerAnswering -> {
                val answeringName = state.players.firstOrNull { it.id == state.answeringPlayerId }?.name ?: "Unknown"
                Text("Answering player: $answeringName")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = controller::markAnswerCorrect,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Palette.Success, contentColor = Color.White),
                    ) {
                        Text("Correct")
                    }
                    Button(
                        onClick = controller::markAnswerWrong,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Palette.Error, contentColor = Color.White),
                    ) {
                        Text("Wrong")
                    }
                    Button(onClick = controller::skipQuestion) {
                        Text("Skip")
                    }
                }
            }
            GamePhase.ShowingAnswer -> {
                Text("Answer revealed.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::showAnswer) {
                    Text("Continue")
                }
            }
            GamePhase.RoundEnd -> {
                Text("Round complete.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::nextRound) {
                    Text("Next Round")
                }
            }
            GamePhase.GameOver -> {
                Text("Game over.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::resetGame) {
                    Text("New Game")
                }
            }
        }
    }
}

@Composable
internal fun HostAnswerCard(answer: Answer?) {
    SectionCard("Host Answer") {
        if (answer == null) {
            Text("No active question.")
        } else {
            answer.hostSummary().forEach { line ->
                Text(line)
            }
        }
    }
}
