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
 *
 * Memory: the layers are decoded with downsampling so an oversized PNG (e.g.
 * a multi-megapixel logo) can't blow the heap when combined with a full-res
 * camera bitmap. The whole apply is also failure-isolated — any decode error
 * or out-of-memory condition falls back to the unbranded photo rather than
 * crashing the capture flow.
 */
object CustomBrandingRenderer {

    private const val TAG = "CustomBrandingRenderer"

    fun apply(source: Bitmap, borderPath: String, overlayPath: String): Bitmap {
        val border = loadScaled(borderPath, source.width, source.height)
        val overlay = loadScaled(overlayPath, source.width, source.height)
        if (border == null && overlay == null) return source

        return try {
            val result = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            val dst = Rect(0, 0, result.width, result.height)

            border?.let { canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), dst, paint) }
            overlay?.let { canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), dst, paint) }
            result
        } catch (t: Throwable) {
            // OutOfMemoryError included — never let branding take down capture.
            Log.e(TAG, "Branding apply failed; using the unbranded photo", t)
            source
        } finally {
            border?.recycle()
            overlay?.recycle()
        }
    }

    /**
     * Decode a branding PNG, downsampled so its dimensions are no larger than
     * roughly the photo it will be drawn onto. Returns null (and logs) when the
     * path is empty, the file is missing/unreadable, or decoding fails.
     */
    private fun loadScaled(path: String, targetW: Int, targetH: Int): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            // Common after a reinstall: the stored absolute path points at a
            // file that no longer exists. Surface it instead of failing silently.
            Log.w(TAG, "Branding image not found at $path — re-upload it under BRANDING")
            return null
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.w(TAG, "Branding image at $path has no decodable bounds")
                return null
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, targetW, targetH)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to decode branding image at $path", t)
            null
        }
    }

    /** Largest power-of-two sample size that keeps the layer >= the target size. */
    private fun computeInSampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        if (dstW <= 0 || dstH <= 0) return 1
        var sample = 1
        while (srcW / (sample * 2) >= dstW && srcH / (sample * 2) >= dstH) {
            sample *= 2
        }
        return sample
    }
}
