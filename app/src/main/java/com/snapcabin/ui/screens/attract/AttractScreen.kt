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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut

@Composable
fun AttractScreen(
    onTap: () -> Unit,
    onAdminLongPress: () -> Unit = {},
    eventName: String = ""
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

    val backdrop = Brush.radialGradient(
        colors = listOf(CabinSurface, Parchment, Oat),
        radius = 1400f
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
            // Logomark on cream tile w/ hairline border.
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(Radii.l))
                    .background(CabinSurface)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.l)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 112.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.015f).em
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.attract_tagline),
                fontSize = 26.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = Walnut,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.xxl + Spacing.s))

            // Pine pill CTA — breathing 0.55 → 1.0 alpha.
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(Radii.full),
                        clip = false
                    )
                    .clip(RoundedCornerShape(Radii.full))
                    .background(CabinPrimary.copy(alpha = ctaAlpha))
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.attract_tap_to_start),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = androidx.compose.ui.graphics.Color.White
                    )
                )
            }

            if (eventName.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                Text(
                    text = eventName,
                    fontSize = 22.sp,
                    fontFamily = FrankRuhlLibre,
                    fontWeight = FontWeight.Medium,
                    color = Walnut,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = stringResource(R.string.attract_admin_hint),
            fontSize = 14.sp,
            fontFamily = HankenGrotesk,
            color = Espresso.copy(alpha = 0.28f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.lg)
        )
    }
}
