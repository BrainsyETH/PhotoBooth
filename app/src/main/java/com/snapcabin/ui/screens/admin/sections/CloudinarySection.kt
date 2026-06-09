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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.KioskSafeLink
import com.snapcabin.ui.screens.admin.NumberedStep
import com.snapcabin.ui.screens.admin.RevealToggle
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.StatusTone
import com.snapcabin.ui.screens.admin.TestStatus
import com.snapcabin.ui.screens.admin.ValidationHint
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun CloudinarySection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        IntegrationStatusHeader(
            blurb = "Hosts each photo so the share screen can show a QR code guests scan to download. Optional — email works without it.",
            tone = cloudinaryTone(settings),
            statusLabel = cloudinaryStatusLabel(settings)
        )

        SettingRow("Enable QR photo hosting") {
            Switch(
                checked = settings.cloudinaryEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(cloudinaryEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.cloudinaryEnabled) {
            InstructionsCard(
                title = "Set up Cloudinary (about 5 minutes)",
                steps = {
                    NumberedStep(1, "Create a free Cloudinary account.") {
                        Text(
                            "The free tier — about 25 GB storage and bandwidth — covers a typical event.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("OPEN CLOUDINARY", "https://cloudinary.com/users/register_free")
                    }
                    NumberedStep(2, "Copy your cloud name.") {
                        Text(
                            "It's on the dashboard under Account Details — usually your account handle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("OPEN DASHBOARD", "https://console.cloudinary.com")
                    }
                    NumberedStep(3, "Create an Unsigned upload preset.") {
                        Text(
                            "Settings → Upload → Add upload preset. Set Signing Mode to Unsigned, allow jpg/png, and cap file size around 10 MB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        Text(
                            "\"Unsigned\" just lets the booth upload photos without storing a secret password on the tablet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("UPLOAD SETTINGS", "https://console.cloudinary.com/settings/upload")
                    }
                    NumberedStep(4, "Paste the cloud name and preset below, then run a test upload.")
                }
            )

            var name by remember { mutableStateOf(settings.cloudinaryCloudName) }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = sanitizeCloudName(it)
                    viewModel.updateSetting { copy(cloudinaryCloudName = name, cloudinaryVerifiedAt = 0L) }
                },
                label = { Text("Cloud name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
            ValidationHint(
                ok = name.isNotBlank(),
                okText = "Cloud name set.",
                hintText = "Copy it exactly from the Cloudinary dashboard — it's case-sensitive."
            )

            var preset by remember { mutableStateOf(settings.cloudinaryUploadPreset) }
            var presetVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = preset,
                onValueChange = {
                    preset = it.trim()
                    viewModel.updateSetting { copy(cloudinaryUploadPreset = preset, cloudinaryVerifiedAt = 0L) }
                },
                label = { Text("Upload preset name") },
                visualTransformation = if (presetVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { RevealToggle(visible = presetVisible, onToggle = { presetVisible = !presetVisible }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
            ValidationHint(
                ok = preset.isNotBlank(),
                okText = "Preset set.",
                hintText = "Use the \"Unsigned\" preset name from Cloudinary, exactly as shown."
            )

            Text(
                text = "Keep the preset Unsigned and restricted to images under ~10 MB. If the name ever leaks, delete it in Cloudinary and make a new one.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            TestUploadBlock(viewModel = viewModel)
        }
    }
}

/**
 * Cloud names are short, lowercase handles, but operators often paste a full
 * dashboard URL like "https://console.cloudinary.com/app/c-abc123/...". Strip an
 * obvious URL down to a plausible cloud name and lowercase it. Conservative: a
 * plain name with no "/" or "http" is only trimmed + lowercased.
 */
internal fun sanitizeCloudName(raw: String): String {
    var s = raw.trim()
    if (s.contains("/") || s.contains("http", ignoreCase = true)) {
        // Drop scheme, then take the last non-empty path-ish segment.
        s = s.substringAfter("://", s)
        val segment = s.split('/', '?', '#')
            .map { it.trim() }
            .lastOrNull { it.isNotEmpty() }
        s = segment ?: s
    }
    return s.lowercase()
}

private fun cloudinaryConfigured(s: BoothSettings) =
    s.cloudinaryCloudName.isNotBlank() && s.cloudinaryUploadPreset.isNotBlank()

private fun cloudinaryTone(s: BoothSettings): StatusTone = when {
    !s.cloudinaryEnabled -> StatusTone.Neutral
    !cloudinaryConfigured(s) -> StatusTone.Warn
    s.cloudinaryVerifiedAt > 0L -> StatusTone.Good
    else -> StatusTone.Warn
}

private fun cloudinaryStatusLabel(s: BoothSettings): String = when {
    !s.cloudinaryEnabled -> "Off"
    !cloudinaryConfigured(s) -> "Needs details"
    s.cloudinaryVerifiedAt > 0L -> "Ready · tested"
    else -> "Run a test"
}

@Composable
private fun TestUploadBlock(viewModel: AdminViewModel) {
    val status by viewModel.testUploadStatus.collectAsState()
    val statusMessage by viewModel.testUploadMessage.collectAsState()

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
            text = "Test the upload",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Pine
        )
        Text(
            text = "Uploads a placeholder image so you can confirm the cloud name and preset work before the event.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.72f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            BigButton(
                text = if (status == TestStatus.Sending) "UPLOADING…" else "TEST UPLOAD",
                onClick = { viewModel.sendCloudinaryTest() },
                variant = BigButtonVariant.Primary,
                enabled = status != TestStatus.Sending
            )
            if (status == TestStatus.Sending) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(20.dp))
            }
        }
        if (statusMessage.isNotEmpty()) {
            val tint = when (status) {
                TestStatus.Sent -> Pine
                TestStatus.Failed -> Clay
                else -> Espresso
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
