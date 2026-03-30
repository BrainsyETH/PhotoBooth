package com.snapcabin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CabinPrimary,
    secondary = CabinSecondary,
    background = CabinBackground,
    surface = CabinSurface,
    onPrimary = CabinOnPrimary,
    onBackground = CabinOnBackground,
    onSurface = CabinOnSurface
)

@Composable
fun SnapCabinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CabinTypography,
        content = content
    )
}
