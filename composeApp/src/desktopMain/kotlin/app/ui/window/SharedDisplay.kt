package app.ui.window

import app.ui.components.QuestionBoard
import app.ui.components.Scoreboard
import app.ui.theme.Palette
import app.ui.media.AudioPlayer
import app.ui.media.VideoPlayer
import app.state.DesktopUiState
import app.state.QuestionDisplayItem
import app.state.displayContents
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer
import com.sick.state.GamePhase

@Composable
internal fun SharedDisplayScreen(state: DesktopUiState, compact: Boolean, onMediaFinished: () -> Unit = {}) {
    val pad = if (compact) 12.dp else 24.dp
    val titleSize = if (compact) 18.sp else 34.sp
    val bodySize = if (compact) 12.sp else 22.sp
    val timerSize = if (compact) 24.sp else 46.sp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.packName, fontSize = titleSize, fontWeight = FontWeight.Bold)
                    val roundLine = buildString {
                        if (state.roundName != null) {
                            append("Round ${state.currentRoundIndex}/${state.totalRounds}: ${state.roundName}")
                        } else {
                            append("Lobby")
                        }
                    }
                    Text(roundLine, fontSize = bodySize, color = Palette.AccentGold)
                    Text("Phase: ${state.phase.name}", fontSize = bodySize)
                }
                Scoreboard(state.players, state.activePlayerId, state.answeringPlayerId, compact)
            }

            when {
                state.phase == GamePhase.ShowingAnswer && state.currentQuestion != null ->
                    AnswerPanel(state.currentQuestion.answer, compact, bodySize)
                state.phase == GamePhase.RevealingQuestion && state.currentQuestion != null ->
                    RevealingQuestionPlaceholder(state, compact, bodySize)
                state.currentQuestion != null ->
                    CurrentQuestionPanel(state, compact, bodySize, timerSize, onMediaFinished)
                else ->
                    BoardOverview(state, compact)
            }
        }
    }
}

@Composable
private fun BoardOverview(state: DesktopUiState, compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 20.dp)) {
            Text(
                if (state.boardThemes.isEmpty()) "Load a pack to begin." else "Board",
                fontSize = if (compact) 16.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            QuestionBoard(
                themes = state.boardThemes,
                enabled = false,
                onQuestionClick = {},
            )
        }
    }
}

@Composable
private fun RevealingQuestionPlaceholder(state: DesktopUiState, compact: Boolean, bodySize: TextUnit) {
    val question = state.currentQuestion ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                state.currentThemeName ?: "Question",
                fontSize = if (compact) 16.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.AccentGold,
            )
            Text("${question.price} points", fontSize = bodySize, color = Color.White)
        }
    }
}

@Composable
private fun AnswerPanel(answer: Answer, compact: Boolean, bodySize: TextUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            Text(
                "Answer",
                fontSize = if (compact) 16.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.AccentGold,
            )
            Divider(color = Color(0x335F7D8D))
            when (answer) {
                is Answer.Simple -> {
                    answer.right.forEach { right ->
                        Text(right, fontSize = bodySize, color = Color(0xFF5CCD8F), fontWeight = FontWeight.Bold)
                    }
                    if (answer.wrong.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Also accepted:", fontSize = bodySize, color = Color.White)
                        answer.wrong.forEach { wrong ->
                            Text(wrong, fontSize = bodySize, color = Color(0xFFAAAAAA))
                        }
                    }
                }
                is Answer.Select -> {
                    answer.options.forEach { option ->
                        val bg = if (option.correct) Color(0xFF1E4D2B) else Color(0x225F7D8D)
                        val textColor = if (option.correct) Color(0xFF5CCD8F) else Color(0xFFAAAAAA)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("${option.name}: ${option.answer}", fontSize = bodySize, color = textColor, fontWeight = if (option.correct) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CurrentQuestionPanel(state: DesktopUiState, compact: Boolean, bodySize: TextUnit, timerSize: TextUnit, onMediaFinished: () -> Unit = {}) {
    val question = state.currentQuestion ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        state.currentThemeName ?: "Question",
                        fontSize = if (compact) 16.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.AccentGold,
                    )
                    Text("${question.price} points", fontSize = bodySize, color = Color.White)
                }
                if (state.timerRemaining > 0) {
                    Text("${state.timerRemaining}", fontSize = timerSize, fontWeight = FontWeight.Bold, color = Color(0xFFF36C5B))
                }
            }

            Divider(color = Color(0x335F7D8D))

            question.displayContents(state.extractedBasePath).forEach { item ->
                when (item) {
                    is QuestionDisplayItem.Text ->
                        Text(item.text, fontSize = bodySize, color = Color.White)
                    is QuestionDisplayItem.LocalImage -> {
                        val bitmap = remember(item.absolutePath) {
                            runCatching {
                                java.io.File(item.absolutePath).inputStream().buffered()
                                    .use(::loadImageBitmap)
                            }.getOrNull()
                        }
                        if (bitmap != null)
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                        else
                            Text("Image not found: ${item.absolutePath}", color = Color.Red, fontSize = bodySize)
                    }
                    is QuestionDisplayItem.RemoteImage -> {
                        val bitmap = remember(item.url) {
                            runCatching {
                                item.url.openStream().buffered().use(::loadImageBitmap)
                            }.getOrNull()
                        }
                        if (bitmap != null)
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                        else
                            Text("Image unavailable: ${item.url}", color = Color.Red, fontSize = bodySize)
                    }
                    is QuestionDisplayItem.LocalVideo -> {
                        if (compact) {
                            Text("▶ Video", fontSize = bodySize, color = Palette.AccentGold)
                        } else {
                            val uri = remember(item.absolutePath) {
                                java.io.File(item.absolutePath).toURI().toString()
                            }
                            VideoPlayer(
                                uri = uri,
                                modifier = Modifier.fillMaxWidth().height(360.dp),
                                stopSignal = state.mediaStopSignal,
                                paused = state.mediaPaused,
                                onFinished = onMediaFinished,
                            )
                        }
                    }
                    is QuestionDisplayItem.RemoteVideo -> {
                        if (compact) {
                            Text("▶ Video", fontSize = bodySize, color = Palette.AccentGold)
                        } else {
                            VideoPlayer(
                                uri = item.url.toString(),
                                modifier = Modifier.fillMaxWidth().height(360.dp),
                                stopSignal = state.mediaStopSignal,
                                paused = state.mediaPaused,
                                onFinished = onMediaFinished,
                            )
                        }
                    }
                    is QuestionDisplayItem.LocalAudio -> {
                        if (compact) {
                            Text("♫ Audio", fontSize = bodySize, color = Palette.AccentGold)
                        } else {
                            val uri = remember(item.absolutePath) {
                                java.io.File(item.absolutePath).toURI().toString()
                            }
                            AudioPlayer(
                                uri = uri,
                                stopSignal = state.mediaStopSignal,
                                paused = state.mediaPaused,
                                onFinished = onMediaFinished,
                            )
                        }
                    }
                    is QuestionDisplayItem.RemoteAudio -> {
                        if (compact) {
                            Text("♫ Audio", fontSize = bodySize, color = Palette.AccentGold)
                        } else {
                            AudioPlayer(
                                uri = item.url.toString(),
                                stopSignal = state.mediaStopSignal,
                                paused = state.mediaPaused,
                                onFinished = onMediaFinished,
                            )
                        }
                    }
                }
            }
        }
    }
}
