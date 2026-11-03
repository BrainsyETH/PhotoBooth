package com.snapcabin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Wraps content and monitors for user inactivity.
 * Shows a warning dialog [warningMs] before the timeout fires.
 * Calls [onTimeout] when no touch events occur for [timeoutMs].
 *
 * Pass a stable [resetKey] (e.g. the current route) to restart the
 * timer whenever it changes.
 */
/**
 * "The guest is still here." Touches in the main window reset the idle timer
 * automatically, but dialogs are separate windows the handler never sees — a
 * guest typing an email in the Share dialog was timed out mid-address (and the
 * "Still there?" warning drew BEHIND the dialog). Screens call this from such
 * inputs.
 */
val LocalUserActivity = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun InactivityHandler(
    timeoutMs: Long,
    warningMs: Long = 15_000L,
    enabled: Boolean = true,
    resetKey: Any? = Unit,
    onTimeout: () -> Unit,
    content: @Composable () -> Unit
) {
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showWarning by remember { mutableStateOf(false) }
    var warningCountdown by remember { mutableIntStateOf(0) }

    fun resetTimer() {
        lastInteraction = System.currentTimeMillis()
        showWarning = false
    }

    // Restart timer when the caller-supplied key changes (e.g. on route change).
    LaunchedEffect(resetKey) { resetTimer() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Observe pointer events on the Initial pass so we see every touch,
            // even ones that children (buttons, sliders) consume.
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        resetTimer()
                    }
                }
            }
    ) {
        val markActivity: () -> Unit = remember { { resetTimer() } }
        CompositionLocalProvider(LocalUserActivity provides markActivity) {
            content()
        }

        if (showWarning && enabled) {
            InactivityWarningDialog(
                remainingSeconds = warningCountdown,
                onDismiss = { resetTimer() }
            )
        }
    }

    LaunchedEffect(enabled, timeoutMs, warningMs) {
        if (!enabled || timeoutMs <= 0) return@LaunchedEffect
        val effectiveWarningMs = warningMs.coerceAtMost(timeoutMs - 1000L).coerceAtLeast(0L)
        val warningThreshold = timeoutMs - effectiveWarningMs

        while (true) {
            delay(1000)
            val elapsed = System.currentTimeMillis() - lastInteraction
            if (elapsed >= timeoutMs) {
                showWarning = false
                onTimeout()
                resetTimer()
            } else if (effectiveWarningMs > 0 && elapsed >= warningThreshold) {
                showWarning = true
                warningCountdown = ((timeoutMs - elapsed) / 1000).toInt().coerceAtLeast(1)
            } else {
                showWarning = false
            }
        }
    }
}
