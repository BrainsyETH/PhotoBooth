package com.snapcabin.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.FrankRuhlLibre

@Composable
fun CoachLine(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = FrankRuhlLibre,
            fontWeight = FontWeight.Medium,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            color = Cream,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.55f),
                blurRadius = 24f
            )
        )
    )
}
