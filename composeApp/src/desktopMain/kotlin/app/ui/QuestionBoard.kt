@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui

import app.state.BoardThemeState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
internal fun QuestionBoard(
    themes: List<BoardThemeState>,
    enabled: Boolean,
    onQuestionClick: (UUID) -> Unit,
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
                                    backgroundColor = if (question.played) Color(0xFFB3AA9E) else Palette.AccentBlue,
                                    contentColor = Color.White,
                                    disabledBackgroundColor = if (question.played) Color(0xFF8B8378) else Palette.AccentBlue,
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
