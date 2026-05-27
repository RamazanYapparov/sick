package app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
internal fun QrCode(
    text: String,
    modifier: Modifier = Modifier,
    fgColor: Color = Color.Black,
    bgColor: Color = Color.White
) {
    val bitMatrix = remember(text) {
        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256, hints)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (bitMatrix != null) {
        val width = bitMatrix.width
        val height = bitMatrix.height
        Canvas(modifier = modifier) {
            // Draw background
            drawRect(color = bgColor)
            
            val sizeX = size.width / width
            val sizeY = size.height / height
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix.get(x, y)) {
                        drawRect(
                            color = fgColor,
                            topLeft = Offset(x * sizeX, y * sizeY),
                            size = Size(sizeX + 0.2f, sizeY + 0.2f) // slightly larger to avoid thin lines due to floating point precision
                        )
                    }
                }
            }
        }
    }
}
