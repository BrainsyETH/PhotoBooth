package com.snapcabin.ui.screens.attract

import androidx.compose.animation.core.EaseInOutSine
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.snapcabin.ui.components.rememberScreenClass
import com.snapcabin.ui.components.scaledDp
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut

@Composable
fun AttractScreen(
    onTap: () -> Unit,
    onAdminLongPress: () -> Unit = {},
    eventName: String = "",
    subtext: String = "",
    isFirstRun: Boolean = false
) {
    val screen = rememberScreenClass()
    val logoSize = screen.scaledDp(220).dp
    val titleLarge = screen.scaledDp(88).sp
    val titleSmall = screen.scaledDp(64).sp
    val subtextSize = screen.scaledDp(26).sp
    val ctaTextSize = screen.scaledDp(40).sp

    val infiniteTransition = rememberInfiniteTransition(label = "attract")

    // The CTA is the hero: a gentle scale "breath" (not just alpha) reads as
    // alive from across a room and competes with an open bar.
    val ctaScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta-scale"
    )
    // A soft champagne halo pulses behind it, slightly out of phase, so the
    // whole screen has ambient motion rather than one blinking pill.
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-alpha"
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
            Image(
                painter = painterResource(id = R.drawable.snapcabin_logov2),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(logoSize)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            val headline = if (eventName.isNotBlank()) eventName else stringResource(R.string.app_name)
            Text(
                text = headline,
                fontSize = if (headline.length > 16) titleSmall else titleLarge,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.015f).em,
                maxLines = 2
            )

            if (subtext.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = subtext,
                    fontSize = subtextSize,
                    fontFamily = FrankRuhlLibre,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = Walnut,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Hero CTA: champagne halo + scaling pine pill with large lettering.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(screen.scaledDp(360).dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Honey.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .scale(ctaScale)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(Radii.full),
                            clip = false
                        )
                        .clip(RoundedCornerShape(Radii.full))
                        .background(CabinPrimary)
                        .padding(horizontal = Spacing.xxl, vertical = Spacing.lg)
                ) {
                    Text(
                        text = stringResource(R.string.attract_tap_to_start),
                        fontFamily = HankenGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = ctaTextSize,
                        letterSpacing = 0.06f.em,
                        color = Color.White
                    )
                }
            }
        }

        // First run only: teach the setup gesture + default PIN once. After the
        // host sets a PIN or starts an event this disappears, so it never
        // invites tipsy guests into the admin dialog at a live event.
        if (isFirstRun) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.xl)
                    .clip(RoundedCornerShape(Radii.l))
                    .background(Cream)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.l))
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text(
                    text = "New booth? Press and hold anywhere to set it up.  ·  PIN 1234",
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Espresso.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
