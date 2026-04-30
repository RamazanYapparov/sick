@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui.components

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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
import com.sick.model.Player
import java.util.UUID

@Composable
internal fun PlayerChipRow(
    players: List<Player>,
    activePlayerId: UUID?,
    onClick: (UUID) -> Unit,
    enabled: Boolean = true,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        players.forEach { player ->
            Button(
                onClick = { onClick(player.id) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (player.id == activePlayerId) Palette.AccentYellow else Palette.AccentBlue,
                    contentColor = if (player.id == activePlayerId) Color(0xFF1B1B1B) else Color.White,
                ),
            ) {
                Text(player.name)
            }
        }
    }
}

@Composable
internal fun PlayerEditorRow(
    player: Player,
    scoreDelta: String,
    onScoreChange: (String) -> Unit,
    onAdjustScore: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(backgroundColor = Color.White, shape = RoundedCornerShape(16.dp), elevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${player.name}  ${player.score}", fontWeight = FontWeight.Bold)
                    Button(onClick = { expanded = true }) {
                        Text("Edit")
                    }
                }
            } else {
                Text("${player.name}  ${player.score}", fontWeight = FontWeight.Bold)
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
