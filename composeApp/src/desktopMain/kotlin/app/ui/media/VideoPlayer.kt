package app.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.ui.theme.Palette
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

private fun formatMillis(ms: Double): String {
    val total = (ms / 1000).toInt()
    val min = total / 60
    val sec = total % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

/**
 * Converts ARGB ints (as delivered by vlcj's RenderCallbackAdapter onDisplay)
 * into a ByteArray with RGBA byte order for Skia's RGBA_8888 pixel format.
 *
 * Input:  pixel[i] = Java int 0xAARRGGBB
 * Output: dest[4*i..4*i+3] = [R, G, B, A]
 */
private fun argbIntsToRgbaBytes(pixels: IntArray, dest: ByteArray) {
    var idx = 0
    for (pixel in pixels) {
        dest[idx] = ((pixel shr 16) and 0xFF).toByte()
        dest[idx + 1] = ((pixel shr 8) and 0xFF).toByte()
        dest[idx + 2] = (pixel and 0xFF).toByte()
        dest[idx + 3] = ((pixel shr 24) and 0xFF).toByte()
        idx += 4
    }
}

/**
 * Returns a platform-appropriate [VideoSurfaceAdapter] for the callback
 * video surface.  On Windows / macOS vlcj ships concrete adapters that
 * hook into the native video pipeline; on other platforms a no-op adapter
 * is used because the callback surface delivers pixels programmatically.
 */
private fun createVideoSurfaceAdapter(): VideoSurfaceAdapter {
    val os = System.getProperty("os.name", "").lowercase()
    return when {
        os.contains("win") ->
            uk.co.caprica.vlcj.player.embedded.videosurface.WindowsVideoSurfaceAdapter()
        os.contains("mac") || os.contains("darwin") ->
            uk.co.caprica.vlcj.player.embedded.videosurface.OsxVideoSurfaceAdapter()
        else ->
            object : VideoSurfaceAdapter {
                override fun attach(mediaPlayer: MediaPlayer, id: Long) {
                    // no-op: pixel data arrives through the callback
                }
            }
    }
}

@Composable
fun VideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    stopSignal: Int = 0,
    paused: Boolean = false,
    muted: Boolean = false,
    onFinished: () -> Unit = {},
) {
    val playerRef = remember { AtomicReference<EmbeddedMediaPlayer?>(null) }
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    val widthRef = remember { mutableStateOf(0) }
    val heightRef = remember { mutableStateOf(0) }
    var totalMs by remember { mutableStateOf(0.0) }
    var currentMs by remember { mutableStateOf(0.0) }
    val lastStopSignal = remember { mutableStateOf(stopSignal) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val normalizedUri = remember(uri) { normalizeMediaUri(uri) }

    DisposableEffect(normalizedUri) {
        errorMessage = null
        currentFrame = null
        widthRef.value = 0
        heightRef.value = 0
        totalMs = 0.0
        currentMs = 0.0

        val factory = VlcSupport.get()

        // Pre-allocate the render callback buffer — vlcj 4.x RenderCallbackAdapter
        // does NOT auto-initialise it, and display() will NPE if buffer is null.
        val initialBuffer = IntArray(1920 * 1080) // resized once format is known

        // reusable buffers to avoid per-frame allocations
        var byteBuffer: ByteArray? = null
        var skiaBitmap: org.jetbrains.skia.Bitmap? = null
        var prevW = 0
        var prevH = 0

        // --- render callback (receives pixel data for each frame) ---
        val renderCb = object : RenderCallbackAdapter(initialBuffer) {
            override fun onDisplay(mediaPlayer: MediaPlayer, pixels: IntArray) {
                try {
                    val w = widthRef.value
                    val h = heightRef.value
                    if (w <= 0 || h <= 0 || pixels.isEmpty()) return

                    val needed = w * h * 4
                    if (byteBuffer == null || byteBuffer!!.size < needed) {
                        byteBuffer = ByteArray(needed)
                    }
                    argbIntsToRgbaBytes(pixels, byteBuffer!!)

                    val info = ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
                    if (skiaBitmap == null || prevW != w || prevH != h) {
                        skiaBitmap?.close()
                        skiaBitmap = Bitmap().apply { allocPixels(info) }
                        prevW = w
                        prevH = h
                    }
                    skiaBitmap!!.installPixels(info, byteBuffer!!, w * 4)
                    @Suppress("DEPRECATION")
                    currentFrame = skiaBitmap!!.asImageBitmap()
                } catch (t: Throwable) {
                    logger.debug(t) { "VideoPlayer.onDisplay: frame dropped" }
                }
            }
        }

        // --- buffer-format callback (tells VLC what pixel format we want) ---
        val bufferFormatCb = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                widthRef.value = sourceWidth
                heightRef.value = sourceHeight
                val pitch = sourceWidth * 4
                // Resize the render callback buffer to match actual video dimensions
                renderCb.setBuffer(IntArray(sourceWidth * sourceHeight))
                return BufferFormat("RV32", sourceWidth, sourceHeight, intArrayOf(pitch), intArrayOf(sourceHeight))
            }

            override fun newFormatSize(width: Int, height: Int, pitches: Int, lines: Int) = Unit
            override fun allocatedBuffers(buffers: Array<ByteBuffer>) = Unit
        }

        // --- video surface (couples the callbacks to the media player) ---
        val videoSurface = CallbackVideoSurface(
            bufferFormatCb,
            renderCb,
            true,                  // attachOnStartOnly
            createVideoSurfaceAdapter(),
        )

        val mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer()
        mediaPlayer.videoSurface().set(videoSurface)
        playerRef.set(mediaPlayer)

        // --- event listener ---
        val listener = createMediaListener(
            tag = "VideoPlayer",
            normalizedUri = normalizedUri,
            onTotalMs = { totalMs = it },
            onCurrentMs = { currentMs = it },
            onErrorMsg = { errorMessage = it },
            onFinished = onFinished,
        )
        mediaPlayer.events().addMediaPlayerEventListener(listener)

        try {
            // Using the `:no-audio` VLC option is more reliable than
            // `audio().setMute(true)` because libVLC never initialises the
            // audio pipeline at all — no risk of an audio leak from a
            // startup race when the player is muted.
            if (muted) {
                mediaPlayer.media().play(normalizedUri, ":no-audio")
            } else {
                mediaPlayer.media().play(normalizedUri)
            }
        } catch (t: Throwable) {
            logger.error(t) { "VideoPlayer: media().play($uri) failed" }
            errorMessage = "Could not start playback: ${t.message}"
        }

        onDispose {
            try {
                mediaPlayer.events().removeMediaPlayerEventListener(listener)
                mediaPlayer.controls().stop()
                mediaPlayer.release()
            } catch (t: Throwable) {
                logger.debug(t) { "VideoPlayer.dispose: cleanup threw" }
            } finally {
                playerRef.set(null)
            }
        }
    }

    StopSignalEffect(
        stopSignal = stopSignal,
        lastStopSignal = lastStopSignal,
        playerRef = { playerRef.get() },
        tag = "VideoPlayer",
        onStop = { currentFrame = null },
    )

    PauseEffect(
        paused = paused,
        playerRef = { playerRef.get() },
        tag = "VideoPlayer",
    )

    Box(modifier = modifier.background(Color.Black)) {
        val frame = currentFrame
        val err = errorMessage
        when {
            err != null -> Text(
                text = err,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
            frame != null -> Image(
                bitmap = frame,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            else -> Text(
                text = "Loading\u2026",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (totalMs > 0) {
            val progress = (currentMs / totalMs).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Palette.VideoOverlay)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Palette.ProgressTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Palette.AccentGold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = formatMillis(currentMs), color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatMillis(totalMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
