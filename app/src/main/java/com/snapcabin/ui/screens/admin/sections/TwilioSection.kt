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
internal fun TwilioSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SetupHint(
            title = "What you'll need",
            url = "https://snapcabin.app/setup/twilio",
            bullets = listOf(
                "A Twilio account (free trial works for testing)",
                "Account SID + Auth Token from console.twilio.com",
                "A purchased Twilio phone number with SMS (and MMS, if you want to send photos)"
            )
        )

        SettingRow("Enable Twilio SMS sending") {
            Switch(
                checked = settings.twilioEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(twilioEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.twilioEnabled) {
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

            Text(
                text = "Without a public photo host, the SMS includes the kiosk's local IP URL — guests must be on the same WiFi to open it. Configure Cloudinary below (or set the host URL above) to deliver as MMS over cellular.",
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
