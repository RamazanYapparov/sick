@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.state.GamePhase

@Composable
internal fun HostWindowContent(controller: DesktopSessionController, state: DesktopUiState) {
    var newPlayerName by remember { mutableStateOf("") }
    val renameDrafts = remember { mutableStateMapOf<String, String>() }
    val scoreDrafts = remember { mutableStateMapOf<String, String>() }
    val lobbyEditable = state.phase == GamePhase.Lobby

    Surface(color = MaterialTheme.colors.background) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionCard("Session") {
                    Text("Pack: ${state.packName}", fontWeight = FontWeight.Bold)
                    if (state.loadedPackPath != null) {
                        Text(state.loadedPackPath, fontSize = 12.sp, color = Color(0xFF555555))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = controller::loadPackFromDialog) {
                            Text("Load Pack")
                        }
                        Button(onClick = controller::resetGame) {
                            Text("Create Game")
                        }
                        Button(onClick = controller::showDisplayWindow) {
                            Text("Show Display")
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
                    state.infoMessage?.let { Text(it, color = Color(0xFF2E7D32)) }
                    state.errorMessage?.let { Text(it, color = Color(0xFFB3261E)) }
                }

                SectionCard("Players") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = { newPlayerName = it },
                            label = { Text("New player") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = lobbyEditable,
                        )
                        Button(
                            onClick = {
                                controller.addPlayer(newPlayerName)
                                newPlayerName = ""
                            },
                            enabled = lobbyEditable,
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (state.players.isEmpty()) {
                        Text("No players yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.players.forEach { player ->
                                val renameKey = player.id.toString()
                                val scoreKey = player.id.toString()
                                val renameValue = renameDrafts.getOrPut(renameKey) { player.name }
                                val scoreValue = scoreDrafts.getOrPut(scoreKey) { "100" }

                                PlayerEditorRow(
                                    player = player,
                                    renameValue = renameValue,
                                    onRenameChange = { renameDrafts[renameKey] = it },
                                    onRenameCommit = { controller.renamePlayer(player.id, renameDrafts[renameKey].orEmpty()) },
                                    onRemove = { controller.removePlayer(player.id) },
                                    renameEnabled = lobbyEditable,
                                    scoreDelta = scoreValue,
                                    onScoreChange = { scoreDrafts[scoreKey] = it },
                                    onAdjustScore = {
                                        scoreDrafts[scoreKey]?.toIntOrNull()?.let { delta ->
                                            controller.adjustScore(player.id, delta)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                PhaseControls(state = state, controller = controller)

                HostAnswerCard(state.currentQuestion?.answer)
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Current Question", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.fillMaxSize()
                        .border(1.dp, Color(0xFFB7AA93), RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A2B35), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                ) {
                    if (state.currentQuestion != null) {
                        CurrentQuestionPanel(
                            state = state,
                            compact = true,
                            bodySize = 12.sp,
                            timerSize = 24.sp,
                            onVideoFinished = controller::videoFinished,
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active question", color = Color(0xFF7A9BAA))
                        }
                    }
                }
            }
        }
    }
}
