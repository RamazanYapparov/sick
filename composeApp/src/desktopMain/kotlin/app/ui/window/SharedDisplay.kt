package app.ui.window

import app.state.AudioPlaybackState
import app.state.BoardView
import app.state.SharedDisplayView
import app.state.Panel
import app.ui.components.PlayerCards
import app.ui.components.QrCode
import app.ui.components.QuestionBoard
import app.ui.theme.Palette
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SharedDisplayScreen(
    view: SharedDisplayView,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(view.header, fontSize = 22.sp, color = Palette.AccentGold)
            Box(modifier = Modifier.weight(1f)) {
                PanelContent(view.panel, audioPlayback, onMediaEnded)
            }
            PlayerCards(view.players)
        }
    }
}

@Composable
internal fun NoPackPanel() {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Load a pack to begin.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
internal fun LobbyPanel(panel: Panel.Lobby) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pack Loaded",
                    fontSize = 16.sp,
                    color = Palette.AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = panel.packName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Connect to Play",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Scan the QR code or enter the connection URL in your browser to join as a player.",
                    fontSize = 16.sp,
                    color = Palette.InfoText
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.ThemeBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = panel.joinUrl,
                        fontSize = 18.sp,
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
                        .padding(16.dp)
                ) {
                    QrCode(
                        text = panel.joinUrl,
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
internal fun BoardOverview(board: BoardView) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Board", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            QuestionBoard(board = board, fillHeight = true)
        }
    }
}
