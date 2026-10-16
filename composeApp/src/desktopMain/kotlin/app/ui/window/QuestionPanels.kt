package app.ui.window

import app.session.MediaSlot
import app.state.AudioPlaybackState
import app.state.Panel
import app.state.QuestionDisplayItem
import app.state.ShownItem
import app.state.TimerView
import app.state.coordinatesLabel
import app.state.isImage
import app.ui.media.AudioPlayer
import app.ui.media.AudioProgress
import app.ui.media.VideoPlayer
import app.ui.media.normalizeMediaUri
import app.ui.theme.LocalPanelSizes
import app.ui.theme.Palette
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sick.model.Answer

/** Renders [panel] at the window's [LocalPanelSizes]; both windows show their panel through here. */
@Composable
internal fun PanelContent(
    panel: Panel,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
    when (panel) {
        Panel.NoPack -> NoPackPanel()
        is Panel.Lobby -> LobbyPanel(panel)
        is Panel.Board -> BoardOverview(panel.board)
        is Panel.Revealing -> RevealingPanel(panel)
        is Panel.Question -> QuestionPanel(panel, audioPlayback, onMediaEnded)
        is Panel.Answer -> AnswerPanel(panel, audioPlayback, onMediaEnded)
        Panel.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active question", color = Palette.MutedText)
        }
    }
}

/**
 * Shows only the theme and price while the question is revealed, so that
 * video/audio playback on the two windows starts at the same time when the
 * reveal finishes.
 */
@Composable
private fun RevealingPanel(panel: Panel.Revealing) {
    val sizes = LocalPanelSizes.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(sizes.corner),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(sizes.padding),
            verticalArrangement = Arrangement.spacedBy(sizes.gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(panel.theme, fontSize = sizes.title, fontWeight = FontWeight.Bold, color = Palette.AccentGold)
            Text("${panel.price} points", fontSize = sizes.body, color = Color.White)
        }
    }
}

@Composable
private fun QuestionPanel(
    panel: Panel.Question,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
    val sizes = LocalPanelSizes.current
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(sizes.corner),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(sizes.padding),
            verticalArrangement = Arrangement.spacedBy(sizes.gap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(panel.theme, fontSize = sizes.title, fontWeight = FontWeight.Bold, color = Palette.AccentGold)
                    Text("${panel.price} points", fontSize = sizes.body, color = Color.White)
                }
                panel.timer?.let { TimerText(it) }
            }

            Divider(color = Palette.DividerColor)

            QuestionContent(
                items = panel.items,
                selectOptions = panel.options,
                revealCorrect = panel.revealCorrectOption,
                fitsText = panel.fitsText,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { shown, size ->
                RenderShownItem(shown, size, panel.ownsSound, audioPlayback, onMediaEnded)
            }
        }
    }
}

@Composable
private fun TimerText(timer: TimerView) {
    val sizes = LocalPanelSizes.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${timer.seconds}", fontSize = sizes.timer, fontWeight = FontWeight.Bold, color = Palette.TimerColor)
        if (timer.paused) {
            Text("Paused", fontSize = sizes.body, fontWeight = FontWeight.Bold, color = Palette.TimerColor)
        }
    }
}

@Composable
private fun AnswerPanel(
    panel: Panel.Answer,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
    val sizes = LocalPanelSizes.current
    val bodySize = sizes.body
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(sizes.corner),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(sizes.padding),
            verticalArrangement = Arrangement.spacedBy(sizes.gap),
        ) {
            Text("Answer", fontSize = sizes.title, fontWeight = FontWeight.Bold, color = Palette.AccentGold)
            Divider(color = Palette.DividerColor)
            when (val answer = panel.answer) {
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
                    if (panel.items.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Palette.DividerColor)
                        panel.items.forEach { shown ->
                            RenderShownItem(shown, bodySize, panel.ownsSound, audioPlayback, onMediaEnded)
                        }
                    }
                }
                is Answer.Select -> SelectOptionsList(answer.options, bodySize, revealCorrect = true)
                is Answer.Point -> {
                    if (panel.items.isEmpty()) {
                        Text(
                            "Point: ${answer.coordinatesLabel()} (question has no image)",
                            fontSize = bodySize,
                            color = Palette.PlayerAnsweringText,
                        )
                    }
                    panel.items.forEach { shown ->
                        RenderShownItem(shown, bodySize, panel.ownsSound, audioPlayback, onMediaEnded)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectOptionsList(
    options: List<Answer.Select.Option>,
    bodySize: TextUnit,
    revealCorrect: Boolean,
) {
    val sizes = LocalPanelSizes.current
    val effectiveBodySize = if (options.size >= 6) bodySize * 0.9f else bodySize
    val letterSize = bodySize * 1.1f
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val highlight = revealCorrect && option.correct
            val confirmed = revealCorrect && !option.correct
            val rowBg = if (highlight) Palette.PlayerAnswering else Palette.SelectRowBg
            val textColor = when {
                highlight -> Palette.PlayerAnsweringText
                confirmed -> Palette.ConfirmedText
                else -> Color.White
            }
            val letterBg = if (highlight) Palette.PlayerAnswering else Palette.SelectLetterBg
            val letterColor = when {
                highlight -> Palette.PlayerAnsweringText
                confirmed -> Palette.ConfirmedText
                else -> Palette.AccentGold
            }
            val textFontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(sizes.optionGap),
                ) {
                    Box(
                        modifier = Modifier
                            .background(letterBg, RoundedCornerShape(sizes.optionLetterCorner))
                            .padding(horizontal = sizes.optionLetterPaddingH, vertical = sizes.optionLetterPaddingV),
                    ) {
                        Text(option.name, fontSize = letterSize, color = letterColor, fontWeight = FontWeight.Bold)
                    }
                    Text(option.answer, fontSize = effectiveBodySize, color = textColor, fontWeight = textFontWeight)
                }
            }
        }
    }
}

private const val MIN_QUESTION_FONT_SP = 18
private const val MAX_QUESTION_FONT_SP = 72

/** Share of the content height all images together may take when text is also shown. */
private const val IMAGE_SHARE_WITH_TEXT = 0.5f

/**
 * Stacks question [items] in pack order, followed by [selectOptions].
 *
 * With [fitsText] the text size is the largest one in
 * [MIN_QUESTION_FONT_SP]..[MAX_QUESTION_FONT_SP] at which every text item and
 * the options, measured with their real composables, fit beside the media.
 * Otherwise the text keeps the window's body size. Media items are composed
 * once and never probed, so players are not duplicated.
 */
@Composable
private fun QuestionContent(
    items: List<ShownItem>,
    selectOptions: List<Answer.Select.Option>?,
    revealCorrect: Boolean,
    fitsText: Boolean,
    modifier: Modifier,
    renderItem: @Composable (ShownItem, TextUnit) -> Unit,
) {
    val sizes = LocalPanelSizes.current
    val fixedBodySize = sizes.body
    val gap = sizes.gap
    val hasTextBlock = selectOptions != null || items.any { it.item is QuestionDisplayItem.Text }
    val imageCount = items.count { it.item.isImage() }

    @Composable
    fun TextBlock(size: TextUnit) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.forEach { if (it.item is QuestionDisplayItem.Text) renderItem(it, size) }
            if (selectOptions != null) {
                SelectOptionsList(selectOptions, size, revealCorrect)
            }
        }
    }

    SubcomposeLayout(modifier) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gapPx = gap.roundToPx()
        val imageCap = if (imageCount == 0) height
        else (if (hasTextBlock) (height * IMAGE_SHARE_WITH_TEXT).toInt() else height) / imageCount

        val media = items.withIndex()
            .filter { it.value.item !is QuestionDisplayItem.Text }
            .associate { (index, shown) ->
                val maxHeight = if (shown.item.isImage()) imageCap else height
                index to subcompose("media" to index) { renderItem(shown, fixedBodySize) }
                    .map { it.measure(Constraints(maxWidth = width, maxHeight = maxHeight)) }
            }
        val mediaHeight = media.values.sumOf { slot -> slot.sumOf { it.height } } + gapPx * media.values.count { it.isNotEmpty() }

        val probes = mutableMapOf<Int, Int>()
        fun fits(sp: Int): Boolean = probes.getOrPut(sp) {
            subcompose("probe" to sp) { TextBlock(sp.sp) }
                .sumOf { it.measure(Constraints(maxWidth = width)).height }
        } <= height - mediaHeight

        val fontSize = if (!fitsText || !hasTextBlock) {
            fixedBodySize
        } else {
            var lo = MIN_QUESTION_FONT_SP
            var hi = MAX_QUESTION_FONT_SP
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (fits(mid)) lo = mid else hi = mid - 1
            }
            lo.sp
        }

        val slots = items.indices.map { index ->
            media[index] ?: subcompose("text" to index) { renderItem(items[index], fontSize) }
                .map { it.measure(Constraints(maxWidth = width)) }
        } + listOfNotNull(selectOptions?.let { options ->
            subcompose("options") { SelectOptionsList(options, fontSize, revealCorrect) }
                .map { it.measure(Constraints(maxWidth = width)) }
        })

        layout(width, height) {
            var y = 0
            slots.filter { it.isNotEmpty() }.forEachIndexed { i, slot ->
                if (i > 0) y += gapPx
                slot.forEach { placeable ->
                    placeable.place(0, y)
                    y += placeable.height
                }
            }
        }
    }
}

/**
 * Renders one item. An image marks the item's Point Answer if it has one. A
 * video or audio item shows only while it is the current media; its player is
 * keyed on the slot, so a new generation (a replay) starts a fresh one.
 */
@Composable
private fun RenderShownItem(
    shown: ShownItem,
    bodySize: TextUnit,
    ownsSound: Boolean,
    audioPlayback: AudioPlaybackState,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
) {
    when (val item = shown.item) {
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
                ImageItem(bitmap, shown.point)
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
                ImageItem(bitmap, shown.point)
            else
                Text("Image unavailable: ${item.url}", color = Color.Red, fontSize = bodySize)
        }
        is QuestionDisplayItem.LocalVideo -> MediaItem(shown.media, onMediaEnded) { paused, onFinished ->
            val uri = remember(item.absolutePath) { normalizeMediaUri(item.absolutePath) }
            QuestionVideo(uri, ownsSound, paused, onFinished)
        }
        is QuestionDisplayItem.RemoteVideo -> MediaItem(shown.media, onMediaEnded) { paused, onFinished ->
            QuestionVideo(item.url.toString(), ownsSound, paused, onFinished)
        }
        is QuestionDisplayItem.LocalAudio -> MediaItem(shown.media, onMediaEnded) { paused, onFinished ->
            AudioItem(item.absolutePath, ownsSound, audioPlayback, paused, onFinished)
        }
        is QuestionDisplayItem.RemoteAudio -> MediaItem(shown.media, onMediaEnded) { paused, onFinished ->
            AudioItem(item.url.toString(), ownsSound, audioPlayback, paused, onFinished)
        }
    }
}

/** Composes [player] only while [media] is set, keyed on its generation and index. */
@Composable
private fun MediaItem(
    media: MediaSlot?,
    onMediaEnded: (generation: Int, index: Int) -> Unit,
    player: @Composable (paused: Boolean, onFinished: () -> Unit) -> Unit,
) {
    if (media == null) return
    key(media.generation, media.index) {
        player(media.paused) { onMediaEnded(media.generation, media.index) }
    }
}

/** Without [ownsSound] the video plays muted, so its sound comes only from the shared display. */
@Composable
private fun QuestionVideo(uri: String, ownsSound: Boolean, paused: Boolean, onFinished: () -> Unit) {
    VideoPlayer(
        uri = uri,
        modifier = Modifier.fillMaxWidth().height(LocalPanelSizes.current.videoHeight),
        paused = paused,
        muted = !ownsSound,
        onFinished = onFinished,
    )
}

/**
 * With [ownsSound] this window runs the single audio player; otherwise it only
 * mirrors that player's progress, so the clip is never heard twice.
 */
@Composable
private fun AudioItem(
    uri: String,
    ownsSound: Boolean,
    audioPlayback: AudioPlaybackState,
    paused: Boolean,
    onFinished: () -> Unit,
) {
    if (ownsSound) {
        AudioPlayer(uri = uri, playback = audioPlayback, paused = paused, onFinished = onFinished)
    } else {
        AudioProgress(uri = uri, playback = audioPlayback)
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
