@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui.window

import app.ui.components.QuestionBoard
import app.ui.components.SectionCard
import app.ui.components.PlayerEditorRow
import app.ui.components.PhaseControls
import app.ui.components.HostAnswerCard
import app.ui.theme.LocalPanelSizes
import app.ui.theme.PanelSizes
import app.ui.theme.Palette
import app.session.DesktopSessionController
import app.state.HostSession
import app.state.HostView
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
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
internal fun HostWindowContent(controller: DesktopSessionController, view: HostView, session: HostSession) {
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
                view.board?.let { board ->
                    SectionCard("Question Board") {
                        QuestionBoard(
                            board = board,
                            onQuestionClick = controller::selectQuestion,
                            onShowCompletedToggle = controller::toggleShowCompleted,
                        )
                    }
                }

                // Host answer right below the question board
                HostAnswerCard(view.answerSummary)

                PhaseControls(controls = view.controls, controller = controller)

                SectionCard("Players") {
                    if (view.players.isEmpty()) {
                        Text("No players yet. Players join via the buzzer URL.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            view.players.forEach { player -> key(player.id) {
                                val scoreKey = player.id.toString()
                                val scoreValue = scoreDrafts.getOrPut(scoreKey) { "100" }
                                PlayerEditorRow(
                                    player = player,
                                    scoreDelta = scoreValue,
                                    onScoreChange = { scoreDrafts[scoreKey] = it },
                                    onAdjustScore = { controller.adjustScore(player.id, it) },
                                    canRemove = view.canRemovePlayers,
                                    onRemove = { controller.removePlayer(player.id) },
                                )
                            } }
                        }
                    }
                }

                SectionCard("Session") {
                    Text("Pack: ${view.packName}", fontWeight = FontWeight.Bold)
                    if (session.loadedPackPath != null) {
                        Text(session.loadedPackPath, fontSize = 12.sp, color = Palette.SecondaryText)
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
                    Text("Phase: ${view.phaseName}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Buzzer page: ${session.serverUrl}")
                        val clipboard = LocalClipboardManager.current
                        IconButton(onClick = { clipboard.setText(AnnotatedString(session.serverUrl)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", modifier = Modifier.padding(4.dp))
                        }
                    }
                    session.infoMessage?.let { Text(it, color = Palette.Success) }
                    session.errorMessage?.let { Text(it, color = Palette.Error) }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(view.panelTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.fillMaxSize()
                        .border(1.dp, Palette.BorderColor, RoundedCornerShape(20.dp))
                        .background(Palette.PanelBackground, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                ) {
                    CompositionLocalProvider(LocalPanelSizes provides PanelSizes.Host) {
                        PanelContent(view.panel, controller.audioPlayback, controller::mediaEnded)
                    }
                }
            }
        }
    }

    // Pack browser dialog
    if (session.showPackBrowser) {
        PackBrowserDialog(
            scannedPacks = session.scannedPacks,
            packHistory = session.packHistory,
            canMoveToTrash = controller.canMoveToTrash,
            isScanning = session.isScanningPacks,
            onDismiss = controller::hidePackBrowser,
            onRefresh = controller::refreshLocalPacks,
            onBrowseFiles = {
                controller.hidePackBrowser()
                controller.loadPackFromDialog()
            },
            onChoosePack = controller::chooseLocalPack,
            onDeletePack = controller::deletePack,
        )
    }
}
