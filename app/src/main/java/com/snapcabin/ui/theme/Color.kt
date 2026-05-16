package com.snapcabin.ui.theme

import androidx.compose.ui.graphics.Color

// Light farmhouse parlor palette
// Parchment & cream backgrounds, pine + walnut + honey actions, espresso ink.
val Parchment = Color(0xFFF6EFE0)       // Page background — warm cream
val Cream = Color(0xFFFBF6EA)           // Elevated card / panel surface
val Oat = Color(0xFFEBE2CC)             // Recessed surface, slider tracks
val Pine = Color(0xFF3F5A3E)            // Primary action — deep moss green
val PineDeep = Color(0xFF2D4530)        // Pressed primary
val Walnut = Color(0xFF6B4F35)          // Secondary action — warm wood
val WalnutDeep = Color(0xFF4D3924)      // Secondary text / pressed
val Honey = Color(0xFFD4A24A)           // Accent — selection / amber pop
val HoneyDeep = Color(0xFFB27F1E)       // High-contrast amber on light
val Clay = Color(0xFFB8633F)            // Warm pop — error / clay CTA
val Sage = Color(0xFF8FA98A)            // Soft tertiary accent
val Espresso = Color(0xFF2A1F13)        // Primary text — ink
val Mist = Color(0xFF9D9281)            // Muted text / hints

// Token aliases (kept stable across the dark → light pivot so existing
// screen code keeps working). The names are dark-theme legacy; values are now light.
val CabinPrimary = Pine
val CabinPrimaryVariant = PineDeep
val CabinSecondary = Walnut
val CabinBackground = Parchment
val CabinSurface = Cream
val CabinSurfaceRecessed = Oat
val CabinOnPrimary = Color.White
val CabinOnBackground = Espresso
val CabinOnSurface = Espresso
val CabinAccent = Honey
val CabinMuted = Mist
val CabinLine = Color(0x1A2A1F13)       // espresso @ 10% — hairline border
val CabinLineStrong = Color(0x382A1F13) // espresso @ 22%
