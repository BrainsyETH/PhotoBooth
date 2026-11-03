package com.snapcabin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

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
    onError = CabinOnPrimary,
    errorContainer = Clay.copy(alpha = 0.16f).compositeOverCream(),
    onErrorContainer = Espresso,
    // Material 3 draws dialogs, menus, snackbars and pickers from these
    // container/inverse roles. Left unset they fall back to the baseline
    // lavender-grey palette, which is why AlertDialogs (email entry, start
    // event, clear log) and the Share snackbar looked off-brand.
    surfaceContainerLowest = Cream,
    surfaceContainerLow = Cream,
    surfaceContainer = Cream,
    surfaceContainerHigh = Cream,
    surfaceContainerHighest = Oat,
    surfaceBright = Cream,
    surfaceDim = Oat,
    surfaceTint = Color.Transparent, // no lavender/sage wash on elevated surfaces
    secondaryContainer = Oat,
    onSecondaryContainer = Espresso,
    tertiaryContainer = Oat,
    onTertiaryContainer = Espresso,
    inverseSurface = Espresso,
    inverseOnSurface = Cream,
    inversePrimary = Sage
)

private fun Color.compositeOverCream(): Color = compositeOver(Cream)

@Composable
fun SnapCabinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CabinTypography,
        shapes = CabinShapes,
        content = content
    )
}
