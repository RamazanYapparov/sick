@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui.components

import app.state.PlayerChoice
import app.state.PlayerView
import app.state.formatReactionTime
import app.ui.theme.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
internal fun PlayerChipRow(
    choices: List<PlayerChoice>,
    onClick: (UUID) -> Unit,
    enabled: Boolean = true,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            Button(
                onClick = { onClick(choice.id) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (choice.highlighted) Palette.AccentYellow else Palette.AccentBlue,
                    contentColor = if (choice.highlighted) Palette.ActivePlayerText else Color.White,
                ),
            ) {
                Text(choice.name)
            }
        }
    }
}

/** The host's row for one player: the background shows the player's role in the question, the title marks the Chooser. */
@Composable
internal fun PlayerEditorRow(
    player: PlayerView,
    scoreDelta: String,
    onScoreChange: (String) -> Unit,
    onAdjustScore: (Int) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${player.name}?") },
            text = { Text("Their score (${player.score}) will be lost.") },
            confirmButton = {
                Button(onClick = { confirmRemove = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            },
        )
    }
    Card(backgroundColor = Palette.roleBackground(player.role), shape = RoundedCornerShape(16.dp), elevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerTitle(player)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { expanded = true }) {
                            Text("Edit")
                        }
                        OutlinedButton(onClick = { confirmRemove = true }, enabled = canRemove) {
                            Text("Remove")
                        }
                    }
                }
            } else {
                PlayerTitle(player)
                val buttonEnabled = scoreDelta.isNotBlank() && scoreDelta != "0"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = scoreDelta,
                        onValueChange = { onScoreChange(it.filter(Char::isDigit)) },
                        label = { Text("Score delta") },
                        modifier = Modifier.width(140.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Button(
                        onClick = {
                            onAdjustScore(scoreDelta.toInt())
                            expanded = false
                        },
                        enabled = buttonEnabled,
                    ) {
                        Text("+")
                    }
                    Button(
                        onClick = {
                            onAdjustScore(-scoreDelta.toInt())
                            expanded = false
                        },
                        enabled = buttonEnabled,
                    ) {
                        Text("−")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTitle(player: PlayerView) {
    val textColor = Palette.roleText(player.role) ?: Color.Unspecified
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${player.name}  ${player.score}", fontWeight = FontWeight.Bold, color = textColor)
        if (player.isChooser) Text("Chooser", color = Palette.AccentGold, fontWeight = FontWeight.Bold)
        player.reactionMillis?.let { Text(formatReactionTime(it), color = Palette.SecondaryText) }
    }
}
