package com.snapcabin.ui.theme

import androidx.compose.ui.graphics.Color

// Upscale wedding meets cabin warmth — ivory & champagne base, sage primary,
// warm taupe secondary, soft champagne-gold accent, gentle terracotta pop.
val Parchment = Color(0xFFFAF5EA)       // Page background — ivory / linen
val Cream = Color(0xFFFDFAF1)           // Elevated card / panel — near-white cream
val Oat = Color(0xFFEEE5CF)             // Recessed surface — soft sand
val Pine = Color(0xFF6B8F73)            // Primary action — fresh sage
val PineDeep = Color(0xFF52755A)        // Pressed primary — deeper sage
val Walnut = Color(0xFF8B7558)          // Secondary action — warm taupe
val WalnutDeep = Color(0xFF6B5840)      // Secondary text / pressed taupe
val Honey = Color(0xFFC9A86A)           // Accent — champagne gold
val HoneyDeep = Color(0xFFA8804A)       // High-contrast champagne on light
val Clay = Color(0xFFC4866A)            // Warm pop — soft terracotta
val Sage = Color(0xFFB5C6AD)            // Tertiary — pale sage
val Espresso = Color(0xFF322619)        // Primary text — warm ink
val Mist = Color(0xFFB5A892)            // Muted text / hints — soft warm grey

// Share-channel accents — softened for the wedding palette
val ShareDenim = Color(0xFF6B96B0)      // Email — slate blue
val ShareLeaf = Color(0xFF8DA887)       // Message — sage leaf

// Token aliases (kept stable across palette pivots so screen code keeps working).
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
val CabinLine = Color(0x1A322619)       // espresso @ 10% — hairline border
val CabinLineStrong = Color(0x38322619) // espresso @ 22%
