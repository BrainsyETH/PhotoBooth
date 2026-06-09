package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.CabinLine
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

            var subject by remember { mutableStateOf(settings.resendSubject) }
            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                    viewModel.updateSetting { copy(resendSubject = subject) }
                },
                label = { Text("Subject line") },
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
                text = "The photo rides as a JPEG attachment, so guests get it even without Cloudinary. The From address must use a domain you've verified in Resend (or onboarding@resend.dev for testing).",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
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
