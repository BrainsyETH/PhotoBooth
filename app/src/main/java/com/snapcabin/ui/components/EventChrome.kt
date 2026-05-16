package com.snapcabin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey

@Composable
fun EventChrome(
    visible: Boolean,
    eventName: String,
    hashtag: String,
    monogram: String,
    modifier: Modifier = Modifier
) {
    if (!visible || eventName.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Espresso.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Honey),
                contentAlignment = Alignment.Center
            ) {
                if (monogram.isNotBlank()) {
                    Text(
                        text = monogram.take(1),
                        fontFamily = FrankRuhlLibre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Espresso
                    )
                }
            }
            Text(
                text = eventName,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = Color.White
            )
        }

        if (hashtag.isNotBlank()) {
            Text(
                text = hashtag.uppercase(),
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.22f.em,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
