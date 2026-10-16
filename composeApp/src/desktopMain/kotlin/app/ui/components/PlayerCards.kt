package app.ui.components

import app.state.PlayerView
import app.state.QuestionRole
import app.state.formatReactionTime
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

/** The Shared Display's score row: the card shows the player's role in the question, the name marks the Chooser. */
@Composable
internal fun PlayerCards(players: List<PlayerView>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        players.forEach { player ->
            val cardBg = Palette.roleBackground(player.role)
            val nameColor = Palette.roleText(player.role)
                ?: if (player.isChooser) Palette.AccentGold else Color.White
            val nameFontWeight = if (player.isChooser) FontWeight.Bold else FontWeight.Normal
            val scoreColor = if (player.role == QuestionRole.Answering) Palette.PlayerAnsweringText else Palette.AccentGold

            Card(
                backgroundColor = cardBg,
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(player.name, fontSize = 16.sp, color = nameColor, fontWeight = nameFontWeight)
                    Text("${player.score}", fontSize = 16.sp, color = scoreColor, fontWeight = FontWeight.Bold)
                    player.reactionMillis?.let { millis ->
                        Text(formatReactionTime(millis), fontSize = 13.sp, color = nameColor)
                    }
                }
            }
        }
    }
}
