package com.snapcabin.ui.screens.attract

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinOnBackground

@Composable
fun AttractScreen(
    onTap: () -> Unit,
    onAdminLongPress: () -> Unit = {}
) {
    // Subtle breathing animation on the CTA text only
    val infiniteTransition = rememberInfiniteTransition(label = "attract")
    val ctaAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onAdminLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo + Title side by side
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "SnapCabin logo",
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SnapCabin",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = CabinOnBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Tap anywhere to start",
                fontSize = 24.sp,
                color = CabinAccent.copy(alpha = ctaAlpha),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Photo  /  Collage  /  GIF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}
