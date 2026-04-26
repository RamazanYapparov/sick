@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer
import com.sick.model.Player
import com.sick.state.GamePhase

@Composable
fun HostApp(controller: DesktopSessionController) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF235A73),
            secondary = Color(0xFFE8B23A),
            surface = Color(0xFFF6F1E8),
            background = Color(0xFFEDE3D1),
        )
    ) {
        HostWindowContent(controller, controller.uiState)
    }
}

@Composable
fun SharedDisplayApp(state: DesktopUiState) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF184A45),
            secondary = Color(0xFFE5B14C),
            surface = Color(0xFF162C36),
            background = Color(0xFF0E1A21),
            onSurface = Color(0xFFF7F4ED),
            onBackground = Color(0xFFF7F4ED),
        )
    ) {
        SharedDisplayScreen(state = state, compact = false)
    }
}

@Composable
private fun HostWindowContent(controller: DesktopSessionController, state: DesktopUiState) {
    var newPlayerName by remember { mutableStateOf("") }
    var renameDrafts = remember { mutableStateMapOf<String, String>() }
    var scoreDrafts = remember { mutableStateMapOf<String, String>() }
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
                    Text("Buzzer page: ${state.serverUrl}")
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
                Text("Display Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFB7AA93), RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A2B35), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                ) {
                    SharedDisplayScreen(state = state, compact = true)
                }
            }
        }
    }
}

@Composable
private fun PhaseControls(state: DesktopUiState, controller: DesktopSessionController) {
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
                Text("Choose who controls the board.")
                Spacer(Modifier.height(8.dp))
                PlayerChipRow(
                    players = state.players,
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
            }
            GamePhase.ShowingQuestion -> {
                Text("Question is live. Timer: ${state.timerRemaining}s")
                Spacer(Modifier.height(8.dp))
                Text("Choose answering player manually or wait for a phone buzz.")
                Spacer(Modifier.height(8.dp))
                PlayerChipRow(
                    players = state.players.filter { it.id != state.answeringPlayerId },
                    activePlayerId = state.answeringPlayerId,
                    onClick = controller::chooseAnsweringPlayer,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = controller::skipQuestion) {
                    Text("Skip Question")
                }
            }
            GamePhase.PlayerAnswering -> {
                val answeringName = state.players.firstOrNull { it.id == state.answeringPlayerId }?.name ?: "Unknown"
                Text("Answering player: $answeringName")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = controller::markAnswerCorrect,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32), contentColor = Color.White),
                    ) {
                        Text("Correct")
                    }
                    Button(
                        onClick = controller::markAnswerWrong,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFB3261E), contentColor = Color.White),
                    ) {
                        Text("Wrong")
                    }
                    Button(onClick = controller::skipQuestion) {
                        Text("Skip")
                    }
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
private fun HostAnswerCard(answer: Answer?) {
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

@Composable
private fun SharedDisplayScreen(state: DesktopUiState, compact: Boolean) {
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
                    Text(roundLine, fontSize = bodySize, color = Color(0xFFE7C98B))
                    Text("Phase: ${state.phase.name}", fontSize = bodySize)
                }
                Scoreboard(state.players, state.activePlayerId, state.answeringPlayerId, compact)
            }

            if (state.currentQuestion == null) {
                BoardOverview(state, compact)
            } else {
                CurrentQuestionPanel(state, compact, bodySize, timerSize)
            }
        }
    }
}

@Composable
private fun BoardOverview(state: DesktopUiState, compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF18313C),
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
private fun CurrentQuestionPanel(state: DesktopUiState, compact: Boolean, bodySize: androidx.compose.ui.unit.TextUnit, timerSize: androidx.compose.ui.unit.TextUnit) {
    val question = state.currentQuestion ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF18313C),
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
                        color = Color(0xFFE7C98B),
                    )
                    Text("${question.price} points", fontSize = bodySize, color = Color.White)
                }
                if (state.timerRemaining > 0) {
                    Text("${state.timerRemaining}", fontSize = timerSize, fontWeight = FontWeight.Bold, color = Color(0xFFF36C5B))
                }
            }

            Divider(color = Color(0x335F7D8D))

            question.displayLines().forEach { line ->
                Text(line, fontSize = bodySize, color = Color.White)
            }
        }
    }
}

@Composable
private fun Scoreboard(players: List<Player>, activePlayerId: java.util.UUID?, answeringPlayerId: java.util.UUID?, compact: Boolean) {
    Card(
        backgroundColor = Color(0xFF274651),
        shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
    ) {
        Column(modifier = Modifier.padding(if (compact) 10.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Scoreboard", fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 20.sp, color = Color.White)
            if (players.isEmpty()) {
                Text("No players", color = Color(0xFFD7DEE2))
            } else {
                players.sortedByDescending { it.score }.forEach { player ->
                    val marker = when (player.id) {
                        answeringPlayerId -> "Answering"
                        activePlayerId -> "Chooser"
                        else -> null
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (marker == null) player.name else "${player.name} [$marker]",
                            fontSize = if (compact) 12.sp else 18.sp,
                            color = Color.White,
                        )
                        Text("${player.score}", fontSize = if (compact) 12.sp else 18.sp, color = Color(0xFFE7C98B))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionBoard(
    themes: List<BoardThemeState>,
    enabled: Boolean,
    onQuestionClick: (java.util.UUID) -> Unit,
) {
    if (themes.isEmpty()) {
        Text("No questions available.", color = MaterialTheme.colors.onSurface)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        themes.forEach { theme ->
            Card(
                backgroundColor = Color(0xFFE9DDBE),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(theme.name, fontWeight = FontWeight.Bold, color = Color(0xFF2B2B2B))
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        theme.questions.forEach { question ->
                            val questionEnabled = enabled && !question.played
                            Button(
                                onClick = { onQuestionClick(question.id) },
                                enabled = questionEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (question.played) Color(0xFFB3AA9E) else Color(0xFF235A73),
                                    contentColor = Color.White,
                                    disabledBackgroundColor = if (question.played) Color(0xFF8B8378) else Color(0xFF235A73),
                                ),
                            ) {
                                Text(question.price.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerChipRow(
    players: List<Player>,
    activePlayerId: java.util.UUID?,
    onClick: (java.util.UUID) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        players.forEach { player ->
            Button(
                onClick = { onClick(player.id) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (player.id == activePlayerId) Color(0xFFE8B23A) else Color(0xFF235A73),
                    contentColor = if (player.id == activePlayerId) Color(0xFF1B1B1B) else Color.White,
                ),
            ) {
                Text(player.name)
            }
        }
    }
}

@Composable
private fun PlayerEditorRow(
    player: Player,
    renameValue: String,
    onRenameChange: (String) -> Unit,
    onRenameCommit: () -> Unit,
    onRemove: () -> Unit,
    renameEnabled: Boolean,
    scoreDelta: String,
    onScoreChange: (String) -> Unit,
    onAdjustScore: () -> Unit,
) {
    Card(backgroundColor = Color.White, shape = RoundedCornerShape(16.dp), elevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${player.name}  ${player.score}", fontWeight = FontWeight.Bold)
                Button(
                    onClick = onRemove,
                    enabled = renameEnabled,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD85852), contentColor = Color.White),
                ) {
                    Text("Remove")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = onRenameChange,
                    label = { Text("Rename") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = renameEnabled,
                )
                Button(onClick = onRenameCommit, enabled = renameEnabled) {
                    Text("Save")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = scoreDelta,
                    onValueChange = onScoreChange,
                    label = { Text("Score delta") },
                    modifier = Modifier.width(140.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Button(onClick = onAdjustScore) {
                    Text("Apply")
                }
                Text("Use negative values to subtract.")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
