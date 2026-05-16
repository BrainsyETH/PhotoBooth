package com.snapcabin.filter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import java.io.File

/**
 * Applies admin-uploaded branding layers to a finished photo.
 *
 * Both border and overlay are arbitrary PNGs (presumed transparent in
 * the areas that should let the photo show through). They are scaled to
 * the source canvas and drawn on top in order: border first, overlay second,
 * so the overlay can sit above a frame.
 */
object CustomBrandingRenderer {

    private const val TAG = "CustomBrandingRenderer"

    fun apply(source: Bitmap, borderPath: String, overlayPath: String): Bitmap {
        val border = loadValid(borderPath)
        val overlay = loadValid(overlayPath)
        if (border == null && overlay == null) return source

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val dst = Rect(0, 0, result.width, result.height)

        border?.let {
            canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), dst, paint)
            it.recycle()
        }
        overlay?.let {
            canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), dst, paint)
            it.recycle()
        }
        return result
    }

    private fun loadValid(path: String): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode branding image at $path", e)
            null
        }
    }
}
