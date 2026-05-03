package app.ui.components

import app.ui.theme.Palette
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Player
import java.util.UUID

@Composable
internal fun PlayerCards(
    players: List<Player>,
    activePlayerId: UUID?,
    answeringPlayerId: UUID?,
    skipVotePlayerIds: Set<UUID>,
    compact: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        players.forEach { player ->
            val isAnswering = player.id == answeringPlayerId
            val isSkipping = player.id in skipVotePlayerIds
            val isChoosing = player.id == activePlayerId

            val cardBg = when {
                isAnswering -> Color(0xFF1E4D2B)
                isSkipping -> Color(0xFF555555)
                else -> Palette.DarkSurface
            }
            val nameColor = when {
                isAnswering -> Color(0xFF5CCD8F)
                isChoosing -> Palette.AccentGold
                else -> Color.White
            }
            val nameFontWeight = if (isChoosing) FontWeight.Bold else FontWeight.Normal
            val scoreColor = if (isAnswering) Color(0xFF5CCD8F) else Palette.AccentGold
            val textSize = if (compact) 12.sp else 16.sp

            Card(
                backgroundColor = cardBg,
                shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(player.name, fontSize = textSize, color = nameColor, fontWeight = nameFontWeight)
                    Text("${player.score}", fontSize = textSize, color = scoreColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
