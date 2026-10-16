package app.ui.components

import app.ui.theme.Palette
import app.session.DesktopSessionController
import app.state.HostControls
import app.state.TimerView
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
import com.sick.service.ScoreMultiplier

@Composable
internal fun PhaseControls(controls: HostControls, controller: DesktopSessionController) {
    SectionCard("Controls") {
        when (controls) {
            is HostControls.Lobby -> {
                Text("Load a pack, add players, then start the game.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::startGame, enabled = controls.canStart) {
                    Text("Start Game")
                }
            }
            is HostControls.ChoosingPlayer -> {
                Text("Name the Chooser (players with the lowest score).")
                Spacer(Modifier.height(8.dp))
                PlayerChipRow(choices = controls.candidates, onClick = controller::selectActivePlayer)
            }
            is HostControls.ChoosingQuestion -> {
                Text("Chooser: ${controls.chooserName ?: "No chooser"}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::skipRound) {
                    Text("Skip Round")
                }
            }
            HostControls.Revealing -> {
                Text("Revealing question…")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::skipQuestion) {
                    Text("Skip")
                }
            }
            is HostControls.Question -> {
                Text("Question is live. ${timerLabel(controls.timer)}")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PauseButton(controls.timer, controller)
                    if (controls.canSkipMedia) {
                        Button(onClick = controller::skipMedia) {
                            Text("Skip Media")
                        }
                    }
                    if (controls.canReplayMedia) {
                        Button(onClick = controller::replayMedia) {
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
                    choices = controls.picks,
                    onClick = controller::chooseAnsweringPlayer,
                    enabled = controls.canPick,
                )
            }
            is HostControls.Answering -> {
                Text("Answering player: ${controls.playerName}. ${timerLabel(controls.timer)}")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PauseButton(controls.timer, controller)
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
            HostControls.ShowingAnswer -> {
                Text("Answer revealed.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::showAnswer) {
                    Text("Continue")
                }
            }
            HostControls.RoundEnd -> {
                Text("Round complete.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::nextRound) {
                    Text("Next Round")
                }
            }
            HostControls.GameOver -> {
                Text("Game over.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::resetGame) {
                    Text("New Game")
                }
            }
        }
    }
}

private fun timerLabel(timer: TimerView) =
    if (timer.paused) "Timer: ${timer.seconds}s (paused)" else "Timer: ${timer.seconds}s"

@Composable
private fun PauseButton(timer: TimerView, controller: DesktopSessionController) {
    if (timer.paused) {
        Button(onClick = controller::resumeTimer) {
            Text("Resume")
        }
    } else {
        Button(onClick = controller::pauseTimer) {
            Text("Pause")
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

/** The answer in words, or a placeholder when [summary] is null because no question is in play. */
@Composable
internal fun HostAnswerCard(summary: List<String>?) {
    SectionCard("Host Answer") {
        if (summary == null) {
            Text("No active question.")
        } else {
            summary.forEach { line ->
                Text(line)
            }
        }
    }
}
