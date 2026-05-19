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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.event.EventSlug
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun EventSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        EventBlock(settings = settings, viewModel = viewModel)

        if (settings.adminPin == "1234") {
            DefaultPinWarning()
        }
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
                fontStyle = FontStyle.Italic,
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
private fun DefaultPinWarning() {
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
