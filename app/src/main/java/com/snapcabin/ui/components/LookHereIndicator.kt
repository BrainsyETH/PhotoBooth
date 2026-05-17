package com.snapcabin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey

enum class LensPosition { Top, Bottom, Left, Right, None }

/**
 * "↑ LOOK HERE" pill that points at the physical camera lens. Position is
 * configured per kiosk because lens location depends on the tablet model
 * and mount orientation. Pulses gently to pull the eye off the live preview
 * during the count.
 */
@Composable
fun LookHereIndicator(
    position: LensPosition,
    modifier: Modifier = Modifier
) {
    if (position == LensPosition.None) return

    val transition = rememberInfiniteTransition(label = "look-here")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "look-here-alpha"
    )

    val alignment = when (position) {
        LensPosition.Top -> Alignment.TopCenter
        LensPosition.Bottom -> Alignment.BottomCenter
        LensPosition.Left -> Alignment.CenterStart
        LensPosition.Right -> Alignment.CenterEnd
        LensPosition.None -> Alignment.Center
    }
    val edgePadding = when (position) {
        LensPosition.Top -> Modifier.padding(top = 28.dp)
        LensPosition.Bottom -> Modifier.padding(bottom = 110.dp) // clear of skip pill
        LensPosition.Left -> Modifier.padding(start = 28.dp)
        LensPosition.Right -> Modifier.padding(end = 28.dp)
        LensPosition.None -> Modifier
    }
    val arrow = when (position) {
        LensPosition.Top -> "↑"
        LensPosition.Bottom -> "↓"
        LensPosition.Left -> "←"
        LensPosition.Right -> "→"
        LensPosition.None -> ""
    }
    val vertical = position == LensPosition.Top || position == LensPosition.Bottom
    val arrowFirst = position == LensPosition.Top || position == LensPosition.Left

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(alignment)
                .then(edgePadding)
                .clip(RoundedCornerShape(999.dp))
                .background(Honey.copy(alpha = alpha))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val label: @Composable () -> Unit = {
                Text(
                    text = "LOOK HERE",
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.22f.em,
                    color = Espresso
                )
            }
            val arrowText: @Composable () -> Unit = {
                Text(text = arrow, color = Espresso, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (vertical) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (arrowFirst) { arrowText(); label() } else { label(); arrowText() }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (arrowFirst) { arrowText(); label() } else { label(); arrowText() }
                }
            }
        }
    }
}

fun lensPositionFrom(raw: String): LensPosition = when (raw.lowercase()) {
    "top" -> LensPosition.Top
    "bottom" -> LensPosition.Bottom
    "left" -> LensPosition.Left
    "right" -> LensPosition.Right
    "none" -> LensPosition.None
    else -> LensPosition.Top
}
