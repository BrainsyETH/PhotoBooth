package com.snapcabin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.snapcabin.settings.PhotoResolution
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSecondary

@Composable
fun AdminScreen(
    onDismiss: () -> Unit,
    onPrivacyPolicy: () -> Unit = {},
    onGallery: () -> Unit = {},
    viewModel: AdminViewModel
) {
    val pinVerified by viewModel.pinVerified.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val cameras by viewModel.availableCameras.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!pinVerified) {
            PinEntry(
                onPinSubmit = { viewModel.verifyPin(it) },
                onCancel = onDismiss
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Settings list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- CAMERA SECTION ---
                    item { SectionHeader("Camera") }

                    item {
                        SettingRow("Use Front Camera") {
                            Switch(
                                checked = settings.useFrontCamera,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(useFrontCamera = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("Mirror Front Camera") {
                            Switch(
                                checked = settings.mirrorFrontCamera,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(mirrorFrontCamera = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    // Camera selector
                    if (cameras.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "Available Cameras",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                cameras.forEach { cam ->
                                    val isSelected = settings.cameraId == cam.id ||
                                        (settings.cameraId.isEmpty() && !cam.isExternal && (
                                            (settings.useFrontCamera && cam.facing == "Front") ||
                                            (!settings.useFrontCamera && cam.facing == "Back")
                                        ))
                                    val label = "${cam.facing} (ID: ${cam.id})${if (cam.isExternal) " - External" else ""}"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BigButton(
                                            text = if (isSelected) "● $label" else "○ $label",
                                            onClick = {
                                                viewModel.updateSetting { copy(cameraId = cam.id) }
                                            },
                                            containerColor = if (isSelected) CabinPrimary else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                "Photo Resolution",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PhotoResolution.entries.forEach { res ->
                                    BigButton(
                                        text = res.label,
                                        onClick = {
                                            viewModel.updateSetting { copy(photoResolution = res) }
                                        },
                                        containerColor = if (settings.photoResolution == res) CabinAccent else MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }

                    // --- MODES SECTION ---
                    item { SectionHeader("Modes") }

                    item {
                        val enabledCount = listOf(
                            settings.enableSinglePhotoMode,
                            settings.enableCollageMode,
                            settings.enableGifMode
                        ).count { it }

                        SettingRow("Single Photo Mode") {
                            Switch(
                                checked = settings.enableSinglePhotoMode,
                                onCheckedChange = { v ->
                                    if (v || enabledCount > 1) {
                                        viewModel.updateSetting { copy(enableSinglePhotoMode = v) }
                                    }
                                },
                                colors = switchColors(),
                                enabled = !settings.enableSinglePhotoMode || enabledCount > 1
                            )
                        }
                    }

                    item {
                        val enabledCount = listOf(
                            settings.enableSinglePhotoMode,
                            settings.enableCollageMode,
                            settings.enableGifMode
                        ).count { it }

                        SettingRow("Collage Mode") {
                            Switch(
                                checked = settings.enableCollageMode,
                                onCheckedChange = { v ->
                                    if (v || enabledCount > 1) {
                                        viewModel.updateSetting { copy(enableCollageMode = v) }
                                    }
                                },
                                colors = switchColors(),
                                enabled = !settings.enableCollageMode || enabledCount > 1
                            )
                        }
                    }

                    item {
                        val enabledCount = listOf(
                            settings.enableSinglePhotoMode,
                            settings.enableCollageMode,
                            settings.enableGifMode
                        ).count { it }

                        SettingRow("GIF Mode") {
                            Switch(
                                checked = settings.enableGifMode,
                                onCheckedChange = { v ->
                                    if (v || enabledCount > 1) {
                                        viewModel.updateSetting { copy(enableGifMode = v) }
                                    }
                                },
                                colors = switchColors(),
                                enabled = !settings.enableGifMode || enabledCount > 1
                            )
                        }
                    }

                    // --- CAPTURE SECTION ---
                    item { SectionHeader("Capture") }

                    item {
                        SettingRow("Countdown: ${settings.countdownSeconds}s") {
                            Slider(
                                value = settings.countdownSeconds.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(countdownSeconds = v.toInt()) }
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = CabinAccent, activeTrackColor = CabinPrimary)
                            )
                        }
                    }

                    item {
                        SettingRow("Flash Effect") {
                            Switch(
                                checked = settings.showFlashEffect,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(showFlashEffect = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("Review Auto-Accept: ${if (settings.reviewAutoAcceptSeconds == 0) "Off" else "${settings.reviewAutoAcceptSeconds}s"}") {
                            Slider(
                                value = settings.reviewAutoAcceptSeconds.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(reviewAutoAcceptSeconds = v.toInt()) }
                                },
                                valueRange = 0f..30f,
                                steps = 29,
                                modifier = Modifier.width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = CabinAccent, activeTrackColor = CabinPrimary)
                            )
                        }
                    }

                    // --- SOUND SECTION ---
                    item { SectionHeader("Sound") }

                    item {
                        SettingRow("Sound Enabled") {
                            Switch(
                                checked = settings.soundEnabled,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(soundEnabled = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("Shutter Sound") {
                            Switch(
                                checked = settings.shutterSoundEnabled,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(shutterSoundEnabled = v) }
                                },
                                colors = switchColors(),
                                enabled = settings.soundEnabled
                            )
                        }
                    }

                    item {
                        SettingRow("Countdown Beep") {
                            Switch(
                                checked = settings.countdownBeepEnabled,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(countdownBeepEnabled = v) }
                                },
                                colors = switchColors(),
                                enabled = settings.soundEnabled
                            )
                        }
                    }

                    // --- SHARING SECTION ---
                    item { SectionHeader("Sharing") }

                    item {
                        SettingRow("Auto-Save to Gallery") {
                            Switch(
                                checked = settings.autoSaveToGallery,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(autoSaveToGallery = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("QR Code Sharing") {
                            Switch(
                                checked = settings.enableQrSharing,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(enableQrSharing = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("JPEG Quality: ${settings.outputQuality}%") {
                            Slider(
                                value = settings.outputQuality.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(outputQuality = v.toInt()) }
                                },
                                valueRange = 50f..100f,
                                steps = 9,
                                modifier = Modifier.width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = CabinAccent, activeTrackColor = CabinPrimary)
                            )
                        }
                    }

                    // --- KIOSK SECTION ---
                    item { SectionHeader("Kiosk") }

                    item {
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CabinAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = CabinAccent,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = CabinAccent
                            )
                        )
                    }

                    item {
                        SettingRow("Kiosk Mode") {
                            Switch(
                                checked = settings.kioskModeEnabled,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(kioskModeEnabled = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    item {
                        SettingRow("Idle Timeout: ${settings.inactivityTimeoutSeconds}s") {
                            Slider(
                                value = settings.inactivityTimeoutSeconds.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(inactivityTimeoutSeconds = v.toInt()) }
                                },
                                valueRange = 15f..300f,
                                steps = 18,
                                modifier = Modifier.width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = CabinAccent, activeTrackColor = CabinPrimary)
                            )
                        }
                    }

                    item {
                        SettingRow("Screen Brightness: ${(settings.screenBrightness * 100).toInt()}%") {
                            Slider(
                                value = settings.screenBrightness,
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(screenBrightness = v) }
                                },
                                valueRange = 0.1f..1f,
                                modifier = Modifier.width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = CabinAccent, activeTrackColor = CabinPrimary)
                            )
                        }
                    }

                    // --- BRANDING SECTION ---
                    item { SectionHeader("Branding") }

                    item {
                        SettingRow("Watermark") {
                            Switch(
                                checked = settings.watermarkEnabled,
                                onCheckedChange = { v ->
                                    viewModel.updateSetting { copy(watermarkEnabled = v) }
                                },
                                colors = switchColors()
                            )
                        }
                    }

                    if (settings.watermarkEnabled) {
                        item {
                            var text by remember { mutableStateOf(settings.watermarkText) }
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    viewModel.updateSetting { copy(watermarkText = it) }
                                },
                                label = { Text("Watermark Text") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CabinAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = CabinAccent,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    cursorColor = CabinAccent
                                )
                            )
                        }
                    }

                    // --- TOOLS ---
                    item { SectionHeader("Tools") }

                    item {
                        BigButton(
                            text = "PHOTO GALLERY",
                            onClick = onGallery,
                            containerColor = CabinPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // --- ABOUT / PRIVACY ---
                    item { SectionHeader("About") }

                    item {
                        BigButton(
                            text = "PRIVACY POLICY",
                            onClick = onPrivacyPolicy,
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        SettingRow("Version") {
                            Text(
                                text = "1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Close button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        BigButton(
                            text = "CLOSE SETTINGS",
                            onClick = onDismiss,
                            containerColor = CabinSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEntry(
    onPinSubmit: (String) -> Boolean,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(48.dp)
        ) {
            Text(
                text = "Admin PIN",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter { c -> c.isDigit() }.take(8)
                    error = false
                },
                label = { Text("Enter PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CabinAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CabinAccent,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = CabinAccent
                )
            )

            if (error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Incorrect PIN",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigButton(
                    text = "CANCEL",
                    onClick = onCancel,
                    containerColor = MaterialTheme.colorScheme.surface
                )
                BigButton(
                    text = "ENTER",
                    onClick = {
                        if (!onPinSubmit(pin)) {
                            error = true
                            pin = ""
                        }
                    },
                    containerColor = CabinPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = CabinAccent,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = CabinAccent,
    checkedTrackColor = CabinPrimary,
    uncheckedThumbColor = Color.Gray,
    uncheckedTrackColor = Color.DarkGray
)
