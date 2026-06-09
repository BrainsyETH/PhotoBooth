package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun SoundSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("Sound Enabled") {
            Switch(
                checked = settings.soundEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(soundEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Shutter Sound") {
            Switch(
                checked = settings.shutterSoundEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(shutterSoundEnabled = v) } },
                colors = adminSwitchColors(),
                enabled = settings.soundEnabled
            )
        }

        SettingRow("Countdown Beep") {
            Switch(
                checked = settings.countdownBeepEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(countdownBeepEnabled = v) } },
                colors = adminSwitchColors(),
                enabled = settings.soundEnabled
            )
        }
    }
}

@Composable
internal fun ShareSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("Save to Gallery button") {
            Switch(
                checked = settings.enableSaveToGallery,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableSaveToGallery = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Share button (system picker)") {
            Switch(
                checked = settings.enableShareIntent,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableShareIntent = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Print button") {
            Switch(
                checked = settings.enablePrint,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enablePrint = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Email button (Resend)") {
            Switch(
                checked = settings.enableEmail,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableEmail = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("QR code (Cloudinary link)") {
            Switch(
                checked = settings.enableQrSharing,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableQrSharing = v) } },
                colors = adminSwitchColors()
            )
        }

        Text(
            text = "QR code needs Cloudinary. Configure it under CLOUDINARY PHOTO HOSTING. The previous LAN-only QR option was removed because most mobile browsers now block plain-HTTP downloads.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )

        SettingRow("Auto-save every photo silently") {
            Switch(
                checked = settings.autoSaveToGallery,
                onCheckedChange = { v -> viewModel.updateSetting { copy(autoSaveToGallery = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("JPEG Quality: ${settings.outputQuality}%") {
            Slider(
                value = settings.outputQuality.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(outputQuality = v.toInt()) } },
                valueRange = 50f..100f,
                steps = 9,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
        }
    }
}
