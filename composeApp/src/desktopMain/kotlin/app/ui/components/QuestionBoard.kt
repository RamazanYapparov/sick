package app.ui.components

import app.ui.theme.Palette
import app.state.BoardThemeState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
internal fun QuestionBoard(
    themes: List<BoardThemeState>,
    enabled: Boolean,
    onQuestionClick: (UUID) -> Unit,
    showCompleted: Boolean = false,
    onShowCompletedToggle: (() -> Unit)? = null,
    fillHeight: Boolean = false,
) {
    if (themes.isEmpty()) {
        Text("No questions available.", color = MaterialTheme.colors.onSurface)
        return
    }

    val completedThemes = themes.filter { theme -> theme.questions.all { it.played } }
    val activeThemes = themes.filter { theme -> !theme.questions.all { it.played } }
    val visibleThemes = if (showCompleted) themes else activeThemes

    Column(
        modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (completedThemes.isNotEmpty() && onShowCompletedToggle != null) {
            Button(
                onClick = onShowCompletedToggle,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB3AA9E),
                    contentColor = Color.White,
                ),
            ) {
                Text(if (showCompleted) "Hide completed" else "Show completed")
            }
        }
        visibleThemes.forEach { theme ->
            Card(
                modifier = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFE9DDBE),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = if (fillHeight)
                        Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)
                    else
                        Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = if (fillHeight) Arrangement.SpaceBetween else Arrangement.Top,
                ) {
                    Text(
                        theme.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B2B2B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!fillHeight) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        theme.questions.forEach { question ->
                            val questionEnabled = enabled && !question.played
                            Button(
                                modifier = Modifier.weight(1f),
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
