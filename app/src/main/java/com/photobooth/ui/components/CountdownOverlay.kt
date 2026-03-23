package com.photobooth.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun CountdownOverlay(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (scaleIn(initialScale = 2f, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400)))
                    .togetherWith(
                        scaleOut(targetScale = 0.5f, animationSpec = tween(400)) +
                                fadeOut(animationSpec = tween(400))
                    )
            },
            label = "countdown"
        ) { targetCount ->
            Text(
                text = targetCount.toString(),
                fontSize = 200.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
