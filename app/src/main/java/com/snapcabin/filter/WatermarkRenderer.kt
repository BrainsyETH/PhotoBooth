package com.snapcabin.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object WatermarkRenderer {

    fun apply(
        bitmap: Bitmap,
        text: String,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        textSizeFraction: Float = 0.03f, // Relative to image width
        opacity: Int = 128 // 0-255
    ): Bitmap {
        if (text.isBlank()) return bitmap

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val textSize = result.width * textSizeFraction
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = opacity
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(textSize * 0.15f, 2f, 2f, Color.argb(opacity, 0, 0, 0))
        }

        val textWidth = paint.measureText(text)
        val padding = textSize * 0.8f

        val (x, y) = when (position) {
            WatermarkPosition.BOTTOM_RIGHT -> {
                Pair(result.width - textWidth - padding, result.height - padding)
            }
            WatermarkPosition.BOTTOM_LEFT -> {
                Pair(padding, result.height - padding)
            }
            WatermarkPosition.TOP_RIGHT -> {
                Pair(result.width - textWidth - padding, textSize + padding)
            }
            WatermarkPosition.TOP_LEFT -> {
                Pair(padding, textSize + padding)
            }
            WatermarkPosition.CENTER -> {
                Pair((result.width - textWidth) / 2f, result.height / 2f)
            }
        }

        canvas.drawText(text, x, y, paint)
        return result
    }
}

enum class WatermarkPosition {
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_RIGHT,
    TOP_LEFT,
    CENTER
}
