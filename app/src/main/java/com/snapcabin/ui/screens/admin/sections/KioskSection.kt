package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun KioskSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        PinChangeBlock(settings = settings, viewModel = viewModel)

        SettingRow("Kiosk Mode") {
            Switch(
                checked = settings.kioskModeEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(kioskModeEnabled = v) } },
                colors = adminSwitchColors()
            )
        }
        Text(
            text = "Locks the tablet to SnapCabin for the event: the status bar, navigation buttons, and other apps are blocked, and the screen stays on while it's plugged in. You can always get back out from here — turn this off, or tap “Exit kiosk mode” below (you're already past the PIN). For the strongest lock-down, a one-time setup with a computer is needed; without it this is a best-effort lock that some tablets can still be backed out of.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )

        if (settings.kioskModeEnabled) {
            var exitMessage by remember { mutableStateOf("") }
            BigButton(
                text = "EXIT KIOSK MODE",
                onClick = {
                    // Flipping the setting off makes MainActivity release
                    // lock-task; the host can then leave the app or press home.
                    viewModel.updateSetting { copy(kioskModeEnabled = false) }
                    exitMessage = "Unlocked. You can now leave the app or press home."
                },
                variant = BigButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            if (exitMessage.isNotEmpty()) {
                Text(
                    text = exitMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Pine,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radii.s))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                .padding(Spacing.md)
        ) {
            Text(
                "Idle timeout",
                style = MaterialTheme.typography.bodyLarge,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (secs, label) ->
                    BigButton(
                        text = label,
                        onClick = { viewModel.updateSetting { copy(inactivityTimeoutSeconds = secs) } },
                        variant = if (settings.inactivityTimeoutSeconds == secs) BigButtonVariant.Accent else BigButtonVariant.Surface
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.s))
            Text(
                text = "How long a screen waits without a tap before resetting to the welcome screen. Capture allows 1.5× this; quick choices (mode select, review) reset at half.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
            )
        }

        SettingRow("Screen Brightness: ${(settings.screenBrightness * 100).toInt()}%") {
            Slider(
                value = settings.screenBrightness,
                onValueChange = { v -> viewModel.updateSetting { copy(screenBrightness = v) } },
                valueRange = 0.1f..1f,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
        }
    }
}

/**
 * Deliberate-action PIN change: type it twice, visible, committed only by the
 * SET PIN button. The old field saved every keystroke once it hit 4 digits —
 * an invisible mid-edit typo could lock the operator out of their own booth,
 * and recovery means wiping app data (the whole configuration).
 */
@Composable
private fun PinChangeBlock(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf("") }

    val pinsMatch = newPin.length >= 4 && newPin == confirmPin
    val showMismatch = confirmPin.isNotEmpty() && !pinsMatch

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = "Admin PIN",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Pine
        )
        Text(
            text = if (settings.adminPin == "1234") {
                "Currently the default (1234) — set your own before the event."
            } else {
                "PIN is set. Enter a new one twice to change it."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.72f)
        )

        // Shown in the clear on purpose: you're already behind the PIN gate,
        // and seeing what you type prevents the lockout typo.
        OutlinedTextField(
            value = newPin,
            onValueChange = {
                newPin = it.filter { c -> c.isDigit() }.take(8)
                savedMessage = ""
            },
            label = { Text("New PIN (4–8 digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                confirmPin = it.filter { c -> c.isDigit() }.take(8)
                savedMessage = ""
            },
            label = { Text("Repeat new PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = showMismatch,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )
        if (showMismatch) {
            Text(
                text = "PINs don't match yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            BigButton(
                text = "SET PIN",
                onClick = {
                    viewModel.updateSetting { copy(adminPin = newPin) }
                    savedMessage = "PIN updated."
                    newPin = ""
                    confirmPin = ""
                },
                variant = BigButtonVariant.Primary,
                enabled = pinsMatch
            )
            if (savedMessage.isNotEmpty()) {
                Text(
                    text = savedMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Pine
                )
            }
        }
    }
}
