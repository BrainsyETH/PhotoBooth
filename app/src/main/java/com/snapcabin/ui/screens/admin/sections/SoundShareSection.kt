package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.StatusPill
import com.snapcabin.ui.screens.admin.StatusTone
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                BigButton(
                    text = "▶",
                    onClick = { viewModel.playShutterSample() },
                    variant = BigButtonVariant.Surface
                )
                Switch(
                    checked = settings.shutterSoundEnabled,
                    onCheckedChange = { v -> viewModel.updateSetting { copy(shutterSoundEnabled = v) } },
                    colors = adminSwitchColors(),
                    enabled = settings.soundEnabled
                )
            }
        }

        SettingRow("Countdown Beep") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                BigButton(
                    text = "▶",
                    onClick = { viewModel.playBeepSample() },
                    variant = BigButtonVariant.Surface
                )
                Switch(
                    checked = settings.countdownBeepEnabled,
                    onCheckedChange = { v -> viewModel.updateSetting { copy(countdownBeepEnabled = v) } },
                    colors = adminSwitchColors(),
                    enabled = settings.soundEnabled
                )
            }
        }

        Text(
            text = "▶ plays a sample at the volume guests will hear.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
    }
}

@Composable
internal fun ShareSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    val emailReady = settings.resendEnabled &&
        settings.resendApiKey.isNotBlank() && settings.resendFromAddress.isNotBlank()
    val qrBackendReady = settings.cloudinaryEnabled &&
        settings.cloudinaryCloudName.isNotBlank() && settings.cloudinaryUploadPreset.isNotBlank()

    // Rows mirror the share screen's button order: QR, Email, Save, Print, Share.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("QR code tile") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                StatusPill(
                    text = when {
                        !settings.enableQrSharing -> "Off"
                        qrBackendReady -> "Ready"
                        else -> "Needs setup"
                    },
                    tone = when {
                        !settings.enableQrSharing -> StatusTone.Neutral
                        qrBackendReady -> StatusTone.Good
                        else -> StatusTone.Warn
                    }
                )
                Switch(
                    checked = settings.enableQrSharing,
                    onCheckedChange = { v -> viewModel.updateSetting { copy(enableQrSharing = v) } },
                    colors = adminSwitchColors()
                )
            }
        }

        // One switch, not two: the guest Email button is governed entirely by
        // EMAIL DELIVERY's enable. This row just reports it, so the operator
        // never wonders why a toggle here didn't make the button appear.
        SettingRow("Email button — managed in EMAIL DELIVERY") {
            StatusPill(
                text = when {
                    !settings.resendEnabled -> "Off"
                    emailReady -> "On"
                    else -> "Needs setup"
                },
                tone = when {
                    !settings.resendEnabled -> StatusTone.Neutral
                    emailReady -> StatusTone.Good
                    else -> StatusTone.Warn
                }
            )
        }

        SettingRow("Save to Gallery button") {
            Switch(
                checked = settings.enableSaveToGallery,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableSaveToGallery = v) } },
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
        Text(
            text = "Only turn on Print with a printer set up on this tablet — otherwise guests land in the Android print dialog.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )

        SettingRow("Share button (system picker)") {
            Switch(
                checked = settings.enableShareIntent,
                onCheckedChange = { v -> viewModel.updateSetting { copy(enableShareIntent = v) } },
                colors = adminSwitchColors()
            )
        }
        Text(
            text = "Opens Android's share sheet — only useful when apps are signed in on this tablet, which a locked-down kiosk usually doesn't have.",
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
