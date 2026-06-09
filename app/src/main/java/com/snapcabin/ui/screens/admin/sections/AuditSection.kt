package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.event.SendLog
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AuditSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    val entries = remember(settings.sendLogJson) { SendLog.parse(settings.sendLogJson) }
    val recent = entries.takeLast(50).reversed()
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear the send log?") },
            text = {
                Text(
                    text = "Deletes all ${entries.size} entries — your only record of which guests were sent photos. This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Espresso.copy(alpha = 0.72f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSetting { copy(sendLogJson = "[]") }
                        showClearDialog = false
                    }
                ) { Text("CLEAR LOG") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("CANCEL") }
            }
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
                onClick = { showClearDialog = true },
                variant = BigButtonVariant.Surface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.s))

        if (recent.isEmpty()) {
            Text(
                text = "No sends recorded yet.",
                fontFamily = HankenGrotesk,
                fontStyle = FontStyle.Italic,
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
                            .clip(CircleShape)
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
