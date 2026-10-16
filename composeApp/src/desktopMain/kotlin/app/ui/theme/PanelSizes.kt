package app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sizes of the panels both windows share: small on the host preview, large on the Shared Display. */
data class PanelSizes(
    val corner: Dp,
    val padding: Dp,
    val gap: Dp,
    val title: TextUnit,
    val body: TextUnit,
    val timer: TextUnit,
    val optionGap: Dp,
    val optionLetterCorner: Dp,
    val optionLetterPaddingH: Dp,
    val optionLetterPaddingV: Dp,
    val videoHeight: Dp,
    val audioBarHeight: Dp,
    val audioLabel: TextUnit,
    val audioTime: TextUnit,
    val audioPaddingH: Dp,
) {
    companion object {
        val Host = PanelSizes(
            corner = 16.dp,
            padding = 12.dp,
            gap = 8.dp,
            title = 16.sp,
            body = 12.sp,
            timer = 24.sp,
            optionGap = 6.dp,
            optionLetterCorner = 4.dp,
            optionLetterPaddingH = 6.dp,
            optionLetterPaddingV = 2.dp,
            videoHeight = 200.dp,
            audioBarHeight = 48.dp,
            audioLabel = 14.sp,
            audioTime = 11.sp,
            audioPaddingH = 10.dp,
        )

        val SharedDisplay = PanelSizes(
            corner = 24.dp,
            padding = 24.dp,
            gap = 14.dp,
            title = 26.sp,
            body = 22.sp,
            timer = 46.sp,
            optionGap = 12.dp,
            optionLetterCorner = 6.dp,
            optionLetterPaddingH = 10.dp,
            optionLetterPaddingV = 4.dp,
            videoHeight = 360.dp,
            audioBarHeight = 72.dp,
            audioLabel = 20.sp,
            audioTime = 13.sp,
            audioPaddingH = 16.dp,
        )
    }
}

/** Provided once per window: [PanelSizes.Host] or [PanelSizes.SharedDisplay]. */
val LocalPanelSizes = staticCompositionLocalOf<PanelSizes> { error("No PanelSizes provided") }
