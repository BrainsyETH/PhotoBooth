package com.snapcabin.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders event branding templates (borders, logos, text overlays)
 * onto photos. Templates can be loaded from assets or external files.
 */
@Singleton
class EventBrandingRenderer @Inject constructor() {

    companion object {
        private const val TAG = "EventBranding"
    }

    fun applyTemplate(
        photo: Bitmap,
        template: EventTemplate,
        eventName: String = "",
        eventDate: String = "",
        customLogoPath: String? = null,
        context: Context? = null
    ): Bitmap {
        val result = photo.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width.toFloat()
        val h = result.height.toFloat()

        when (template) {
            EventTemplate.NONE -> { /* no-op */ }

            EventTemplate.CLASSIC_BORDER -> {
                drawClassicBorder(canvas, w, h, eventName, eventDate)
            }

            EventTemplate.WEDDING -> {
                drawWeddingFrame(canvas, w, h, eventName, eventDate)
            }

            EventTemplate.BIRTHDAY -> {
                drawBirthdayFrame(canvas, w, h, eventName, eventDate)
            }

            EventTemplate.CORPORATE -> {
                drawCorporateFrame(canvas, w, h, eventName)
            }

            EventTemplate.HOLIDAY -> {
                drawHolidayFrame(canvas, w, h, eventName, eventDate)
            }

            EventTemplate.CUSTOM -> {
                drawCustomFrame(canvas, w, h, eventName, eventDate, customLogoPath, context)
            }
        }

        return result
    }

    private fun drawClassicBorder(canvas: Canvas, w: Float, h: Float, name: String, date: String) {
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = w * 0.02f
        }
        val inset = w * 0.03f
        canvas.drawRect(inset, inset, w - inset, h - inset, borderPaint)

        // Inner double line
        val innerInset = inset + w * 0.01f
        borderPaint.strokeWidth = w * 0.005f
        canvas.drawRect(innerInset, innerInset, w - innerInset, h - innerInset, borderPaint)

        drawEventText(canvas, w, h, name, date, Color.WHITE, inset * 2)
    }

    private fun drawWeddingFrame(canvas: Canvas, w: Float, h: Float, name: String, date: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Soft gold border
        paint.color = Color.parseColor("#D4AF37")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.015f
        val inset = w * 0.025f
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), w * 0.02f, w * 0.02f, paint)

        // Corner flourishes (simple L shapes in gold)
        paint.strokeWidth = w * 0.008f
        val flourishLen = w * 0.06f
        val corners = listOf(
            floatArrayOf(inset * 2, inset * 2, inset * 2 + flourishLen, inset * 2),
            floatArrayOf(inset * 2, inset * 2, inset * 2, inset * 2 + flourishLen),
            floatArrayOf(w - inset * 2, inset * 2, w - inset * 2 - flourishLen, inset * 2),
            floatArrayOf(w - inset * 2, inset * 2, w - inset * 2, inset * 2 + flourishLen),
            floatArrayOf(inset * 2, h - inset * 2, inset * 2 + flourishLen, h - inset * 2),
            floatArrayOf(inset * 2, h - inset * 2, inset * 2, h - inset * 2 - flourishLen),
            floatArrayOf(w - inset * 2, h - inset * 2, w - inset * 2 - flourishLen, h - inset * 2),
            floatArrayOf(w - inset * 2, h - inset * 2, w - inset * 2, h - inset * 2 - flourishLen)
        )
        for (c in corners) {
            canvas.drawLine(c[0], c[1], c[2], c[3], paint)
        }

        drawEventText(canvas, w, h, name, date, Color.parseColor("#D4AF37"), inset * 3)
    }

    private fun drawBirthdayFrame(canvas: Canvas, w: Float, h: Float, name: String, date: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Colorful confetti dots along border
        val colors = intArrayOf(
            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"), Color.parseColor("#FFA07A"),
            Color.parseColor("#98D8C8"), Color.parseColor("#FFD93D")
        )
        val dotSize = w * 0.012f
        val spacing = w * 0.04f

        var x = spacing
        var colorIdx = 0
        while (x < w - spacing) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawCircle(x, dotSize * 2, dotSize, paint)
            canvas.drawCircle(x, h - dotSize * 2, dotSize, paint)
            x += spacing
            colorIdx++
        }

        var y = spacing
        colorIdx = 2
        while (y < h - spacing) {
            paint.color = colors[colorIdx % colors.size]
            canvas.drawCircle(dotSize * 2, y, dotSize, paint)
            canvas.drawCircle(w - dotSize * 2, y, dotSize, paint)
            y += spacing
            colorIdx++
        }

        drawEventText(canvas, w, h, name, date, Color.WHITE, spacing)
    }

    private fun drawCorporateFrame(canvas: Canvas, w: Float, h: Float, name: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Bottom bar with company name
        val barHeight = h * 0.08f
        paint.color = Color.parseColor("#2C3E50")
        paint.alpha = 200
        canvas.drawRect(0f, h - barHeight, w, h, paint)

        if (name.isNotBlank()) {
            paint.color = Color.WHITE
            paint.alpha = 255
            paint.textSize = barHeight * 0.5f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(name, w / 2, h - barHeight * 0.3f, paint)
        }

        // Subtle top line accent
        paint.color = Color.parseColor("#3498DB")
        paint.alpha = 255
        canvas.drawRect(0f, 0f, w, h * 0.005f, paint)
    }

    private fun drawHolidayFrame(canvas: Canvas, w: Float, h: Float, name: String, date: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Red and green alternating border
        val segmentWidth = w * 0.03f
        val borderThickness = h * 0.015f

        var x = 0f
        var isRed = true
        while (x < w) {
            paint.color = if (isRed) Color.parseColor("#C0392B") else Color.parseColor("#27AE60")
            canvas.drawRect(x, 0f, (x + segmentWidth).coerceAtMost(w), borderThickness, paint)
            canvas.drawRect(x, h - borderThickness, (x + segmentWidth).coerceAtMost(w), h, paint)
            x += segmentWidth
            isRed = !isRed
        }

        var y = 0f
        isRed = true
        while (y < h) {
            paint.color = if (isRed) Color.parseColor("#C0392B") else Color.parseColor("#27AE60")
            canvas.drawRect(0f, y, borderThickness, (y + segmentWidth).coerceAtMost(h), paint)
            canvas.drawRect(w - borderThickness, y, w, (y + segmentWidth).coerceAtMost(h), paint)
            y += segmentWidth
            isRed = !isRed
        }

        drawEventText(canvas, w, h, name, date, Color.WHITE, segmentWidth * 2)
    }

    private fun drawCustomFrame(
        canvas: Canvas,
        w: Float,
        h: Float,
        name: String,
        date: String,
        logoPath: String?,
        context: Context?
    ) {
        // Load and draw custom logo if available
        if (logoPath != null && context != null) {
            try {
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    val logo = BitmapFactory.decodeStream(FileInputStream(logoFile))
                    if (logo != null) {
                        val logoMaxW = w * 0.2f
                        val logoMaxH = h * 0.15f
                        val scale = minOf(logoMaxW / logo.width, logoMaxH / logo.height)
                        val scaledW = (logo.width * scale).toInt()
                        val scaledH = (logo.height * scale).toInt()
                        val scaledLogo = Bitmap.createScaledBitmap(logo, scaledW, scaledH, true)

                        // Bottom-right corner
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        paint.alpha = 200
                        canvas.drawBitmap(
                            scaledLogo,
                            w - scaledW - w * 0.02f,
                            h - scaledH - h * 0.02f,
                            paint
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load custom logo", e)
            }
        }

        drawEventText(canvas, w, h, name, date, Color.WHITE, w * 0.04f)
    }

    private fun drawEventText(
        canvas: Canvas,
        w: Float,
        h: Float,
        name: String,
        date: String,
        color: Int,
        bottomPadding: Float
    ) {
        if (name.isBlank() && date.isBlank()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(w * 0.005f, 2f, 2f, Color.argb(128, 0, 0, 0))
        }

        if (name.isNotBlank()) {
            paint.textSize = w * 0.025f
            canvas.drawText(name, w / 2, h - bottomPadding, paint)
        }

        if (date.isNotBlank()) {
            paint.textSize = w * 0.018f
            paint.typeface = Typeface.DEFAULT
            val dateY = if (name.isNotBlank()) h - bottomPadding + w * 0.03f else h - bottomPadding
            canvas.drawText(date, w / 2, dateY, paint)
        }
    }
}

enum class EventTemplate(val displayName: String) {
    NONE("None"),
    CLASSIC_BORDER("Classic Border"),
    WEDDING("Wedding"),
    BIRTHDAY("Birthday"),
    CORPORATE("Corporate"),
    HOLIDAY("Holiday"),
    CUSTOM("Custom Logo")
}
