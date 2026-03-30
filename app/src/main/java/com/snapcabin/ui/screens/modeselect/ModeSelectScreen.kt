package com.snapcabin.ui.screens.modeselect

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinOnBackground
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSecondary
import com.snapcabin.ui.theme.CabinSurface

@Composable
fun ModeSelectScreen(
    onSinglePhoto: () -> Unit,
    onCollage: () -> Unit,
    onGif: () -> Unit
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
                text = "Choose Your Mode",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = CabinOnBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ModeCard(
                    title = "Photo",
                    description = "One perfect shot",
                    accentColor = CabinSecondary,
                    onClick = onSinglePhoto
                )
                ModeCard(
                    title = "Collage",
                    description = "Multiple photos, one layout",
                    accentColor = CabinPrimary,
                    onClick = onCollage
                )
                ModeCard(
                    title = "GIF",
                    description = "Animated sequence",
                    accentColor = CabinAccent,
                    onClick = onGif
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CabinSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 32.dp)
            .width(180.dp)
    ) {
        // Minimal accent indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = CabinOnBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}
