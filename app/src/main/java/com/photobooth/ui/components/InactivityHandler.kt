package com.photobooth.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Wraps content and monitors for user inactivity.
 * Calls [onTimeout] when no touch events occur for [timeoutMs].
 */
@Composable
fun InactivityHandler(
    timeoutMs: Long,
    enabled: Boolean = true,
    onTimeout: () -> Unit,
    content: @Composable () -> Unit
) {
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Reset on any touch
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            lastInteraction = System.currentTimeMillis()
                            tryAwaitRelease()
                        }
                    )
                }
            }
    ) {
        content()
    }

    // Timeout checker
    LaunchedEffect(enabled, timeoutMs) {
        if (!enabled || timeoutMs <= 0) return@LaunchedEffect
        while (true) {
            delay(1000)
            val elapsed = System.currentTimeMillis() - lastInteraction
            if (elapsed >= timeoutMs) {
                onTimeout()
                lastInteraction = System.currentTimeMillis() // Reset after timeout
            }
        }
    }
}
