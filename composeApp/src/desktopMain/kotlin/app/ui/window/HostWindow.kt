@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui.window

import app.ui.components.QuestionBoard
import app.ui.components.SectionCard
import app.ui.components.PlayerEditorRow
import app.ui.components.PhaseControls
import app.ui.components.HostAnswerCard
import app.ui.theme.Palette
import app.session.DesktopSessionController
import app.state.DesktopUiState
import com.sick.state.GamePhase
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HostWindowContent(controller: DesktopSessionController, state: DesktopUiState) {
    val scoreDrafts = remember { mutableStateMapOf<String, String>() }

    Surface(color = MaterialTheme.colors.background) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Question board at the top so it's immediately visible
                if (state.phase == GamePhase.ChoosingQuestion) {
                    SectionCard("Question Board") {
                        QuestionBoard(
                            themes = state.boardThemes,
                            enabled = true,
                            onQuestionClick = controller::selectQuestion,
                            showCompleted = state.showCompleted,
                            onShowCompletedToggle = controller::toggleShowCompleted,
                        )
                    }
                }

                // Host answer right below the question board
                HostAnswerCard(state.currentQuestion?.answer)

                PhaseControls(state = state, controller = controller)

                SectionCard("Players") {
                    if (state.players.isEmpty()) {
                        Text("No players yet. Players join via the buzzer URL.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.players.forEach { player ->
                                val scoreKey = player.id.toString()
                                val scoreValue = scoreDrafts.getOrPut(scoreKey) { "100" }
                                val playerBg = when {
                                    player.id in state.failedBuzzPlayerIds -> Palette.PlayerFailed
                                    player.id in state.skipVotePlayerIds -> Palette.PlayerSkipped
                                    else -> Palette.DarkSurface
                                }
                                PlayerEditorRow(
                                    player = player,
                                    scoreDelta = scoreValue,
                                    onScoreChange = { scoreDrafts[scoreKey] = it },
                                    onAdjustScore = { controller.adjustScore(player.id, it) },
                                    backgroundColor = playerBg,
                                )
                            }
                        }
                    }
                }

                SectionCard("Session") {
                    Text("Pack: ${state.packName}", fontWeight = FontWeight.Bold)
                    if (state.loadedPackPath != null) {
                        Text(state.loadedPackPath, fontSize = 12.sp, color = Palette.SecondaryText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = controller::showPackBrowser) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse", modifier = Modifier.padding(end = 4.dp).size(18.dp))
                            Text("Browse Packs")
                        }
                        Button(onClick = controller::loadPackFromDialog) {
                            Text("Open File...")
                        }
                        Button(onClick = controller::resetGame) {
                            Text("Create Game")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Phase: ${state.phase.name}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Buzzer page: ${state.serverUrl}")
                        val clipboard = LocalClipboardManager.current
                        IconButton(onClick = { clipboard.setText(AnnotatedString(state.serverUrl)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", modifier = Modifier.padding(4.dp))
                        }
                    }
                    state.infoMessage?.let { Text(it, color = Palette.Success) }
                    state.errorMessage?.let { Text(it, color = Palette.Error) }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (state.phase == GamePhase.ShowingAnswer) "Answer" else "Current Question",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .border(1.dp, Palette.BorderColor, RoundedCornerShape(20.dp))
                        .background(Palette.PanelBackground, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                ) {
                    if (state.currentQuestion != null) {
                        if (state.phase == GamePhase.ShowingAnswer) {
                            AnswerPanel(
                                answer = state.currentQuestion.answer,
                                basePath = state.extractedBasePath,
                                compact = true,
                                bodySize = 12.sp,
                                onMediaFinished = controller::mediaFinished,
                                mediaStopSignal = state.mediaStopSignal,
                                mediaPaused = state.mediaPaused,
                            )
                        } else if (state.phase == GamePhase.RevealingQuestion) {
                            // Match the shared display: don't show question content
                            // (video/audio) until the reveal animation finishes, so
                            // both windows start media playback in sync.
                            RevealingQuestionPlaceholder(
                                state = state,
                                compact = true,
                                bodySize = 12.sp,
                            )
                        } else {
                            CurrentQuestionPanel(
                                state = state,
                                compact = true,
                                bodySize = 12.sp,
                                timerSize = 24.sp,
                                onMediaFinished = controller::mediaFinished,
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active question", color = Palette.MutedText)
                        }
                    }
                }
            }
        }
    }

    // Pack browser dialog
    if (state.showPackBrowser) {
        PackBrowserDialog(
            scannedPacks = state.scannedPacks,
            isScanning = state.isScanningPacks,
            onDismiss = controller::hidePackBrowser,
            onRefresh = controller::refreshLocalPacks,
            onBrowseFiles = {
                controller.hidePackBrowser()
                controller.loadPackFromDialog()
            },
            onChoosePack = controller::chooseLocalPack,
        )
    }
}




