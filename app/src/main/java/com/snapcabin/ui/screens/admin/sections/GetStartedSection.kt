package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.screens.admin.ChecklistRow
import com.snapcabin.ui.screens.admin.StatusPill
import com.snapcabin.ui.screens.admin.StatusTone
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun GetStartedSection(
    settings: BoothSettings,
    onJumpTo: (String) -> Unit = {}
) {
    val pinChanged = settings.adminPin != "1234"
    val eventStarted = settings.currentEventName.isNotBlank()

    val emailConfigured = settings.resendEnabled &&
        settings.resendApiKey.isNotBlank() && settings.resendFromAddress.isNotBlank()
    val emailVerified = emailConfigured && settings.resendVerifiedAt > 0L

    val qrConfigured = settings.cloudinaryEnabled &&
        settings.cloudinaryCloudName.isNotBlank() && settings.cloudinaryUploadPreset.isNotBlank()
    val qrVerified = qrConfigured && settings.cloudinaryVerifiedAt > 0L

    val deliveryReady = emailConfigured || qrConfigured
    val allReady = pinChanged && eventStarted && deliveryReady

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = "A quick checklist before guests arrive. Tap any item to jump to the section that finishes it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Espresso.copy(alpha = 0.75f)
        )

        if (allReady) {
            Banner(
                tone = StatusTone.Good,
                title = "You're ready to go.",
                body = "PIN set, event started, and at least one way for guests to get their photos."
            )
        } else {
            Banner(
                tone = StatusTone.Warn,
                title = "A few things left.",
                body = "Work through the checklist below. Nothing here blocks the booth from running, but it's how you avoid surprises at the event."
            )
        }

        ChecklistRow(
            done = pinChanged,
            label = "Change the admin PIN",
            hint = if (pinChanged) "No longer the default." else "Still 1234 — tap to set a new one under KIOSK.",
            onClick = { onJumpTo("kiosk") }
        )
        ChecklistRow(
            done = eventStarted,
            label = "Start your event",
            hint = if (eventStarted) "Active: ${settings.currentEventName}." else "Tap to name it under EVENT — photos, logs, and limits get scoped to it.",
            onClick = { onJumpTo("event") }
        )
        ChecklistRow(
            done = deliveryReady,
            label = "Set up photo delivery",
            hint = if (deliveryReady) "Guests can get their photos." else "Tap to set up EMAIL DELIVERY (or QR DOWNLOADS for scan-to-save).",
            onClick = { onJumpTo("resend") }
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
        DeliverySummary(
            emailTone = emailTone(settings.resendEnabled, emailConfigured, emailVerified),
            emailLabel = emailLabel(settings.resendEnabled, emailConfigured, emailVerified),
            qrTone = qrTone(settings.cloudinaryEnabled, qrConfigured, qrVerified),
            qrLabel = qrLabel(settings.cloudinaryEnabled, qrConfigured, qrVerified),
            onJumpTo = onJumpTo
        )
    }
}

private fun emailTone(enabled: Boolean, configured: Boolean, verified: Boolean): StatusTone = when {
    !enabled -> StatusTone.Neutral
    !configured -> StatusTone.Warn
    verified -> StatusTone.Good
    else -> StatusTone.Warn
}

private fun emailLabel(enabled: Boolean, configured: Boolean, verified: Boolean): String = when {
    !enabled -> "Off"
    !configured -> "Needs details"
    verified -> "Ready · tested"
    else -> "Send a test"
}

private fun qrTone(enabled: Boolean, configured: Boolean, verified: Boolean): StatusTone = when {
    !enabled -> StatusTone.Neutral
    !configured -> StatusTone.Warn
    verified -> StatusTone.Good
    else -> StatusTone.Warn
}

private fun qrLabel(enabled: Boolean, configured: Boolean, verified: Boolean): String = when {
    !enabled -> "Off"
    !configured -> "Needs details"
    verified -> "Ready · tested"
    else -> "Run a test"
}

@Composable
private fun DeliverySummary(
    emailTone: StatusTone,
    emailLabel: String,
    qrTone: StatusTone,
    qrLabel: String,
    onJumpTo: (String) -> Unit
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
        Text(
            text = "DELIVERY METHODS · tap to configure",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Espresso.copy(alpha = 0.6f)
        )
        DeliveryRow("Email", "Photo as an attachment via Resend", emailLabel, emailTone) {
            onJumpTo("resend")
        }
        DeliveryRow("QR code", "Scan-to-download via Cloudinary", qrLabel, qrTone) {
            onJumpTo("cloudinary")
        }
    }
}

@Composable
private fun DeliveryRow(
    name: String,
    desc: String,
    statusLabel: String,
    statusTone: StatusTone,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.xs))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Espresso
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.65f)
            )
        }
        StatusPill(text = statusLabel, tone = statusTone)
    }
}

@Composable
private fun Banner(
    tone: StatusTone,
    title: String,
    body: String
) {
    val accent = when (tone) {
        StatusTone.Good -> Pine
        else -> com.snapcabin.ui.theme.HoneyDeep
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(Radii.s))
            .padding(Spacing.md)
    ) {
        Text(
            text = title,
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = accent
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.75f)
        )
    }
}
