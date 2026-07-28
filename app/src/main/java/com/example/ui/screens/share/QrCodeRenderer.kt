package com.example.ui.screens.share

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders invite links as QR codes for burning into share cards.
 *
 * A screenshot of a story card loses any tappable link, and typed URLs leak conversions.
 * A QR survives the screenshot, and survives being re-shared as an image.
 */
object QrCodeRenderer {

    private const val TAG = "QrCodeRenderer"

    /**
     * @param sizePx side length of the returned square bitmap
     * @param darkColor module colour — keep it high-contrast against [lightColor]
     * @return the QR bitmap, or null if encoding failed (callers should degrade to text)
     */
    fun encode(
        content: String,
        sizePx: Int,
        darkColor: Int = Color.parseColor("#03140C"),
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = mapOf(
                // Medium recovers ~15% of modules — enough for a re-compressed social image.
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) darkColor else lightColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            Log.w(TAG, "QR encode failed for $sizePx px: ${e.message}")
            null
        }
    }
}
