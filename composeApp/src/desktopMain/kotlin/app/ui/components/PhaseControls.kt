package app.ui.components

import app.ui.theme.Palette
import app.session.DesktopSessionController
import app.state.DesktopUiState
import app.state.hasMedia
import app.state.hostSummary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sick.model.Answer
import com.sick.service.ScoreMultiplier
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
                    if (state.mediaActive || state.mediaReplaying) {
                        Button(onClick = controller::skipMedia) {
                            Text("Skip Media")
                        }
                    }
                    if (!state.mediaActive && state.currentQuestion?.hasMedia() == true) {
                        Button(onClick = controller::replayMedia, enabled = !state.isTimerPaused) {
                            Text("Replay Media")
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
                    if (state.isTimerPaused) {
                        Button(onClick = controller::resumeTimer) {
                            Text("Resume")
                        }
                    } else {
                        Button(onClick = controller::pauseTimer) {
                            Text("Pause")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    AcceptButtonGroup(onAccept = controller::markAnswerCorrect)
                    Button(
                        onClick = controller::markAnswerWrong,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Palette.Error, contentColor = Color.White),
                    ) {
                        Text("Wrong")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = controller::skipAnswer) {
                        Text("Skip Answer")
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

/** Joined `[x0.5 | Correct | x2]` buttons; the scaled variants are tinted lighter than Correct. */
@Composable
private fun AcceptButtonGroup(onAccept: (ScoreMultiplier) -> Unit) {
    val corner = 4.dp
    @Composable
    fun segment(multiplier: ScoreMultiplier, label: String, shape: Shape, alpha: Float) {
        Button(
            onClick = { onAccept(multiplier) },
            shape = shape,
            elevation = null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Palette.Success.copy(alpha = alpha),
                contentColor = Color.White,
            ),
        ) {
            Text(label)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        segment(ScoreMultiplier.HALF, "x0.5", RoundedCornerShape(topStart = corner, bottomStart = corner), alpha = 0.6f)
        segment(ScoreMultiplier.FULL, "Correct", RectangleShape, alpha = 1f)
        segment(ScoreMultiplier.DOUBLE, "x2", RoundedCornerShape(topEnd = corner, bottomEnd = corner), alpha = 0.6f)
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
