package app.ui.media

import androidx.compose.foundation.Image
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

private const val MIN_FRAME_DURATION_MILLIS = 20L
private const val MAX_DECODED_GIF_BYTES = 64L * 1024L * 1024L

internal class GifAnimation(
    val frames: List<Frame>,
    val repetitionCount: Int,
) : AutoCloseable {
    internal class Frame(
        val image: ImageBitmap,
        val durationMillis: Long,
        internal val bitmap: Bitmap,
    )

    override fun close() {
        frames.forEach { it.bitmap.close() }
    }
}

internal fun decodeGif(bytes: ByteArray): GifAnimation {
    require(bytes.size >= 6 && bytes.copyOfRange(0, 6).decodeToString() in setOf("GIF87a", "GIF89a")) {
        "Content is not a GIF image"
    }

    return Data.makeFromBytes(bytes).use { data ->
        Codec.makeFromData(data).use { codec ->
            val frameCount = codec.frameCount
            require(frameCount > 0) { "GIF contains no frames" }

            val decodedBytes = codec.width.toLong() * codec.height.toLong() * 4L * frameCount.toLong()
            require(decodedBytes <= MAX_DECODED_GIF_BYTES) {
                "Decoded GIF exceeds the ${MAX_DECODED_GIF_BYTES / 1024L / 1024L} MB safety limit"
            }

            val frames = mutableListOf<GifAnimation.Frame>()
            try {
                repeat(frameCount) { index ->
                    val bitmap = Bitmap()
                    try {
                        check(bitmap.allocPixels(codec.imageInfo)) { "Could not allocate GIF frame" }
                        codec.readPixels(bitmap, index, -1)
                        bitmap.setImmutable()
                        frames += GifAnimation.Frame(
                            image = bitmap.asComposeImageBitmap(),
                            durationMillis = codec.getFrameInfo(index).duration
                                .toLong()
                                .coerceAtLeast(MIN_FRAME_DURATION_MILLIS),
                            bitmap = bitmap,
                        )
                    } catch (error: Throwable) {
                        bitmap.close()
                        throw error
                    }
                }
                GifAnimation(frames, codec.repetitionCount)
            } catch (error: Throwable) {
                frames.forEach { it.bitmap.close() }
                throw error
            }
        }
    }
}

@Composable
internal fun AnimatedGif(
    sourceKey: Any,
    loadBytes: () -> ByteArray,
    errorText: String,
    bodySize: TextUnit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    paused: Boolean = false,
) {
    val animation = remember(sourceKey) {
        runCatching { decodeGif(loadBytes()) }.getOrNull()
    }

    DisposableEffect(animation) {
        onDispose { animation?.close() }
    }

    if (animation == null) {
        Text(errorText, color = Color.Red, fontSize = bodySize)
        return
    }

    var frameIndex by remember(animation) { mutableStateOf(0) }
    var completedRepetitions by remember(animation) { mutableStateOf(0) }

    LaunchedEffect(animation, paused) {
        if (paused) return@LaunchedEffect

        while (isActive) {
            delay(animation.frames[frameIndex].durationMillis)
            val isLastFrame = frameIndex == animation.frames.lastIndex
            if (!isLastFrame) {
                frameIndex += 1
                continue
            }

            if (animation.repetitionCount >= 0 && completedRepetitions >= animation.repetitionCount) {
                return@LaunchedEffect
            }

            completedRepetitions += 1
            frameIndex = 0
        }
    }

    Image(
        bitmap = animation.frames[frameIndex].image,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}
