package app.ui.components

import app.ui.theme.Palette
import app.state.BoardView
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID

/** A board without [onQuestionClick] is read-only; the completed-themes toggle shows only with [onShowCompletedToggle]. */
@Composable
internal fun QuestionBoard(
    board: BoardView,
    onQuestionClick: ((UUID) -> Unit)? = null,
    onShowCompletedToggle: (() -> Unit)? = null,
    fillHeight: Boolean = false,
) {
    Column(
        modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (board.hasCompletedThemes && onShowCompletedToggle != null) {
            Button(
                onClick = onShowCompletedToggle,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Palette.QuestionButtonColor,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (board.showCompleted) "Hide completed" else "Show completed")
            }
        }
        board.themes.forEach { theme ->
            Card(
                modifier = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
                backgroundColor = Palette.ThemeCardBg,
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
                        color = Palette.ThemeNameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!fillHeight) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        theme.questions.forEach { question ->
                            val questionEnabled = onQuestionClick != null && !question.played
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onQuestionClick?.invoke(question.id) },
                                enabled = questionEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (question.played) Palette.QuestionButtonColor else Palette.AccentBlue,
                                    contentColor = Color.White,
                                    disabledBackgroundColor = if (question.played) Palette.QuestionButtonDisabled else Palette.AccentBlue,
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
