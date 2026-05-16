package com.snapcabin.ui.screens.attract

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinOnBackground
import com.snapcabin.ui.theme.CabinOnPrimary
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment

@Composable
fun AttractScreen(
    onTap: () -> Unit,
    onAdminLongPress: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "attract")
    val ctaAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta"
    )

    // Cream-at-center radial fading to parchment then oat at the edges.
    val backdrop = Brush.radialGradient(
        colors = listOf(CabinSurface, Parchment, Oat),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground)
            .background(backdrop)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onAdminLongPress() }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Logomark sits on a cream tile w/ hairline border so the
            // dark-background launcher icon reads cleanly on light surfaces.
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CabinSurface)
                    .border(1.dp, CabinLine, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = CabinOnBackground,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.attract_tagline),
                fontSize = 22.sp,
                fontStyle = FontStyle.Italic,
                color = Espresso.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Pine pill — the only place a primary action becomes a pill.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CabinPrimary.copy(alpha = ctaAlpha))
                    .padding(horizontal = 56.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.attract_tap_to_start),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CabinOnPrimary,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.attract_modes),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Espresso.copy(alpha = 0.50f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }

        Text(
            text = stringResource(R.string.attract_admin_hint),
            fontSize = 12.sp,
            color = Espresso.copy(alpha = 0.28f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
