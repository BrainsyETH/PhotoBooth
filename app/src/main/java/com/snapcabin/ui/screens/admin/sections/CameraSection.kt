package com.snapcabin.ui.screens.admin.sections

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                colors = adminSwitchColors(),
                enabled = settings.cameraId.isEmpty()
            )
        }
        if (settings.cameraId.isNotEmpty()) {
            Text(
                text = "A specific camera is selected below, so this toggle is ignored. Choose \"Auto\" to use it again.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
        }

        SettingRow("Mirror Image") {
            Switch(
                checked = settings.mirrorImage,
                onCheckedChange = { v -> viewModel.updateSetting { copy(mirrorImage = v) } },
                colors = adminSwitchColors()
            )
        }
        Text(
            text = "Mirrored = photos match what guests see on screen, like a mirror. Most booths leave this on.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )

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
                    "Camera",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Espresso.copy(alpha = 0.72f),
                    modifier = Modifier.padding(bottom = Spacing.s)
                )
                val autoSelected = settings.cameraId.isEmpty()
                BigButton(
                    text = "Auto (follows the Front Camera toggle)",
                    onClick = { viewModel.updateSetting { copy(cameraId = "") } },
                    variant = if (autoSelected) BigButtonVariant.Accent else BigButtonVariant.Surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs)
                )
                cameras.forEach { cam ->
                    val isSelected = settings.cameraId == cam.id
                    val label = "${cam.facing} (ID: ${cam.id})${if (cam.isExternal) " — External" else ""}"

                    BigButton(
                        text = label,
                        onClick = { viewModel.updateSetting { copy(cameraId = cam.id) } },
                        variant = if (isSelected) BigButtonVariant.Accent else BigButtonVariant.Surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs)
                    )
                }
            }
        }

        CameraPreviewBlock(settings = settings, viewModel = viewModel)

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
            Spacer(modifier = Modifier.height(Spacing.s))
            Text(
                text = "Full-resolution photos make 3–8 MB email attachments. Medium keeps emails snappy and still looks great on phones.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * On-demand live preview so the operator can verify the selected camera and
 * mirror setting without leaving admin. Deliberately opt-in (a button) rather
 * than always-on: LazyColumn disposes off-screen items, and an always-bound
 * preview would churn camera rebinds while scrolling the settings list.
 */
@Composable
private fun CameraPreviewBlock(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    var showPreview by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        BigButton(
            text = if (showPreview) "HIDE PREVIEW" else "TEST CAMERA",
            onClick = { showPreview = !showPreview },
            variant = if (showPreview) BigButtonVariant.Surface else BigButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )
        if (showPreview) {
            // key() forces a fresh PreviewView + rebind whenever the operator
            // changes camera selection or mirror while the preview is open.
            key(settings.cameraId, settings.useFrontCamera, settings.mirrorImage) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                            viewModel.cameraManager.bindCamera(
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                useFront = settings.useFrontCamera,
                                cameraId = settings.cameraId,
                                mirror = settings.mirrorImage,
                                maxResolution = settings.photoResolution.maxDimension
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(Radii.xs))
                )
            }
            DisposableEffect(Unit) {
                onDispose { viewModel.cameraManager.release() }
            }
            Text(
                text = "This is the camera guests will get. Capture screens rebind it automatically when you leave admin.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
            )
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
                    // The button IS the swatch — painted in the actual flash
                    // color so the operator isn't guessing what "Champagne" is.
                    val swatch = remember(hex) {
                        Color(android.graphics.Color.parseColor(hex))
                    }
                    BigButton(
                        text = if (isSel) "● $label" else label,
                        onClick = { viewModel.updateSetting { copy(flashColor = hex) } },
                        containerColor = swatch,
                        contentColor = Espresso
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radii.s))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                .padding(Spacing.md)
        ) {
            Text(
                "Review auto-accept",
                style = MaterialTheme.typography.bodyLarge,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(0 to "Off", 5 to "5s", 10 to "10s", 20 to "20s", 30 to "30s").forEach { (secs, label) ->
                    BigButton(
                        text = label,
                        onClick = { viewModel.updateSetting { copy(reviewAutoAcceptSeconds = secs) } },
                        variant = if (settings.reviewAutoAcceptSeconds == secs) BigButtonVariant.Accent else BigButtonVariant.Surface
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.s))
            Text(
                text = "What guests see: a countdown on the USE THIS ONE button. When it hits zero the photo is accepted automatically — tapping anywhere pauses it. Off means guests must tap to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
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
