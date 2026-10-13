package app.ui.window

import app.state.AudioPlaybackState
import app.state.DesktopUiState
import app.state.QuestionDisplayItem
import app.state.coordinatesLabel
import app.state.displayContents
import app.state.isImage
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer
import com.sick.model.Question
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
    question: Question<*>,
    basePath: Path?,
    compact: Boolean,
    bodySize: TextUnit,
    audioPlayback: AudioPlaybackState,
    onMediaFinished: () -> Unit = {},
    mediaStopSignal: Int = 0,
    mediaPaused: Boolean = false,
) {
    val answer = question.answer
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
                is Answer.Point -> {
                    val image = remember(question, basePath) {
                        question.displayContents(basePath).firstOrNull { it.isImage() }
                    }
                    if (image != null) {
                        RenderQuestionDisplayItem(
                            item = image,
                            compact = compact,
                            bodySize = bodySize,
                            audioPlayback = audioPlayback,
                            point = answer,
                        )
                    } else {
                        Text(
                            "Point: ${answer.coordinatesLabel()} (question has no image)",
                            fontSize = bodySize,
                            color = Palette.PlayerAnsweringText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * [point] marks a [Answer.Point] on image items; other item types ignore it.
 */
@Composable
internal fun RenderQuestionDisplayItem(
    item: QuestionDisplayItem,
    compact: Boolean,
    bodySize: TextUnit,
    audioPlayback: AudioPlaybackState,
    onMediaFinished: () -> Unit = {},
    mediaStopSignal: Int = 0,
    mediaPaused: Boolean = false,
    point: Answer.Point? = null,
) {
    when (item) {
        is QuestionDisplayItem.Text ->
            Text(
                item.text,
                fontSize = bodySize,
                lineHeight = bodySize * 1.4f,
                color = Color.White,
            )
        is QuestionDisplayItem.LocalImage -> {
            val bitmap = remember(item.absolutePath) {
                runCatching {
                    java.io.File(item.absolutePath).inputStream().buffered()
                        .use(::loadImageBitmap)
                }.getOrNull()
            }
            if (bitmap != null)
                ImageItem(bitmap, point)
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
                ImageItem(bitmap, point)
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
private fun ImageItem(bitmap: ImageBitmap, point: Answer.Point?) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        // Always render the full image (no crop, no stretch).
        // `Fit` scales the bitmap to fit within the available
        // bounds while maintaining aspect ratio, so the entire
        // image is always visible (letterboxed if needed).
        modifier = Modifier.fillMaxWidth().then(
            if (point != null) Modifier.drawWithContent {
                drawContent()
                drawAnswerPoint(point, bitmap.width.toFloat(), bitmap.height.toFloat())
            } else Modifier
        ),
        contentScale = ContentScale.Fit,
    )
}

/** The marker stroke is this fraction of the image's shorter side, but at least [MIN_POINT_STROKE_PX]. */
private const val POINT_STROKE_SHARE = 1f / 150f
private const val MIN_POINT_STROKE_PX = 2f
private const val POINT_DOT_RADIUS_IN_STROKES = 2.5f
private const val POINT_AREA_ALPHA = 0.2f

/**
 * Draws [point] over a bitmap of [bitmapWidth] x [bitmapHeight] that
 * `ContentScale.Fit` centred in this draw scope. The accepted area is a
 * circle of `deviation` times the image's shorter side, as SIQuester draws it.
 */
private fun DrawScope.drawAnswerPoint(point: Answer.Point, bitmapWidth: Float, bitmapHeight: Float) {
    val scale = minOf(size.width / bitmapWidth, size.height / bitmapHeight)
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val center = Offset(
        x = (size.width - width) / 2 + point.x.toFloat() * width,
        y = (size.height - height) / 2 + point.y.toFloat() * height,
    )
    val radius = point.deviation.toFloat() * minOf(width, height)
    val stroke = maxOf(MIN_POINT_STROKE_PX, minOf(width, height) * POINT_STROKE_SHARE)
    if (radius > 0f) {
        drawCircle(Palette.AnswerPoint.copy(alpha = POINT_AREA_ALPHA), radius, center)
        drawCircle(Palette.AnswerPoint, radius, center, style = Stroke(width = stroke))
    }
    drawCircle(Palette.AnswerPoint, stroke * POINT_DOT_RADIUS_IN_STROKES, center)
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
