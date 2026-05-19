package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun KioskSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        var pinText by remember { mutableStateOf(settings.adminPin) }
        OutlinedTextField(
            value = pinText,
            onValueChange = { newPin ->
                pinText = newPin.filter { c -> c.isDigit() }.take(8)
                if (pinText.length >= 4) {
                    viewModel.updateSetting { copy(adminPin = pinText) }
                }
            },
            label = { Text("Admin PIN (min 4 digits)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )

        SettingRow("Kiosk Mode") {
            Switch(
                checked = settings.kioskModeEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(kioskModeEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Idle Timeout: ${settings.inactivityTimeoutSeconds}s") {
            Slider(
                value = settings.inactivityTimeoutSeconds.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(inactivityTimeoutSeconds = v.toInt()) } },
                valueRange = 15f..300f,
                steps = 18,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
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
