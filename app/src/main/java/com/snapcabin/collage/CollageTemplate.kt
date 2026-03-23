package com.snapcabin.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

enum class CollageLayout(val displayName: String, val photoCount: Int) {
    SINGLE("Single", 1),
    SIDE_BY_SIDE("Side by Side", 2),
    GRID_2X2("Grid 2x2", 4),
    STRIP_VERTICAL("Photo Strip", 3),
    FEATURE_LEFT("Feature Left", 3);
}

object CollageRenderer {

    private const val PADDING = 12
    private const val BACKGROUND_COLOR = 0xFF1E1E1E.toInt()
    private const val CORNER_RADIUS = 16f

    fun render(photos: List<Bitmap>, layout: CollageLayout, outputWidth: Int = 1920, outputHeight: Int = 1080): Bitmap {
        val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val bgPaint = Paint().apply { color = BACKGROUND_COLOR }
        canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), bgPaint)

        val slots = getSlots(layout, outputWidth, outputHeight)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

        for (i in slots.indices) {
            val photo = photos.getOrNull(i) ?: photos.lastOrNull() ?: continue
            val slot = slots[i]
            drawPhotoInSlot(canvas, photo, slot, paint)
        }

        return result
    }

    private fun getSlots(layout: CollageLayout, w: Int, h: Int): List<RectF> {
        val p = PADDING.toFloat()
        return when (layout) {
            CollageLayout.SINGLE -> listOf(
                RectF(p, p, w - p, h - p)
            )

            CollageLayout.SIDE_BY_SIDE -> {
                val mid = w / 2f
                listOf(
                    RectF(p, p, mid - p / 2, h - p),
                    RectF(mid + p / 2, p, w - p, h - p)
                )
            }

            CollageLayout.GRID_2X2 -> {
                val midX = w / 2f
                val midY = h / 2f
                listOf(
                    RectF(p, p, midX - p / 2, midY - p / 2),
                    RectF(midX + p / 2, p, w - p, midY - p / 2),
                    RectF(p, midY + p / 2, midX - p / 2, h - p),
                    RectF(midX + p / 2, midY + p / 2, w - p, h - p)
                )
            }

            CollageLayout.STRIP_VERTICAL -> {
                val slotH = (h - p * 4) / 3
                listOf(
                    RectF(w * 0.25f, p, w * 0.75f, p + slotH),
                    RectF(w * 0.25f, p * 2 + slotH, w * 0.75f, p * 2 + slotH * 2),
                    RectF(w * 0.25f, p * 3 + slotH * 2, w * 0.75f, p * 3 + slotH * 3)
                )
            }

            CollageLayout.FEATURE_LEFT -> {
                val splitX = w * 0.6f
                val midY = h / 2f
                listOf(
                    RectF(p, p, splitX - p / 2, h - p),
                    RectF(splitX + p / 2, p, w - p, midY - p / 2),
                    RectF(splitX + p / 2, midY + p / 2, w - p, h - p)
                )
            }
        }
    }

    private fun drawPhotoInSlot(canvas: Canvas, photo: Bitmap, slot: RectF, paint: Paint) {
        val slotW = slot.width()
        val slotH = slot.height()
        val photoAspect = photo.width.toFloat() / photo.height
        val slotAspect = slotW / slotH

        val srcRect: Rect
        if (photoAspect > slotAspect) {
            // Photo is wider — crop sides
            val cropW = (photo.height * slotAspect).toInt()
            val offset = (photo.width - cropW) / 2
            srcRect = Rect(offset, 0, offset + cropW, photo.height)
        } else {
            // Photo is taller — crop top/bottom
            val cropH = (photo.width / slotAspect).toInt()
            val offset = (photo.height - cropH) / 2
            srcRect = Rect(0, offset, photo.width, offset + cropH)
        }

        // Clip to rounded rect
        canvas.save()
        val path = android.graphics.Path()
        path.addRoundRect(slot, CORNER_RADIUS, CORNER_RADIUS, android.graphics.Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawBitmap(photo, srcRect, Rect(slot.left.toInt(), slot.top.toInt(), slot.right.toInt(), slot.bottom.toInt()), paint)
        canvas.restore()
    }
}
