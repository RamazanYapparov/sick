package app.ui.components

import app.ui.theme.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Player
import java.util.UUID

@Composable
internal fun Scoreboard(
    players: List<Player>,
    activePlayerId: UUID?,
    answeringPlayerId: UUID?,
    compact: Boolean,
) {
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
                        Text("${player.score}", fontSize = if (compact) 12.sp else 18.sp, color = Palette.AccentGold)
                    }
                }
            }
        }
    }
}
