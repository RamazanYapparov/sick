package app.ui.window

import app.state.AudioPlaybackState
import app.state.DesktopUiState
import app.state.QuestionDisplayItem
import app.state.displayContents
import app.ui.media.AudioPlayer
import app.ui.media.AudioProgress
import app.ui.media.VideoPlayer
import app.ui.media.normalizeMediaUri
import app.ui.theme.Palette
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer
import java.nio.file.Path

/**
 * Renders the brief placeholder shown on both the [SharedDisplayScreen] and
 * the host window during the [com.sick.state.GamePhase.RevealingQuestion]
 * phase. It deliberately shows no media so that video/audio playback on the
 * two windows starts at the same time when the reveal finishes.
 */
@Composable
internal fun RevealingQuestionPlaceholder(state: DesktopUiState, compact: Boolean, bodySize: TextUnit) {
    val question = state.currentQuestion ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                state.currentThemeName ?: "Question",
                fontSize = if (compact) 16.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.AccentGold,
            )
            Text("${question.price} points", fontSize = bodySize, color = Color.White)
        }
    }
}

@Composable
internal fun AnswerPanel(
    answer: Answer,
    basePath: Path?,
    compact: Boolean,
    bodySize: TextUnit,
    audioPlayback: AudioPlaybackState,
    onMediaFinished: () -> Unit = {},
    mediaStopSignal: Int = 0,
    mediaPaused: Boolean = false,
) {
    @Suppress("UNUSED_LOCAL_VARIABLE") val reservedForFuture = basePath
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            Text(
                "Answer",
                fontSize = if (compact) 16.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.AccentGold,
            )
            Divider(color = Palette.DividerColor)
            when (answer) {
                is Answer.Simple -> {
                    answer.right.forEach { right ->
                        Text(right, fontSize = bodySize, color = Palette.PlayerAnsweringText, fontWeight = FontWeight.Bold)
                    }
                    if (answer.wrong.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Also accepted:", fontSize = bodySize, color = Color.White)
                        answer.wrong.forEach { wrong ->
                            Text(wrong, fontSize = bodySize, color = Palette.ConfirmedText)
                        }
                    }
                    if (answer.contents.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Palette.DividerColor)
                        displayContents(answer.contents, basePath).forEach { item ->
                            RenderQuestionDisplayItem(
                                item = item,
                                compact = compact,
                                bodySize = bodySize,
                                audioPlayback = audioPlayback,
                                onMediaFinished = onMediaFinished,
                                mediaStopSignal = mediaStopSignal,
                                mediaPaused = mediaPaused,
                            )
                        }
                    }
                }
                is Answer.Select -> {
                    SelectOptionsList(
                        options = answer.options,
                        basePath = basePath,
                        compact = compact,
                        bodySize = bodySize,
                        revealCorrect = true,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RenderQuestionDisplayItem(
    item: QuestionDisplayItem,
    compact: Boolean,
    bodySize: TextUnit,
    audioPlayback: AudioPlaybackState,
    onMediaFinished: () -> Unit = {},
    mediaStopSignal: Int = 0,
    mediaPaused: Boolean = false,
) {
    when (item) {
        is QuestionDisplayItem.Text ->
            Text(item.text, fontSize = bodySize, color = Color.White)
        is QuestionDisplayItem.LocalImage -> {
            val bitmap = remember(item.absolutePath) {
                runCatching {
                    java.io.File(item.absolutePath).inputStream().buffered()
                        .use(::loadImageBitmap)
                }.getOrNull()
            }
            if (bitmap != null)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    // Always render the full image (no crop, no stretch).
                    // `Fit` scales the bitmap to fit within the available
                    // bounds while maintaining aspect ratio, so the entire
                    // image is always visible (letterboxed if needed).
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            else
                Text("Image not found: ${item.absolutePath}", color = Color.Red, fontSize = bodySize)
        }
        is QuestionDisplayItem.RemoteImage -> {
            val bitmap = remember(item.url) {
                runCatching {
                    item.url.openStream().buffered().use(::loadImageBitmap)
                }.getOrNull()
            }
            if (bitmap != null)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    // Always render the full image (no crop, no stretch).
                    // `Fit` scales the bitmap to fit within the available
                    // bounds while maintaining aspect ratio, so the entire
                    // image is always visible (letterboxed if needed).
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            else
                Text("Image unavailable: ${item.url}", color = Color.Red, fontSize = bodySize)
        }
        is QuestionDisplayItem.LocalVideo -> {
            if (compact) {
                // Compact (host) view: video plays but is muted so audio
                // stays unique to the shared display window.
                val uri = remember(item.absolutePath) { normalizeMediaUri(item.absolutePath) }
                VideoPlayer(
                    uri = uri,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    stopSignal = mediaStopSignal,
                    paused = mediaPaused,
                    muted = true,
                    onFinished = onMediaFinished,
                )
            } else {
                val uri = remember(item.absolutePath) { normalizeMediaUri(item.absolutePath) }
                VideoPlayer(
                    uri = uri,
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    stopSignal = mediaStopSignal,
                    paused = mediaPaused,
                    onFinished = onMediaFinished,
                )
            }
        }
        is QuestionDisplayItem.RemoteVideo -> {
            if (compact) {
                VideoPlayer(
                    uri = item.url.toString(),
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    stopSignal = mediaStopSignal,
                    paused = mediaPaused,
                    muted = true,
                    onFinished = onMediaFinished,
                )
            } else {
                VideoPlayer(
                    uri = item.url.toString(),
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    stopSignal = mediaStopSignal,
                    paused = mediaPaused,
                    onFinished = onMediaFinished,
                )
            }
        }
        is QuestionDisplayItem.LocalAudio -> AudioItem(
            uri = item.absolutePath,
            compact = compact,
            audioPlayback = audioPlayback,
            onMediaFinished = onMediaFinished,
            mediaStopSignal = mediaStopSignal,
            mediaPaused = mediaPaused,
        )
        is QuestionDisplayItem.RemoteAudio -> AudioItem(
            uri = item.url.toString(),
            compact = compact,
            audioPlayback = audioPlayback,
            onMediaFinished = onMediaFinished,
            mediaStopSignal = mediaStopSignal,
            mediaPaused = mediaPaused,
        )
    }
}

@Composable
private fun AudioItem(
    uri: String,
    compact: Boolean,
    audioPlayback: AudioPlaybackState,
    onMediaFinished: () -> Unit,
    mediaStopSignal: Int,
    mediaPaused: Boolean,
) {
    if (compact) {
        // Compact (host) view: progress only. The shared display window owns
        // the single audio player, so the clip is never heard twice.
        AudioProgress(uri = uri, playback = audioPlayback, compact = true)
    } else {
        AudioPlayer(
            uri = uri,
            playback = audioPlayback,
            stopSignal = mediaStopSignal,
            paused = mediaPaused,
            onFinished = onMediaFinished,
        )
    }
}
