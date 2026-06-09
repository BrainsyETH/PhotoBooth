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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.TestEmailStatus
import com.snapcabin.ui.screens.admin.adminSliderColors
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
internal fun ResendSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SetupHint(
            title = "What you'll need",
            url = "https://snapcabin.app/setup/resend",
            bullets = listOf(
                "A free Resend account (resend.com)",
                "An API key from resend.com/api-keys",
                "A verified sending domain (or use onboarding@resend.dev for testing)"
            )
        )

        SettingRow("Enable email delivery via Resend") {
            Switch(
                checked = settings.resendEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(resendEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.resendEnabled) {
            var key by remember { mutableStateOf(settings.resendApiKey) }
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it.trim()
                    viewModel.updateSetting { copy(resendApiKey = key) }
                },
                label = { Text("API key (starts with re_)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            var from by remember { mutableStateOf(settings.resendFromAddress) }
            OutlinedTextField(
                value = from,
                onValueChange = {
                    from = it.trim()
                    viewModel.updateSetting { copy(resendFromAddress = from) }
                },
                label = { Text("From address") },
                placeholder = { Text("SnapCabin <booth@yourdomain.com>") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            if (from.contains("@resend.dev", ignoreCase = true)) {
                TestingModeBanner()
            }

            var replyTo by remember { mutableStateOf(settings.resendReplyToAddress) }
            OutlinedTextField(
                value = replyTo,
                onValueChange = {
                    replyTo = it.trim()
                    viewModel.updateSetting { copy(resendReplyToAddress = replyTo) }
                },
                label = { Text("Reply-to (optional)") },
                placeholder = { Text("you@yourdomain.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            var subject by remember { mutableStateOf(settings.resendSubject) }
            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                    viewModel.updateSetting { copy(resendSubject = subject) }
                },
                label = { Text("Subject line ({event} expands)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            SettingRow("Max emails per session: ${settings.resendMaxPerSession}") {
                Slider(
                    value = settings.resendMaxPerSession.toFloat(),
                    onValueChange = { v ->
                        viewModel.updateSetting { copy(resendMaxPerSession = v.toInt()) }
                    },
                    valueRange = 1f..50f,
                    steps = 48,
                    modifier = Modifier.width(200.dp),
                    colors = adminSliderColors()
                )
            }

            SettingRow("Max per address (per event): ${settings.resendMaxPerAddress}") {
                Slider(
                    value = settings.resendMaxPerAddress.toFloat(),
                    onValueChange = { v ->
                        viewModel.updateSetting { copy(resendMaxPerAddress = v.toInt()) }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.width(200.dp),
                    colors = adminSliderColors()
                )
            }

            Text(
                text = "Resend's free tier caps at 100 emails per day, so we default the per-session cap to 3. The photo rides as a JPEG attachment, so guests get it even without Cloudinary.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.s))
            TestSendBlock(viewModel = viewModel)
        }
    }
}

@Composable
private fun TestingModeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Color(0xFFFFF4D6))
            .border(1.dp, Color(0xFFB8862E), RoundedCornerShape(Radii.s))
            .padding(Spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Testing mode — set up a verified domain before the event so emails don't arrive from a generic Resend address.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B4F0E)
        )
    }
}

@Composable
private fun TestSendBlock(viewModel: AdminViewModel) {
    val status by viewModel.testEmailStatus.collectAsState()
    val statusMessage by viewModel.testEmailMessage.collectAsState()
    var testAddress by remember { mutableStateOf("") }

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
            text = "Send a test email",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Pine
        )
        Text(
            text = "Sends a placeholder JPEG so you can confirm your API key and From address work before the event.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.72f)
        )
        OutlinedTextField(
            value = testAddress,
            onValueChange = {
                testAddress = it.trim().take(120)
                if (status != TestEmailStatus.Idle) viewModel.resetTestEmailState()
            },
            label = { Text("Test recipient") },
            placeholder = { Text("you@yourdomain.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            BigButton(
                text = if (status == TestEmailStatus.Sending) "SENDING…" else "SEND TEST",
                onClick = { viewModel.sendResendTestEmail(testAddress) },
                variant = BigButtonVariant.Primary,
                enabled = status != TestEmailStatus.Sending && testAddress.isNotBlank()
            )
            if (status == TestEmailStatus.Sending) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(20.dp))
            }
        }
        if (statusMessage.isNotEmpty()) {
            val tint = when (status) {
                TestEmailStatus.Sent -> Pine
                TestEmailStatus.Failed -> Clay
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

@Composable
internal fun SetupHint(
    title: String,
    url: String,
    bullets: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, Pine.copy(alpha = 0.4f), RoundedCornerShape(Radii.s))
            .padding(Spacing.md)
    ) {
        Text(
            text = title,
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Pine
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        bullets.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Step-by-step: $url",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.6f)
        )
    }
}
