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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.snapcabin.ui.screens.admin.StatusPill
import com.snapcabin.ui.screens.admin.StatusTone
import com.snapcabin.ui.screens.admin.TestStatus
import com.snapcabin.ui.screens.admin.ValidationHint
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

private val EMAIL_RE = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun fromLooksValid(from: String): Boolean {
    val trimmed = from.trim()
    // Accept "email@domain.tld" or "Display Name <email@domain.tld>".
    val angle = Regex("<([^>]+)>").find(trimmed)?.groupValues?.get(1)?.trim()
    val candidate = angle ?: trimmed
    return EMAIL_RE.matches(candidate)
}

@Composable
internal fun ResendSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        IntegrationStatusHeader(
            blurb = "Emails each guest their photo as a JPEG attachment. Works over WiFi — no carrier, no phone numbers.",
            tone = resendTone(settings),
            statusLabel = resendStatusLabel(settings)
        )

        SettingRow("Enable email delivery") {
            Switch(
                checked = settings.resendEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(resendEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.resendEnabled) {
            InstructionsCard(
                title = "Set up Resend (about 10 minutes)",
                steps = {
                    NumberedStep(1, "Create a free Resend account.") {
                        Text(
                            "The free tier covers 3,000 emails a month, 100 a day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("OPEN RESEND", "https://resend.com/signup")
                    }
                    NumberedStep(2, "Verify a sending domain.") {
                        Text(
                            "Add the DNS records Resend shows you. To just test first, skip this and use onboarding@resend.dev as the From address.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("ADD A DOMAIN", "https://resend.com/domains")
                    }
                    NumberedStep(3, "Create an API key with Sending access.") {
                        Text(
                            "Copy it — Resend shows the key only once. It starts with re_.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Espresso.copy(alpha = 0.7f)
                        )
                        KioskSafeLink("API KEYS", "https://resend.com/api-keys")
                    }
                    NumberedStep(4, "Paste the key and From address below, then send a test.")
                }
            )

            var key by remember { mutableStateOf(settings.resendApiKey) }
            var keyVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it.trim()
                    // A credential change invalidates any prior successful test.
                    viewModel.updateSetting { copy(resendApiKey = key, resendVerifiedAt = 0L) }
                },
                label = { Text("API key") },
                placeholder = { Text("re_…") },
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { RevealToggle(visible = keyVisible, onToggle = { keyVisible = !keyVisible }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
            ValidationHint(
                ok = key.startsWith("re_") && key.length > 8,
                okText = "Looks like a Resend key.",
                hintText = "Resend API keys start with re_."
            )

            var from by remember { mutableStateOf(settings.resendFromAddress) }
            OutlinedTextField(
                value = from,
                onValueChange = {
                    from = it.trim()
                    viewModel.updateSetting { copy(resendFromAddress = from, resendVerifiedAt = 0L) }
                },
                label = { Text("From address") },
                placeholder = { Text("SnapCabin <booth@yourdomain.com>") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
            ValidationHint(
                ok = fromLooksValid(from),
                okText = "Sender looks good.",
                hintText = "Use an address on a domain you verified, e.g. SnapCabin <booth@yourdomain.com>."
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
            Text(
                text = "Where guest replies go. Leave blank to use the From address.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
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
            Text(
                text = "Type {event} anywhere and it becomes your event name.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )

            var body by remember { mutableStateOf(settings.resendBodyText) }
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    viewModel.updateSetting { copy(resendBodyText = body) }
                },
                label = { Text("Email message") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
            Text(
                text = "Shown above the attached photo. {event} expands to the event name; leave a blank line between paragraphs.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
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
                text = "Resend's free tier caps at 100 emails a day, so the per-session default is 3.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            TestSendBlock(viewModel = viewModel)
        }
    }
}

private fun resendConfigured(s: BoothSettings) =
    s.resendApiKey.isNotBlank() && s.resendFromAddress.isNotBlank()

private fun resendTone(s: BoothSettings): StatusTone = when {
    !s.resendEnabled -> StatusTone.Neutral
    !resendConfigured(s) -> StatusTone.Warn
    s.resendVerifiedAt > 0L -> StatusTone.Good
    else -> StatusTone.Warn
}

private fun resendStatusLabel(s: BoothSettings): String = when {
    !s.resendEnabled -> "Off"
    !resendConfigured(s) -> "Needs details"
    s.resendVerifiedAt > 0L -> "Ready · tested"
    else -> "Send a test"
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
            text = "Testing mode — emails arrive from a generic Resend address. Verify your own domain before the event.",
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
            text = "Sends a placeholder image so you can confirm your key and From address work before the event.",
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.72f)
        )
        OutlinedTextField(
            value = testAddress,
            onValueChange = {
                testAddress = it.trim().take(120)
                if (status != TestStatus.Idle) viewModel.resetTestEmailState()
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
                text = if (status == TestStatus.Sending) "SENDING…" else "SEND TEST",
                onClick = { viewModel.sendResendTestEmail(testAddress) },
                variant = BigButtonVariant.Primary,
                enabled = status != TestStatus.Sending && testAddress.isNotBlank()
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

/** Shared header used by the Resend and Cloudinary sections: a one-line "what
 *  this does" plus a status pill. */
@Composable
internal fun IntegrationStatusHeader(
    blurb: String,
    tone: StatusTone,
    statusLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            Text(
                text = "Status",
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            StatusPill(text = statusLabel, tone = tone)
        }
        Text(
            text = blurb,
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.75f)
        )
    }
}

/** Shared card that wraps a list of NumberedSteps. */
@Composable
internal fun InstructionsCard(
    title: String,
    steps: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, Pine.copy(alpha = 0.35f), RoundedCornerShape(Radii.s))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = title,
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Pine
        )
        steps()
    }
}
