package com.snapcabin.ui.screens.modeselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.components.AnimatedMode
import com.snapcabin.ui.components.AnimatedModePreview
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSecondary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.HoneyDeep
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

private enum class ModeGlyph { Camera, Grid, FilmStrip }

private fun ModeGlyph.toAnimated(): AnimatedMode = when (this) {
    ModeGlyph.Camera -> AnimatedMode.Single
    ModeGlyph.Grid -> AnimatedMode.Collage
    ModeGlyph.FilmStrip -> AnimatedMode.Gif
}

@Composable
fun ModeSelectScreen(
    onSinglePhoto: () -> Unit,
    onCollage: () -> Unit,
    onGif: () -> Unit,
    singlePhotoEnabled: Boolean = true,
    collageEnabled: Boolean = true,
    gifEnabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.mode_title),
                fontSize = 56.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Medium,
                color = Espresso,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.015f).em
            )

            Spacer(modifier = Modifier.height(60.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
            ) {
                if (singlePhotoEnabled) {
                    ModeCard(
                        title = stringResource(R.string.mode_single_photo),
                        accentColor = CabinSecondary,
                        glyph = ModeGlyph.Camera,
                        onClick = onSinglePhoto
                    )
                }
                if (collageEnabled) {
                    ModeCard(
                        title = stringResource(R.string.mode_collage),
                        accentColor = CabinPrimary,
                        glyph = ModeGlyph.Grid,
                        onClick = onCollage
                    )
                }
                if (gifEnabled) {
                    ModeCard(
                        title = stringResource(R.string.mode_gif),
                        accentColor = HoneyDeep,
                        glyph = ModeGlyph.FilmStrip,
                        onClick = onGif
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    accentColor: Color,
    glyph: ModeGlyph,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(270.dp)
            .height(340.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(Radii.l))
            .clip(RoundedCornerShape(Radii.l))
            .background(CabinSurface)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.l))
            .clickable(onClick = onClick)
            .padding(Spacing.xl)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(Radii.m))
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedModePreview(
                mode = glyph.toAnimated(),
                color = accentColor,
                size = 56.dp
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = title,
            fontSize = 32.sp,
            fontFamily = FrankRuhlLibre,
            fontWeight = FontWeight.Bold,
            color = Espresso,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "START →",
            fontSize = 14.sp,
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 0.14f.em,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

