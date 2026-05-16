package com.snapcabin.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.snapcabin.ui.theme.Walnut

/**
 * Section eyebrow — uppercase, tracked, walnut by default.
 * Mirrors the titleMedium type role from the design system.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Walnut
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier
    )
}
