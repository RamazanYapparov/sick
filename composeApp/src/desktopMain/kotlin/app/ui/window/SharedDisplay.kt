package app.ui.window

import app.ui.components.PlayerCards
import app.ui.components.QuestionBoard
import app.ui.components.QrCode
import app.ui.theme.Palette
import app.state.DesktopUiState
import app.state.QuestionDisplayItem
import app.state.displayContents
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer
import com.sick.state.GamePhase
import java.nio.file.Path

@Composable
internal fun SharedDisplayScreen(state: DesktopUiState, compact: Boolean, onMediaFinished: () -> Unit = {}) {
    val pad = if (compact) 12.dp else 24.dp
    val bodySize = if (compact) 12.sp else 22.sp
    val timerSize = if (compact) 24.sp else 46.sp

    val currentAnswer = state.currentQuestion?.answer
    val questionItems = if (state.phase == GamePhase.ShowingAnswer && currentAnswer is Answer.Simple) {
        displayContents(currentAnswer.contents, state.extractedBasePath)
    } else {
        state.currentQuestion?.displayContents(state.extractedBasePath).orEmpty()
    }
    val useFullscreenMedia = shouldUseFullscreenQuestionMedia(state.phase, questionItems)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background,
    ) {
        if (useFullscreenMedia) {
            FullscreenQuestionMedia(
                state = state,
                items = questionItems,
                bodySize = bodySize,
                timerSize = timerSize,
                onMediaFinished = onMediaFinished,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(pad),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
            ) {
                Text(
                    if (state.roundName != null) "Round ${state.currentRoundIndex} / ${state.totalRounds}" else "Lobby",
                    fontSize = bodySize,
                    color = Palette.AccentGold,
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.phase == GamePhase.ShowingAnswer && state.currentQuestion != null ->
                            AnswerPanel(
                                state.currentQuestion.answer,
                                state.extractedBasePath,
                                compact,
                                bodySize,
                                onMediaFinished = onMediaFinished,
                                mediaStopSignal = state.mediaStopSignal,
                                mediaPaused = state.mediaPaused,
                            )
                        state.phase == GamePhase.RevealingQuestion && state.currentQuestion != null ->
                            RevealingQuestionPlaceholder(state, compact, bodySize)
                        state.currentQuestion != null ->
                            CurrentQuestionPanel(state, compact, bodySize, timerSize, onMediaFinished)
                        state.phase == GamePhase.Lobby && state.hasPack ->
                            LobbyPanel(state, compact)
                        else ->
                            BoardOverview(state, compact)
                    }
                }
                PlayerCards(state.players, state.activePlayerId, state.answeringPlayerId, state.skipVotePlayerIds, state.failedBuzzPlayerIds, compact)
            }
        }
    }
}

internal fun shouldUseFullscreenQuestionMedia(
    phase: GamePhase,
    items: List<QuestionDisplayItem>,
): Boolean =
    (phase == GamePhase.ShowingQuestion ||
        phase == GamePhase.PlayerAnswering ||
        phase == GamePhase.ShowingAnswer) &&
        items.any(QuestionDisplayItem::isVisualMedia)

private fun QuestionDisplayItem.isVisualMedia(): Boolean =
    this is QuestionDisplayItem.LocalImage ||
        this is QuestionDisplayItem.RemoteImage ||
        this is QuestionDisplayItem.LocalVideo ||
        this is QuestionDisplayItem.RemoteVideo

@Composable
private fun FullscreenQuestionMedia(
    state: DesktopUiState,
    items: List<QuestionDisplayItem>,
    bodySize: TextUnit,
    timerSize: TextUnit,
    onMediaFinished: () -> Unit,
) {
    val visualItems = items.filter(QuestionDisplayItem::isVisualMedia)
    val textItems = buildList {
        if (state.phase == GamePhase.ShowingAnswer) {
            addAll((state.currentQuestion?.answer as? Answer.Simple)?.right.orEmpty())
        }
        addAll(items.filterIsInstance<QuestionDisplayItem.Text>().map { it.text })
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (visualItems.size == 1) {
            RenderQuestionDisplayItem(
                item = visualItems.single(),
                compact = false,
                bodySize = bodySize,
                onMediaFinished = onMediaFinished,
                mediaStopSignal = state.mediaStopSignal,
                mediaPaused = state.mediaPaused,
                fillAvailableSpace = true,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                visualItems.forEach { item ->
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        RenderQuestionDisplayItem(
                            item = item,
                            compact = false,
                            bodySize = bodySize,
                            onMediaFinished = onMediaFinished,
                            mediaStopSignal = state.mediaStopSignal,
                            mediaPaused = state.mediaPaused,
                            fillAvailableSpace = true,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    if (state.phase == GamePhase.ShowingAnswer) "Answer" else state.currentThemeName ?: "Question",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.AccentGold,
                )
                Text(
                    "${state.currentQuestion?.price ?: 0} points",
                    fontSize = bodySize,
                    color = Color.White,
                )
            }

            val remaining = if (state.phase == GamePhase.PlayerAnswering) {
                state.answerTimerRemaining
            } else {
                state.timerRemaining
            }
            if (remaining > 0) {
                Text(
                    text = "$remaining",
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    fontSize = timerSize,
                    fontWeight = FontWeight.Bold,
                    color = Palette.TimerColor,
                )
            }
        }

        if (textItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 56.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                textItems.forEach { text ->
                    Text(
                        text = text,
                        fontSize = bodySize,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun LobbyPanel(state: DesktopUiState, compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(if (compact) 16.dp else 32.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)
            ) {
                Text(
                    text = "Pack Loaded",
                    fontSize = if (compact) 12.sp else 16.sp,
                    color = Palette.AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.packName,
                    fontSize = if (compact) 18.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(if (compact) 4.dp else 12.dp))

                Text(
                    text = "Connect to Play",
                    fontSize = if (compact) 14.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Scan the QR code or enter the connection URL in your browser to join as a player.",
                    fontSize = if (compact) 11.sp else 16.sp,
                    color = Palette.InfoText
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.ThemeBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = state.serverUrl,
                        fontSize = if (compact) 12.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.AccentGold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    backgroundColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(if (compact) 8.dp else 16.dp)
                ) {
                    QrCode(
                        text = state.serverUrl,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        fgColor = Palette.ThemeBackground,
                        bgColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardOverview(state: DesktopUiState, compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(if (compact) 12.dp else 20.dp)) {
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
                showCompleted = state.showCompleted,
                fillHeight = true,
            )
        }
    }
}

@Composable
internal fun SelectOptionsList(
    options: List<Answer.Select.Option>,
    basePath: Path?,
    compact: Boolean,
    bodySize: TextUnit,
    revealCorrect: Boolean,
) {
    // basePath is reserved for future per-option media; intentionally unused for v1.
    @Suppress("UNUSED_PARAMETER") val reservedBasePath = basePath
    val effectiveBodySize = if (options.size >= 6) bodySize * 0.9f else bodySize
    val letterSize = bodySize * 1.1f
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val highlight = revealCorrect && option.correct
            val confirmed = revealCorrect && !option.correct
            val rowBg = if (highlight) Palette.PlayerAnswering else Palette.SelectRowBg
            val textColor = when {
                highlight -> Palette.PlayerAnsweringText
                confirmed -> Palette.ConfirmedText
                else -> Color.White
            }
            val letterBg = if (highlight) Palette.PlayerAnswering else Palette.SelectLetterBg
            val letterColor = when {
                highlight -> Palette.PlayerAnsweringText
                confirmed -> Palette.ConfirmedText
                else -> Palette.AccentGold
            }
            val textFontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .background(letterBg, RoundedCornerShape(if (compact) 4.dp else 6.dp))
                            .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 2.dp else 4.dp),
                    ) {
                        Text(
                            option.name,
                            fontSize = letterSize,
                            color = letterColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        option.answer,
                        fontSize = effectiveBodySize,
                        color = textColor,
                        fontWeight = textFontWeight,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CurrentQuestionPanel(state: DesktopUiState, compact: Boolean, bodySize: TextUnit, timerSize: TextUnit, onMediaFinished: () -> Unit = {}) {
    val question = state.currentQuestion ?: return
    val selectAnswer = question.answer
    val displayItems = question.displayContents(state.extractedBasePath)
    val singleVisualItem = displayItems.filter(QuestionDisplayItem::isVisualMedia).singleOrNull()

    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (compact) 12.dp else 24.dp),
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
                // During PlayerAnswering: answer timer. Other phases: question timer.
                when (state.phase) {
                    GamePhase.PlayerAnswering -> Text(
                        text = "${state.answerTimerRemaining}",
                        fontSize = timerSize,
                        fontWeight = FontWeight.Bold,
                        color = Palette.TimerColor,
                    )
                    else -> if (state.timerRemaining > 0) Text(
                        text = "${state.timerRemaining}",
                        fontSize = timerSize,
                        fontWeight = FontWeight.Bold,
                        color = Palette.TimerColor,
                    )
                }
            }

            Divider(color = Palette.DividerColor)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
                ) {
                    displayItems.forEach { item ->
                        if (item === singleVisualItem) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                RenderQuestionDisplayItem(
                                    item = item,
                                    compact = compact,
                                    bodySize = bodySize,
                                    onMediaFinished = onMediaFinished,
                                    mediaStopSignal = state.mediaStopSignal,
                                    mediaPaused = state.mediaPaused,
                                    fillAvailableSpace = true,
                                )
                            }
                        } else {
                            RenderQuestionDisplayItem(
                                item = item,
                                compact = compact,
                                bodySize = bodySize,
                                onMediaFinished = onMediaFinished,
                                mediaStopSignal = state.mediaStopSignal,
                                mediaPaused = state.mediaPaused,
                            )
                        }
                    }
                    if (selectAnswer is Answer.Select) {
                        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                        SelectOptionsList(
                            options = selectAnswer.options,
                            basePath = state.extractedBasePath,
                            compact = compact,
                            bodySize = bodySize,
                            revealCorrect = false,
                        )
                    }
                }
            }
        }
    }
}
