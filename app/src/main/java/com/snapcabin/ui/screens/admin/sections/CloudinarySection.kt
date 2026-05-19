package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun CloudinarySection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SetupHint(
            title = "What you'll need",
            url = "https://snapcabin.app/setup/cloudinary",
            bullets = listOf(
                "A Cloudinary account (the free tier covers most events)",
                "Your cloud name — top of the Cloudinary dashboard",
                "An unsigned upload preset (Settings → Upload → Add upload preset, Signing Mode = Unsigned)"
            )
        )

        SettingRow("Upload photos to Cloudinary before SMS") {
            Switch(
                checked = settings.cloudinaryEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(cloudinaryEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.cloudinaryEnabled) {
            var name by remember { mutableStateOf(settings.cloudinaryCloudName) }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.trim()
                    viewModel.updateSetting { copy(cloudinaryCloudName = name) }
                },
                label = { Text("Cloud name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            var preset by remember { mutableStateOf(settings.cloudinaryUploadPreset) }
            OutlinedTextField(
                value = preset,
                onValueChange = {
                    preset = it.trim()
                    viewModel.updateSetting { copy(cloudinaryUploadPreset = preset) }
                },
                label = { Text("Unsigned upload preset name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            Text(
                text = "For safety: restrict the preset to image/* and cap file size around 10 MB. The preset name above must match exactly. Without Cloudinary, SMS falls back to a local-WiFi link only.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
        }
    }
}
