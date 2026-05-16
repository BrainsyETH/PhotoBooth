package com.snapcabin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CabinPrimary,
    onPrimary = CabinOnPrimary,
    primaryContainer = CabinPrimaryVariant,
    onPrimaryContainer = CabinOnPrimary,
    secondary = CabinSecondary,
    onSecondary = CabinOnPrimary,
    tertiary = CabinAccent,
    onTertiary = Espresso,
    background = CabinBackground,
    onBackground = CabinOnBackground,
    surface = CabinSurface,
    onSurface = CabinOnSurface,
    surfaceVariant = CabinSurfaceRecessed,
    onSurfaceVariant = Espresso,
    outline = CabinLineStrong,
    outlineVariant = CabinLine,
    error = Clay,
    onError = CabinOnPrimary
)

@Composable
fun SnapCabinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CabinTypography,
        shapes = CabinShapes,
        content = content
    )
}
