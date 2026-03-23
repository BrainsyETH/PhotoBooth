package com.photobooth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BoothPrimary,
    secondary = BoothSecondary,
    background = BoothBackground,
    surface = BoothSurface,
    onPrimary = BoothOnPrimary,
    onBackground = BoothOnBackground,
    onSurface = BoothOnSurface
)

@Composable
fun PhotoBoothTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = BoothTypography,
        content = content
    )
}
