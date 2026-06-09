package com.snapcabin.filter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import java.io.File

/**
 * Applies admin-uploaded branding layers to a finished photo.
 *
 * The border is always drawn full-frame (a frame is meant to wrap the whole
 * photo). The overlay (a logo) can either be stretched full-frame or placed in
 * one corner at a chosen size — see [overlayPlacement]. Layers are drawn in
 * order: border first, overlay second, so the logo can sit above a frame.
 *
 * Memory: the layers are decoded with downsampling so an oversized PNG (e.g.
 * a multi-megapixel logo) can't blow the heap when combined with a full-res
 * camera bitmap. The whole apply is also failure-isolated — any decode error
 * or out-of-memory condition falls back to the unbranded photo rather than
 * crashing the capture flow.
 */
object CustomBrandingRenderer {

    private const val TAG = "CustomBrandingRenderer"
    private const val CORNER_MARGIN_FRACTION = 0.04f

    /**
     * @param overlayPlacement "stretch" (full-frame) or "corner".
     * @param overlayCorner    "tl" | "tr" | "bl" | "br" — only used for corner placement.
     * @param overlaySizePct   logo width as a percent of the photo width — only used for corner.
     */
    fun apply(
        source: Bitmap,
        borderPath: String,
        overlayPath: String,
        overlayPlacement: String = "stretch",
        overlayCorner: String = "br",
        overlaySizePct: Int = 20
    ): Bitmap {
        val cornerMode = overlayPlacement.equals("corner", ignoreCase = true)
        val border = loadScaled(borderPath, source.width, source.height)
        // For a corner logo we only need it at ~sizePct of the photo width, so
        // decode it downsampled to that box rather than the full frame.
        val (ovTargetW, ovTargetH) = if (cornerMode) {
            val w = (source.width * overlaySizePct.coerceIn(5, 60) / 100).coerceAtLeast(1)
            w to w
        } else {
            source.width to source.height
        }
        val overlay = loadScaled(overlayPath, ovTargetW, ovTargetH)
        if (border == null && overlay == null) return source

        return try {
            val result = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            val full = Rect(0, 0, result.width, result.height)

            border?.let { canvas.drawBitmap(it, Rect(0, 0, it.width, it.height), full, paint) }
            overlay?.let { ov ->
                if (cornerMode) {
                    drawCornerOverlay(canvas, ov, result.width, result.height, overlayCorner, overlaySizePct, paint)
                } else {
                    canvas.drawBitmap(ov, Rect(0, 0, ov.width, ov.height), full, paint)
                }
            }
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

    /** Scale the overlay to [sizePct] of the photo width (aspect preserved) and
     *  place it in the chosen corner with a small margin. */
    private fun drawCornerOverlay(
        canvas: Canvas,
        overlay: Bitmap,
        photoW: Int,
        photoH: Int,
        corner: String,
        sizePct: Int,
        paint: Paint
    ) {
        if (overlay.width <= 0 || overlay.height <= 0) return
        val targetW = photoW * sizePct.coerceIn(5, 60) / 100f
        val scale = targetW / overlay.width
        val targetH = overlay.height * scale
        val margin = photoW * CORNER_MARGIN_FRACTION

        val left = when (corner.lowercase()) {
            "tl", "bl" -> margin
            else -> photoW - targetW - margin // tr, br
        }
        val top = when (corner.lowercase()) {
            "tl", "tr" -> margin
            else -> photoH - targetH - margin // bl, br
        }
        val dst = RectF(left, top, left + targetW, top + targetH)
        canvas.drawBitmap(overlay, null, dst, paint)
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
