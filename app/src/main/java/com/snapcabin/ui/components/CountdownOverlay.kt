package com.snapcabin.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre

@Composable
fun CountdownOverlay(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Espresso.copy(alpha = 0.45f))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Espresso.copy(alpha = 0f),
                        Espresso.copy(alpha = 0.4f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (scaleIn(initialScale = 2f, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400)))
                    .togetherWith(
                        scaleOut(targetScale = 0.7f, animationSpec = tween(400)) +
                                fadeOut(animationSpec = tween(400))
                    )
            },
            label = "countdown"
        ) { targetCount ->
            Text(
                text = targetCount.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FrankRuhlLibre,
                    fontSize = 240.sp,
                    color = Cream
                )
            )
        }
    }
}
