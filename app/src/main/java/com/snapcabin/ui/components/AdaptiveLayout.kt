package com.snapcabin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Utility for adaptive layouts across different tablet sizes.
 * Samsung Galaxy Tab S series: 10.5"-12.4" screens, 2560x1600 or 2800x1752
 * Samsung Galaxy Tab A series: 8.7"-10.5" screens, lower resolutions
 */
data class ScreenClass(
    val widthDp: Int,
    val heightDp: Int,
    val isLandscape: Boolean,
    val sizeCategory: SizeCategory
)

enum class SizeCategory {
    COMPACT,    // < 600dp width (phones)
    MEDIUM,     // 600-839dp (small tablets, Tab A 8.7)
    EXPANDED,   // 840-1199dp (Tab A 10.5, Tab S7)
    LARGE       // >= 1200dp (Tab S8+, Tab S9 Ultra)
}

@Composable
fun rememberScreenClass(): ScreenClass {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    val heightDp = config.screenHeightDp
    val isLandscape = widthDp > heightDp

    val category = when {
        widthDp < 600 -> SizeCategory.COMPACT
        widthDp < 840 -> SizeCategory.MEDIUM
        widthDp < 1200 -> SizeCategory.EXPANDED
        else -> SizeCategory.LARGE
    }

    return ScreenClass(
        widthDp = widthDp,
        heightDp = heightDp,
        isLandscape = isLandscape,
        sizeCategory = category
    )
}

/**
 * Returns scaled dp values based on screen size.
 * Base values designed for 10.5" tablet in landscape.
 */
fun ScreenClass.scaledDp(baseDp: Int): Int {
    val scale = when (sizeCategory) {
        SizeCategory.COMPACT -> 0.7f
        SizeCategory.MEDIUM -> 0.85f
        SizeCategory.EXPANDED -> 1.0f
        SizeCategory.LARGE -> 1.15f
    }
    return (baseDp * scale).toInt()
}

/**
 * Returns sidebar width appropriate for screen size
 */
fun ScreenClass.sidebarWidth(): Int {
    return when (sizeCategory) {
        SizeCategory.COMPACT -> 280
        SizeCategory.MEDIUM -> 320
        SizeCategory.EXPANDED -> 360
        SizeCategory.LARGE -> 400
    }
}

/**
 * Returns button height appropriate for screen size and touch targets.
 * Ensures minimum 48dp for accessibility on all devices.
 */
fun ScreenClass.buttonHeight(): Int {
    return when (sizeCategory) {
        SizeCategory.COMPACT -> 56
        SizeCategory.MEDIUM -> 64
        SizeCategory.EXPANDED -> 80
        SizeCategory.LARGE -> 88
    }
}
