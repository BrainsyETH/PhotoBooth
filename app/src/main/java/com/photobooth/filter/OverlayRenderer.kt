package com.photobooth.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

enum class PhotoOverlay(val displayName: String) {
    NONE("None"),
    FILM_STRIP("Film Strip"),
    VIGNETTE("Vignette"),
    PARTY_BORDER("Party"),
    CORNER_HEARTS("Hearts"),
    STAR_BURST("Stars");
}

object OverlayRenderer {

    fun applyOverlay(source: Bitmap, overlay: PhotoOverlay): Bitmap {
        if (overlay == PhotoOverlay.NONE) return source

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        when (overlay) {
            PhotoOverlay.NONE -> {}
            PhotoOverlay.FILM_STRIP -> drawFilmStrip(canvas)
            PhotoOverlay.VIGNETTE -> drawVignette(canvas)
            PhotoOverlay.PARTY_BORDER -> drawPartyBorder(canvas)
            PhotoOverlay.CORNER_HEARTS -> drawCornerHearts(canvas)
            PhotoOverlay.STAR_BURST -> drawStarBurst(canvas)
        }
        return result
    }

    private fun drawFilmStrip(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val stripH = h * 0.06f
        val paint = Paint().apply {
            color = 0xFF1A1A1A.toInt()
            style = Paint.Style.FILL
        }
        val holePaint = Paint().apply {
            color = 0xFF333333.toInt()
            style = Paint.Style.FILL
        }

        // Top and bottom strips
        canvas.drawRect(0f, 0f, w, stripH, paint)
        canvas.drawRect(0f, h - stripH, w, h, paint)

        // Sprocket holes
        val holeSize = stripH * 0.5f
        val spacing = holeSize * 2.5f
        var x = spacing
        while (x < w) {
            val cy1 = stripH / 2
            val cy2 = h - stripH / 2
            canvas.drawRoundRect(
                RectF(x - holeSize / 2, cy1 - holeSize / 2, x + holeSize / 2, cy1 + holeSize / 2),
                4f, 4f, holePaint
            )
            canvas.drawRoundRect(
                RectF(x - holeSize / 2, cy2 - holeSize / 2, x + holeSize / 2, cy2 + holeSize / 2),
                4f, 4f, holePaint
            )
            x += spacing
        }
    }

    private fun drawVignette(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val cx = w / 2
        val cy = h / 2
        val radius = maxOf(w, h) * 0.7f

        val paint = Paint().apply {
            shader = android.graphics.RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0x00000000, 0xAA000000.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun drawPartyBorder(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val borderWidth = minOf(w, h) * 0.03f
        val colors = intArrayOf(
            0xFFFF6584.toInt(), // pink
            0xFF6C63FF.toInt(), // purple
            0xFF00E5FF.toInt(), // cyan
            0xFFFFD700.toInt(), // gold
            0xFF00FF87.toInt()  // green
        )

        val paint = Paint().apply { style = Paint.Style.FILL }
        val segmentLen = (2 * (w + h)) / (colors.size * 8)
        var pos = 0f
        var colorIdx = 0

        // Top edge
        var x = 0f
        while (x < w) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawRect(x, 0f, minOf(x + segmentLen, w), borderWidth, paint)
            x += segmentLen
            colorIdx++
        }
        // Bottom edge
        x = 0f
        while (x < w) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawRect(x, h - borderWidth, minOf(x + segmentLen, w), h, paint)
            x += segmentLen
            colorIdx++
        }
        // Left edge
        var y = borderWidth
        while (y < h - borderWidth) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawRect(0f, y, borderWidth, minOf(y + segmentLen, h - borderWidth), paint)
            y += segmentLen
            colorIdx++
        }
        // Right edge
        y = borderWidth
        while (y < h - borderWidth) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawRect(w - borderWidth, y, w, minOf(y + segmentLen, h - borderWidth), paint)
            y += segmentLen
            colorIdx++
        }
    }

    private fun drawCornerHearts(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val heartSize = minOf(w, h) * 0.08f
        val margin = heartSize * 0.8f
        val paint = Paint().apply {
            color = 0xCCFF6584.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val corners = listOf(
            margin to margin,
            w - margin to margin,
            margin to h - margin,
            w - margin to h - margin
        )

        for ((cx, cy) in corners) {
            drawHeart(canvas, cx, cy, heartSize, paint)
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val half = size / 2
        path.moveTo(cx, cy + half * 0.4f)
        path.cubicTo(cx - half, cy - half * 0.5f, cx - half * 1.5f, cy + half * 0.3f, cx, cy + half)
        path.moveTo(cx, cy + half * 0.4f)
        path.cubicTo(cx + half, cy - half * 0.5f, cx + half * 1.5f, cy + half * 0.3f, cx, cy + half)
        canvas.drawPath(path, paint)
    }

    private fun drawStarBurst(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val starSize = minOf(w, h) * 0.04f
        val paint = Paint().apply {
            color = 0xBBFFD700.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Scatter stars around edges
        val positions = listOf(
            w * 0.05f to h * 0.05f,
            w * 0.15f to h * 0.03f,
            w * 0.85f to h * 0.05f,
            w * 0.95f to h * 0.08f,
            w * 0.03f to h * 0.92f,
            w * 0.12f to h * 0.96f,
            w * 0.88f to h * 0.94f,
            w * 0.95f to h * 0.90f,
            w * 0.5f to h * 0.03f,
            w * 0.5f to h * 0.97f,
            w * 0.03f to h * 0.5f,
            w * 0.97f to h * 0.5f
        )

        for ((sx, sy) in positions) {
            drawStar(canvas, sx, sy, starSize, paint)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val outerR = size
        val innerR = size * 0.4f
        val points = 5

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
