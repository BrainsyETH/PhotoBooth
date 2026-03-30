package com.snapcabin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Shows a brief white flash effect to simulate a camera flash.
 * Set [trigger] to true to fire the flash.
 */
@Composable
fun FlashOverlay(
    trigger: Boolean,
    onFlashComplete: () -> Unit = {}
) {
    var showFlash by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            showFlash = true
            delay(150)
            showFlash = false
            onFlashComplete()
        }
    }

    AnimatedVisibility(
        visible = showFlash,
        enter = fadeIn(initialAlpha = 0.8f),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}
