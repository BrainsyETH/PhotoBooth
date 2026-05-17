package com.snapcabin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AnimatedMode { Single, Collage, Gif }

/**
 * Looping preview glyph for each capture mode. Plays continuously to telegraph
 * what the mode actually does on first glance — no text required.
 */
@Composable
fun AnimatedModePreview(
    mode: AnimatedMode,
    color: Color,
    size: Dp = 40.dp
) {
    when (mode) {
        AnimatedMode.Single -> SinglePreview(color, size)
        AnimatedMode.Collage -> CollagePreview(color, size)
        AnimatedMode.Gif -> GifPreview(color, size)
    }
}

@Composable
private fun SinglePreview(color: Color, size: Dp) {
    // One photo card eases up into view, holds, fades — every 2s.
    val transition = rememberInfiniteTransition(label = "single")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "single-prog"
    )
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cardW = w * 0.65f
        val cardH = h * 0.78f
        // Ease in over 0..0.35, hold 0.35..0.75, fade 0.75..1.0
        val appear = (progress / 0.35f).coerceIn(0f, 1f)
        val fade = ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
        val alpha = appear * (1f - fade)
        val yOffset = (1f - appear) * 8f
        drawRoundRect(
            color = color.copy(alpha = alpha * 0.95f),
            topLeft = Offset((w - cardW) / 2f, (h - cardH) / 2f + yOffset),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Inner "image" rectangle
        val pad = cardW * 0.12f
        drawRoundRect(
            color = color.copy(alpha = alpha * 0.35f),
            topLeft = Offset((w - cardW) / 2f + pad, (h - cardH) / 2f + yOffset + pad),
            size = Size(cardW - pad * 2, cardH - pad * 2.2f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

@Composable
private fun CollagePreview(color: Color, size: Dp) {
    // Four cells fade in one at a time on a 2.4s loop.
    val transition = rememberInfiniteTransition(label = "collage")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "collage-prog"
    )
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val gap = 3f
        val cellW = (w - gap) / 2f
        val cellH = (h - gap) / 2f
        val cells = listOf(
            Offset(0f, 0f),
            Offset(cellW + gap, 0f),
            Offset(0f, cellH + gap),
            Offset(cellW + gap, cellH + gap)
        )
        // Each cell appears at i*0.18, holds until 0.85, then fade for restart
        cells.forEachIndexed { i, offset ->
            val start = i * 0.18f
            val appear = ((progress - start) / 0.15f).coerceIn(0f, 1f)
            val fade = ((progress - 0.88f) / 0.12f).coerceIn(0f, 1f)
            val alpha = appear * (1f - fade)
            drawRoundRect(
                color = color.copy(alpha = alpha * 0.9f),
                topLeft = offset,
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
private fun GifPreview(color: Color, size: Dp) {
    // A small "frame counter" of 6 segments that fills clockwise around a card.
    val transition = rememberInfiniteTransition(label = "gif")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        ),
        label = "gif-prog"
    )
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cardW = w * 0.72f
        val cardH = h * 0.72f
        val left = (w - cardW) / 2f
        val top = (h - cardH) / 2f
        // The "film frame"
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Six perforation dots along the top, one fully lit per beat
        val dotR = w * 0.04f
        val activeIdx = progress.toInt() % 6
        for (i in 0 until 6) {
            val x = left + cardW * (0.12f + i * 0.152f)
            val yT = top - dotR * 1.6f
            val yB = top + cardH + dotR * 1.6f
            val alpha = if (i == activeIdx) 1f else 0.35f
            drawCircle(color = color.copy(alpha = alpha), radius = dotR, center = Offset(x, yT))
            drawCircle(color = color.copy(alpha = alpha), radius = dotR, center = Offset(x, yB))
        }
    }
}
