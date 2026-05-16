package com.snapcabin.ui.screens.thankyou

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut
import kotlinx.coroutines.delay

@Composable
fun ThankYouScreen(
    onDone: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(5000)
        onDone()
    }

    val targetScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = tween(durationMillis = 500),
        label = "thank-scale"
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "thank-alpha"
    )

    val backdrop = Brush.radialGradient(
        colors = listOf(CabinSurface, Parchment, Oat),
        radius = 1500f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground)
            .background(backdrop),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(targetScale)
                .alpha(targetAlpha)
        ) {
            Text(
                text = "Thank you.",
                fontSize = 140.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02f).em
            )

            Spacer(modifier = Modifier.height(Spacing.lg + Spacing.xs))

            // Decorative divider — 64dp × 1dp walnut @ 50%
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .background(Walnut.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(Spacing.lg + Spacing.xs))

            Text(
                text = "Your photo is ready",
                fontSize = 30.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = Walnut,
                textAlign = TextAlign.Center
            )
        }
    }
}
