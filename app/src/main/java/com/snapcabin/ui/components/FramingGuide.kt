package com.snapcabin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.snapcabin.ui.theme.Honey

@Composable
fun FramingGuide(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "framing-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "framing-pulse-alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokePx = 2.dp.toPx()
        val rx = size.width * 0.34f
        val ry = size.height * 0.18f
        val cx = size.width / 2f
        val cy = size.height * 0.66f

        drawOval(
            color = Honey.copy(alpha = 0.85f * alpha),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2, ry * 2),
            style = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
            )
        )
    }
}
