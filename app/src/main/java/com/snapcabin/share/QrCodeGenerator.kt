package com.snapcabin.share

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeGenerator @Inject constructor() {

    /**
     * Render [content] as a QR code.
     *
     * The default colors are inverted (white modules on black) to match the
     * share-screen card treatment. Pass [darkColor]/[lightColor] explicitly for
     * a standard black-on-white code — e.g. the admin setup links, which are
     * meant to be scanned by a phone camera.
     */
    fun generate(
        content: String,
        size: Int = 512,
        darkColor: Int = Color.WHITE,
        lightColor: Int = Color.BLACK
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
            }
        }
        return bitmap
    }
}
