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
import androidx.compose.ui.unit.dp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.PhotoResolution
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.CameraInfo
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun CameraSection(
    settings: BoothSettings,
    cameras: List<CameraInfo>,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("Use Front Camera") {
            Switch(
                checked = settings.useFrontCamera,
                onCheckedChange = { v -> viewModel.updateSetting { copy(useFrontCamera = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Mirror Image") {
            Switch(
                checked = settings.mirrorImage,
                onCheckedChange = { v -> viewModel.updateSetting { copy(mirrorImage = v) } },
                colors = adminSwitchColors()
            )
        }

        if (cameras.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radii.s))
                    .background(Cream)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                    .padding(Spacing.md)
            ) {
                Text(
                    "Available Cameras",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Espresso.copy(alpha = 0.72f),
                    modifier = Modifier.padding(bottom = Spacing.s)
                )
                cameras.forEach { cam ->
                    val isSelected = settings.cameraId == cam.id ||
                        (settings.cameraId.isEmpty() && !cam.isExternal && (
                            (settings.useFrontCamera && cam.facing == "Front") ||
                                (!settings.useFrontCamera && cam.facing == "Back")
                            ))
                    val label = "${cam.facing} (ID: ${cam.id})${if (cam.isExternal) " — External" else ""}"

                    BigButton(
                        text = if (isSelected) "● $label" else "○ $label",
                        onClick = { viewModel.updateSetting { copy(cameraId = cam.id) } },
                        variant = if (isSelected) BigButtonVariant.Primary else BigButtonVariant.Surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs)
                    )
                }
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
                "Photo Resolution",
                style = MaterialTheme.typography.bodyLarge,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                PhotoResolution.entries.forEach { res ->
                    BigButton(
                        text = res.label,
                        onClick = { viewModel.updateSetting { copy(photoResolution = res) } },
                        variant = if (settings.photoResolution == res) BigButtonVariant.Accent else BigButtonVariant.Surface
                    )
                }
            }
        }
    }
}

@Composable
internal fun ModesSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    val enabledCount = listOf(
        settings.enableSinglePhotoMode,
        settings.enableCollageMode,
        settings.enableGifMode
    ).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("Single Photo Mode") {
            Switch(
                checked = settings.enableSinglePhotoMode,
                onCheckedChange = { v ->
                    if (v || enabledCount > 1) {
                        viewModel.updateSetting { copy(enableSinglePhotoMode = v) }
                    }
                },
                colors = adminSwitchColors(),
                enabled = !settings.enableSinglePhotoMode || enabledCount > 1
            )
        }

        SettingRow("Collage Mode") {
            Switch(
                checked = settings.enableCollageMode,
                onCheckedChange = { v ->
                    if (v || enabledCount > 1) {
                        viewModel.updateSetting { copy(enableCollageMode = v) }
                    }
                },
                colors = adminSwitchColors(),
                enabled = !settings.enableCollageMode || enabledCount > 1
            )
        }

        SettingRow("GIF Mode") {
            Switch(
                checked = settings.enableGifMode,
                onCheckedChange = { v ->
                    if (v || enabledCount > 1) {
                        viewModel.updateSetting { copy(enableGifMode = v) }
                    }
                },
                colors = adminSwitchColors(),
                enabled = !settings.enableGifMode || enabledCount > 1
            )
        }

        Text(
            text = "At least one mode must stay enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
    }
}

@Composable
internal fun CaptureSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SettingRow("Countdown: ${settings.countdownSeconds}s") {
            Slider(
                value = settings.countdownSeconds.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(countdownSeconds = v.toInt()) } },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
        }

        SettingRow("Collage Shots: ${settings.collageShotCount}") {
            Slider(
                value = settings.collageShotCount.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(collageShotCount = v.toInt()) } },
                valueRange = 2f..6f,
                steps = 3,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
        }

        SettingRow("GIF Frames: ${settings.gifFrameCount}") {
            Slider(
                value = settings.gifFrameCount.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(gifFrameCount = v.toInt()) } },
                valueRange = 4f..12f,
                steps = 7,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
        }

        SettingRow("Coaching microcopy") {
            Switch(
                checked = settings.coachingEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(coachingEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Framing guide") {
            Switch(
                checked = settings.framingGuideEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(framingGuideEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radii.s))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                .padding(Spacing.md)
        ) {
            Text("Flash Color", style = MaterialTheme.typography.bodyLarge, color = Espresso)
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                val flashOptions = listOf(
                    "Cream" to "#FDFAF1",
                    "Champagne" to "#C9A86A",
                    "White" to "#FFFFFF"
                )
                flashOptions.forEach { (label, hex) ->
                    val isSel = settings.flashColor.equals(hex, ignoreCase = true)
                    BigButton(
                        text = label,
                        onClick = { viewModel.updateSetting { copy(flashColor = hex) } },
                        variant = if (isSel) BigButtonVariant.Accent else BigButtonVariant.Surface
                    )
                }
            }
        }

        SettingRow("Flash Effect") {
            Switch(
                checked = settings.showFlashEffect,
                onCheckedChange = { v -> viewModel.updateSetting { copy(showFlashEffect = v) } },
                colors = adminSwitchColors()
            )
        }

        SettingRow("Review Auto-Accept: ${if (settings.reviewAutoAcceptSeconds == 0) "Off" else "${settings.reviewAutoAcceptSeconds}s"}") {
            Slider(
                value = settings.reviewAutoAcceptSeconds.toFloat(),
                onValueChange = { v -> viewModel.updateSetting { copy(reviewAutoAcceptSeconds = v.toInt()) } },
                valueRange = 0f..30f,
                steps = 29,
                modifier = Modifier.width(200.dp),
                colors = adminSliderColors()
            )
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
                "Camera lens position (\"Look here\" hint)",
                style = MaterialTheme.typography.bodyLarge,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf("Top", "Bottom", "Left", "Right", "None").forEach { pos ->
                    val key = pos.lowercase()
                    val sel = settings.cameraLensPosition.equals(key, ignoreCase = true)
                    BigButton(
                        text = pos,
                        onClick = { viewModel.updateSetting { copy(cameraLensPosition = key) } },
                        variant = if (sel) BigButtonVariant.Accent else BigButtonVariant.Surface
                    )
                }
            }
        }

        PromptField(
            initialValue = settings.posePromptsCollage,
            label = "Collage pose prompts (separate with ||)",
            placeholder = "Group hug! || Goofy face || Strike a pose",
            onChange = { viewModel.updateSetting { copy(posePromptsCollage = it) } }
        )

        PromptField(
            initialValue = settings.posePromptsGif,
            label = "GIF frame prompts (separate with ||)",
            placeholder = "Wave! || Dance! || Surprised face",
            onChange = { viewModel.updateSetting { copy(posePromptsGif = it) } }
        )
    }
}

@Composable
private fun PromptField(
    initialValue: String,
    label: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    OutlinedTextField(
        value = value,
        onValueChange = {
            value = it
            onChange(it)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = false,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.s),
        colors = adminTextFieldColors()
    )
}
