package com.photobooth.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class PhotoFilter(val displayName: String) {
    NORMAL("Normal"),
    GRAYSCALE("B&W"),
    SEPIA("Sepia"),
    WARM("Warm"),
    COOL("Cool"),
    VIVID("Vivid"),
    FADE("Fade"),
    NOIR("Noir");
}

object FilterEngine {

    fun applyFilter(source: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.NORMAL) return source

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(getColorMatrix(filter))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun getColorMatrix(filter: PhotoFilter): ColorMatrix {
        return when (filter) {
            PhotoFilter.NORMAL -> ColorMatrix()

            PhotoFilter.GRAYSCALE -> ColorMatrix().apply {
                setSaturation(0f)
            }

            PhotoFilter.SEPIA -> ColorMatrix().apply {
                setSaturation(0f)
                val sepiaTone = ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 20f,
                    0f, 1.0f, 0f, 0f, 10f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                postConcat(sepiaTone)
            }

            PhotoFilter.WARM -> ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 10f,
                0f, 1.1f, 0f, 0f, 5f,
                0f, 0f, 0.9f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))

            PhotoFilter.COOL -> ColorMatrix(floatArrayOf(
                0.9f, 0f, 0f, 0f, -10f,
                0f, 1.0f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            ))

            PhotoFilter.VIVID -> ColorMatrix().apply {
                setSaturation(1.8f)
                val brightness = ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 10f,
                    0f, 1.1f, 0f, 0f, 10f,
                    0f, 0f, 1.1f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                postConcat(brightness)
            }

            PhotoFilter.FADE -> ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 30f,
                0f, 1f, 0f, 0f, 30f,
                0f, 0f, 1f, 0f, 30f,
                0f, 0f, 0f, 0.9f, 0f
            )).apply {
                val desat = ColorMatrix()
                desat.setSaturation(0.6f)
                postConcat(desat)
            }

            PhotoFilter.NOIR -> ColorMatrix().apply {
                setSaturation(0f)
                val contrast = ColorMatrix(floatArrayOf(
                    1.5f, 0f, 0f, 0f, -40f,
                    0f, 1.5f, 0f, 0f, -40f,
                    0f, 0f, 1.5f, 0f, -40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                postConcat(contrast)
            }
        }
    }
}
