package com.snapcabin.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.snapcabin.event.EventSlug
import com.snapcabin.event.SendLog
import com.snapcabin.settings.BoothSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.snapcabin.settings.PhotoResolution
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Mist
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

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
            .background(Parchment)
    ) {
        if (!pinVerified) {
            PinEntry(
                onPinSubmit = { viewModel.verifyPin(it) },
                onCancel = onDismiss
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                item {
                    Text(
                        text = "Settings",
                        fontSize = 36.sp,
                        fontFamily = FrankRuhlLibre,
                        fontWeight = FontWeight.Bold,
                        color = Espresso,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                // EVENT (active event scopes Cloudinary folder, audit log, rate limits)
                item { AdminEyebrow("EVENT") }

                item { EventBlock(settings = settings, viewModel = viewModel) }

                // Default-PIN warning — surfaces only if the host hasn't changed it.
                if (settings.adminPin == "1234") {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radii.s))
                                .background(Clay.copy(alpha = 0.15f))
                                .border(1.dp, Clay, RoundedCornerShape(Radii.s))
                                .padding(Spacing.md)
                        ) {
                            Text(
                                text = "Admin PIN is still the default (1234).",
                                fontFamily = HankenGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Clay
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Change it under KIOSK before deploying. Anyone tapping the corner long-press can reach this screen with 1234.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Espresso.copy(alpha = 0.72f)
                            )
                        }
                    }
                }

                // CAMERA
                item { AdminEyebrow("CAMERA") }

                item {
                    SettingRow("Use Front Camera") {
                        Switch(
                            checked = settings.useFrontCamera,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(useFrontCamera = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Mirror Image") {
                        Switch(
                            checked = settings.mirrorImage,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(mirrorImage = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                if (cameras.isNotEmpty()) {
                    item {
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
                                    onClick = {
                                        viewModel.updateSetting { copy(cameraId = cam.id) }
                                    },
                                    variant = if (isSelected) BigButtonVariant.Primary else BigButtonVariant.Surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.xs)
                                )
                            }
                        }
                    }
                }

                item {
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
                                    onClick = {
                                        viewModel.updateSetting { copy(photoResolution = res) }
                                    },
                                    variant = if (settings.photoResolution == res) BigButtonVariant.Accent else BigButtonVariant.Surface
                                )
                            }
                        }
                    }
                }

                // MODES
                item { AdminEyebrow("MODES") }

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
                            colors = adminSwitchColors(),
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
                            colors = adminSwitchColors(),
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
                            colors = adminSwitchColors(),
                            enabled = !settings.enableGifMode || enabledCount > 1
                        )
                    }
                }

                // CAPTURE
                item { AdminEyebrow("CAPTURE") }

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
                            colors = adminSliderColors()
                        )
                    }
                }

                item {
                    SettingRow("Collage Shots: ${settings.collageShotCount}") {
                        Slider(
                            value = settings.collageShotCount.toFloat(),
                            onValueChange = { v ->
                                viewModel.updateSetting { copy(collageShotCount = v.toInt()) }
                            },
                            valueRange = 2f..6f,
                            steps = 3,
                            modifier = Modifier.width(200.dp),
                            colors = adminSliderColors()
                        )
                    }
                }

                item {
                    SettingRow("GIF Frames: ${settings.gifFrameCount}") {
                        Slider(
                            value = settings.gifFrameCount.toFloat(),
                            onValueChange = { v ->
                                viewModel.updateSetting { copy(gifFrameCount = v.toInt()) }
                            },
                            valueRange = 4f..12f,
                            steps = 7,
                            modifier = Modifier.width(200.dp),
                            colors = adminSliderColors()
                        )
                    }
                }

                item {
                    SettingRow("Coaching microcopy") {
                        Switch(
                            checked = settings.coachingEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(coachingEnabled = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Framing guide") {
                        Switch(
                            checked = settings.framingGuideEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(framingGuideEnabled = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radii.s))
                            .background(Cream)
                            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                            .padding(Spacing.md)
                    ) {
                        Text(
                            "Flash Color",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Espresso
                        )
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
                                    onClick = {
                                        viewModel.updateSetting { copy(flashColor = hex) }
                                    },
                                    variant = if (isSel) BigButtonVariant.Accent else BigButtonVariant.Surface
                                )
                            }
                        }
                    }
                }

                item {
                    SettingRow("Flash Effect") {
                        Switch(
                            checked = settings.showFlashEffect,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(showFlashEffect = v) }
                            },
                            colors = adminSwitchColors()
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
                            colors = adminSliderColors()
                        )
                    }
                }

                item {
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
                }

                item {
                    var prompts by remember { mutableStateOf(settings.posePromptsCollage) }
                    OutlinedTextField(
                        value = prompts,
                        onValueChange = {
                            prompts = it
                            viewModel.updateSetting { copy(posePromptsCollage = it) }
                        },
                        label = { Text("Collage pose prompts (separate with ||)") },
                        placeholder = { Text("Group hug! || Goofy face || Strike a pose") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                }

                item {
                    var prompts by remember { mutableStateOf(settings.posePromptsGif) }
                    OutlinedTextField(
                        value = prompts,
                        onValueChange = {
                            prompts = it
                            viewModel.updateSetting { copy(posePromptsGif = it) }
                        },
                        label = { Text("GIF frame prompts (separate with ||)") },
                        placeholder = { Text("Wave! || Dance! || Surprised face") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                }

                // SOUND
                item { AdminEyebrow("SOUND") }

                item {
                    SettingRow("Sound Enabled") {
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(soundEnabled = v) }
                            },
                            colors = adminSwitchColors()
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
                            colors = adminSwitchColors(),
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
                            colors = adminSwitchColors(),
                            enabled = settings.soundEnabled
                        )
                    }
                }

                // SHARE OPTIONS
                item { AdminEyebrow("SHARE OPTIONS") }

                item {
                    SettingRow("Save to Gallery button") {
                        Switch(
                            checked = settings.enableSaveToGallery,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enableSaveToGallery = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Share button (system picker)") {
                        Switch(
                            checked = settings.enableShareIntent,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enableShareIntent = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Print button") {
                        Switch(
                            checked = settings.enablePrint,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enablePrint = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Email button") {
                        Switch(
                            checked = settings.enableEmail,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enableEmail = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Message (SMS) button") {
                        Switch(
                            checked = settings.enableSms,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enableSms = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("QR code (scan to download)") {
                        Switch(
                            checked = settings.enableQrSharing,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(enableQrSharing = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                item {
                    SettingRow("Auto-save every photo silently") {
                        Switch(
                            checked = settings.autoSaveToGallery,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(autoSaveToGallery = v) }
                            },
                            colors = adminSwitchColors()
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
                            colors = adminSliderColors()
                        )
                    }
                }

                // TWILIO SMS
                item { AdminEyebrow("TWILIO SMS") }

                item {
                    SettingRow("Enable Twilio SMS sending") {
                        Switch(
                            checked = settings.twilioEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(twilioEnabled = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                if (settings.twilioEnabled) {
                    item {
                        var sid by remember { mutableStateOf(settings.twilioAccountSid) }
                        OutlinedTextField(
                            value = sid,
                            onValueChange = {
                                sid = it.trim()
                                viewModel.updateSetting { copy(twilioAccountSid = sid) }
                            },
                            label = { Text("Account SID (starts with AC...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radii.s),
                            colors = adminTextFieldColors()
                        )
                    }

                    item {
                        var token by remember { mutableStateOf(settings.twilioAuthToken) }
                        OutlinedTextField(
                            value = token,
                            onValueChange = {
                                token = it.trim()
                                viewModel.updateSetting { copy(twilioAuthToken = token) }
                            },
                            label = { Text("Auth Token") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radii.s),
                            colors = adminTextFieldColors()
                        )
                    }

                    item {
                        var from by remember { mutableStateOf(settings.twilioFromNumber) }
                        OutlinedTextField(
                            value = from,
                            onValueChange = {
                                from = it.trim()
                                viewModel.updateSetting { copy(twilioFromNumber = from) }
                            },
                            label = { Text("From number (E.164, e.g. +15551234567)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radii.s),
                            colors = adminTextFieldColors()
                        )
                    }

                    item {
                        var urlBase by remember { mutableStateOf(settings.twilioPhotoUrlBase) }
                        OutlinedTextField(
                            value = urlBase,
                            onValueChange = {
                                urlBase = it.trim()
                                viewModel.updateSetting { copy(twilioPhotoUrlBase = urlBase) }
                            },
                            label = { Text("Photo host base URL (optional, public)") },
                            placeholder = { Text("https://your-bucket.example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radii.s),
                            colors = adminTextFieldColors()
                        )
                    }

                    item {
                        SettingRow("Max SMS per session: ${settings.twilioMaxPerSession}") {
                            Slider(
                                value = settings.twilioMaxPerSession.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSetting { copy(twilioMaxPerSession = v.toInt()) }
                                },
                                valueRange = 1f..50f,
                                steps = 48,
                                modifier = Modifier.width(200.dp),
                                colors = adminSliderColors()
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Without a public photo host, the SMS includes the kiosk's local IP URL — guests must be on the same WiFi to open it. Configure Cloudinary below (or set the host URL above) to deliver as MMS over cellular.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = Spacing.xs)
                        )
                    }
                }

                // CLOUDINARY (public photo hosting for Twilio MMS)
                item { AdminEyebrow("CLOUDINARY PHOTO HOSTING") }

                item {
                    SettingRow("Upload photos to Cloudinary before SMS") {
                        Switch(
                            checked = settings.cloudinaryEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(cloudinaryEnabled = v) }
                            },
                            colors = adminSwitchColors()
                        )
                    }
                }

                if (settings.cloudinaryEnabled) {
                    item {
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
                    }

                    item {
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
                    }

                    item {
                        Text(
                            text = "Create the preset in Cloudinary → Settings → Upload → Add upload preset. Set Signing Mode to \"Unsigned\". For safety: restrict allowed formats to image/* and set a max file size (~10 MB). The preset name above must match exactly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = Spacing.xs)
                        )
                    }
                }

                // KIOSK
                item { AdminEyebrow("KIOSK") }

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
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                }

                item {
                    SettingRow("Kiosk Mode") {
                        Switch(
                            checked = settings.kioskModeEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(kioskModeEnabled = v) }
                            },
                            colors = adminSwitchColors()
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
                            colors = adminSliderColors()
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
                            colors = adminSliderColors()
                        )
                    }
                }

                // BRANDING
                item { AdminEyebrow("BRANDING") }

                item {
                    var name by remember { mutableStateOf(settings.eventName) }
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            viewModel.updateSetting { copy(eventName = it) }
                        },
                        label = { Text("Event Name (Attract headline)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                }

                item {
                    var sub by remember { mutableStateOf(settings.attractSubtext) }
                    OutlinedTextField(
                        value = sub,
                        onValueChange = {
                            sub = it
                            viewModel.updateSetting { copy(attractSubtext = it) }
                        },
                        label = { Text("Attract Subtext (tagline)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                }

                item {
                    SettingRow("Watermark") {
                        Switch(
                            checked = settings.watermarkEnabled,
                            onCheckedChange = { v ->
                                viewModel.updateSetting { copy(watermarkEnabled = v) }
                            },
                            colors = adminSwitchColors()
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
                            shape = RoundedCornerShape(Radii.s),
                            colors = adminTextFieldColors()
                        )
                    }
                }

                // Custom border image
                item {
                    val context = LocalContext.current
                    val borderLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            val file = copyUriToInternal(context, it, "custom_border.png")
                            file?.let { f ->
                                viewModel.updateSetting { copy(customBorderPath = f.absolutePath) }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Custom Border / Frame",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Espresso,
                                modifier = Modifier.weight(1f)
                            )
                            BigButton(
                                text = if (settings.customBorderPath.isNotEmpty()) "CHANGE" else "UPLOAD",
                                onClick = { borderLauncher.launch("image/*") },
                                variant = BigButtonVariant.Primary
                            )
                        }
                        if (settings.customBorderPath.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.s))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = java.io.File(settings.customBorderPath)
                                    ),
                                    contentDescription = "Custom border preview",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(Radii.xs))
                                )
                                BigButton(
                                    text = "REMOVE",
                                    onClick = {
                                        viewModel.updateSetting { copy(customBorderPath = "") }
                                    },
                                    variant = BigButtonVariant.Surface
                                )
                            }
                        }
                    }
                }

                // Custom overlay image
                item {
                    val context = LocalContext.current
                    val overlayLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            val file = copyUriToInternal(context, it, "custom_overlay.png")
                            file?.let { f ->
                                viewModel.updateSetting { copy(customOverlayPath = f.absolutePath) }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Custom Overlay / Logo",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Espresso,
                                modifier = Modifier.weight(1f)
                            )
                            BigButton(
                                text = if (settings.customOverlayPath.isNotEmpty()) "CHANGE" else "UPLOAD",
                                onClick = { overlayLauncher.launch("image/*") },
                                variant = BigButtonVariant.Primary
                            )
                        }
                        if (settings.customOverlayPath.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.s))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = java.io.File(settings.customOverlayPath)
                                    ),
                                    contentDescription = "Custom overlay preview",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(Radii.xs))
                                )
                                BigButton(
                                    text = "REMOVE",
                                    onClick = {
                                        viewModel.updateSetting { copy(customOverlayPath = "") }
                                    },
                                    variant = BigButtonVariant.Surface
                                )
                            }
                        }
                    }
                }

                // TOOLS
                // AUDIT LOG (last 500 sends)
                item { AdminEyebrow("AUDIT LOG") }

                item { SendLogBlock(settings = settings, viewModel = viewModel) }

                item { AdminEyebrow("TOOLS") }

                item {
                    BigButton(
                        text = "PHOTO GALLERY",
                        onClick = onGallery,
                        variant = BigButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ABOUT
                item { AdminEyebrow("ABOUT") }

                item {
                    BigButton(
                        text = "PRIVACY POLICY",
                        onClick = onPrivacyPolicy,
                        variant = BigButtonVariant.Surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    SettingRow("Version") {
                        Text(
                            text = "1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Espresso.copy(alpha = 0.72f)
                        )
                    }
                }

                // CLOSE
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    BigButton(
                        text = "CLOSE SETTINGS",
                        onClick = onDismiss,
                        variant = BigButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
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
                .clip(RoundedCornerShape(Radii.xl))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(Radii.xl))
                .padding(Spacing.xxl)
        ) {
            Text(
                text = "Admin PIN",
                fontSize = 36.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

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
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            if (error) {
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "Incorrect PIN",
                    color = Clay,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                BigButton(
                    text = "CANCEL",
                    onClick = onCancel,
                    variant = BigButtonVariant.Surface
                )
                BigButton(
                    text = "ENTER",
                    onClick = {
                        if (!onPinSubmit(pin)) {
                            error = true
                            pin = ""
                        }
                    },
                    variant = BigButtonVariant.Primary
                )
            }
        }
    }
}

@Composable
private fun AdminEyebrow(text: String) {
    Spacer(modifier = Modifier.height(Spacing.s))
    Eyebrow(text = text, color = Honey)
    Spacer(modifier = Modifier.height(Spacing.xs))
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Espresso,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

@Composable
private fun adminSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Pine,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Mist,
    uncheckedTrackColor = Oat,
    uncheckedBorderColor = CabinLineStrong
)

@Composable
private fun adminSliderColors() = SliderDefaults.colors(
    thumbColor = Honey,
    activeTrackColor = Pine,
    inactiveTrackColor = Oat
)

@Composable
private fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pine,
    unfocusedBorderColor = CabinLineStrong,
    focusedTextColor = Espresso,
    unfocusedTextColor = Espresso,
    focusedLabelColor = Pine,
    unfocusedLabelColor = Espresso.copy(alpha = 0.72f),
    cursorColor = Pine,
    focusedContainerColor = Cream,
    unfocusedContainerColor = Cream,
    errorBorderColor = Clay,
    errorLabelColor = Clay
)

/**
 * Copies a content URI to internal app storage so the path persists across sessions.
 */
private fun copyUriToInternal(
    context: android.content.Context,
    uri: Uri,
    filename: String
): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val brandingDir = java.io.File(context.filesDir, "branding")
        brandingDir.mkdirs()
        val destFile = java.io.File(brandingDir, filename)
        destFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        destFile
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun EventBlock(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    var showStartDialog by remember { mutableStateOf(false) }
    var newEventName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(Spacing.md)
    ) {
        if (settings.currentEventName.isNotBlank()) {
            Text(
                text = settings.currentEventName,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Slug: ${settings.currentEventSlug}",
                fontFamily = HankenGrotesk,
                fontSize = 13.sp,
                color = Espresso.copy(alpha = 0.72f)
            )
            if (settings.currentEventStartedAt > 0L) {
                val started = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
                    .format(Date(settings.currentEventStartedAt))
                Text(
                    text = "Started $started",
                    fontFamily = HankenGrotesk,
                    fontSize = 13.sp,
                    color = Espresso.copy(alpha = 0.72f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.s))
            Text(
                text = "Photos upload to Cloudinary folder events/${settings.currentEventSlug}/",
                fontFamily = HankenGrotesk,
                fontSize = 12.sp,
                color = Espresso.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "No active event",
                fontFamily = FrankRuhlLibre,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 22.sp,
                color = Espresso.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Photos will upload to events/unassigned/.",
                fontFamily = HankenGrotesk,
                fontSize = 12.sp,
                color = Espresso.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.s))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            BigButton(
                text = if (settings.currentEventName.isBlank()) "START EVENT" else "START NEW EVENT",
                onClick = {
                    newEventName = ""
                    showStartDialog = true
                },
                variant = BigButtonVariant.Primary
            )
            if (settings.currentEventName.isNotBlank()) {
                BigButton(
                    text = "END EVENT",
                    onClick = {
                        viewModel.updateSetting {
                            copy(
                                currentEventName = "",
                                currentEventSlug = "",
                                currentEventStartedAt = 0L
                            )
                        }
                    },
                    variant = BigButtonVariant.Surface
                )
            }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start new event") },
            text = {
                Column {
                    Text(
                        text = "Event name will scope the Cloudinary folder, audit log, and per-phone SMS limits. Used through the event.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Espresso.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(Spacing.s))
                    OutlinedTextField(
                        value = newEventName,
                        onValueChange = { newEventName = it.take(60) },
                        label = { Text("Event name (e.g. The Hewlett Wedding)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = adminTextFieldColors()
                    )
                    if (newEventName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Slug preview: ${EventSlug.from(newEventName)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val slug = EventSlug.from(newEventName, now)
                        viewModel.updateSetting {
                            copy(
                                currentEventName = newEventName.trim(),
                                currentEventSlug = slug,
                                currentEventStartedAt = now
                            )
                        }
                        showStartDialog = false
                    },
                    enabled = newEventName.isNotBlank()
                ) { Text("START") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun SendLogBlock(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    val entries = remember(settings.sendLogJson) { SendLog.parse(settings.sendLogJson) }
    val recent = entries.takeLast(50).reversed()
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${entries.size} entries total · last 50 shown",
                fontFamily = HankenGrotesk,
                fontSize = 13.sp,
                color = Espresso.copy(alpha = 0.72f)
            )
            BigButton(
                text = "CLEAR LOG",
                onClick = { viewModel.updateSetting { copy(sendLogJson = "[]") } },
                variant = BigButtonVariant.Surface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.s))

        if (recent.isEmpty()) {
            Text(
                text = "No sends recorded yet.",
                fontFamily = HankenGrotesk,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 13.sp,
                color = Espresso.copy(alpha = 0.6f)
            )
        } else {
            recent.forEach { e ->
                val tint = if (e.status == "ok") Pine else Clay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(tint)
                    )
                    Text(
                        text = fmt.format(Date(e.timestampMs)),
                        fontFamily = HankenGrotesk,
                        fontSize = 12.sp,
                        color = Espresso.copy(alpha = 0.72f),
                        modifier = Modifier.width(110.dp)
                    )
                    Text(
                        text = e.channel.uppercase(),
                        fontFamily = HankenGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Walnut,
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        text = e.recipientMasked,
                        fontFamily = HankenGrotesk,
                        fontSize = 12.sp,
                        color = Espresso,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (e.eventSlug.isNotBlank()) e.eventSlug else "—",
                        fontFamily = HankenGrotesk,
                        fontSize = 11.sp,
                        color = Espresso.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                if (e.status == "err" && e.note.isNotBlank()) {
                    Text(
                        text = e.note,
                        fontFamily = HankenGrotesk,
                        fontSize = 11.sp,
                        color = Clay,
                        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}
