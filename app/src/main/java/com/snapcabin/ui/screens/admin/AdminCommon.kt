package com.snapcabin.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Mist
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

internal data class AdminSection(
    val key: String,
    val label: String,
    val helpUrl: String? = null,
    val helpLabel: String = "SETUP GUIDE",
    val content: @Composable () -> Unit
)

@Composable
internal fun SectionHeader(
    label: String,
    helpUrl: String? = null,
    helpLabel: String = "SETUP GUIDE"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Eyebrow(text = label, color = Honey)
        if (helpUrl != null) {
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radii.xs))
                    .background(Cream)
                    .border(1.dp, Pine.copy(alpha = 0.4f), RoundedCornerShape(Radii.xs))
                    .clickable { openExternalUrl(context, helpUrl) }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$helpLabel →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Pine
                )
            }
        }
    }
}

@Composable
internal fun SettingRow(
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
internal fun adminSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Pine,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Mist,
    uncheckedTrackColor = Oat,
    uncheckedBorderColor = CabinLineStrong
)

@Composable
internal fun adminSliderColors() = SliderDefaults.colors(
    thumbColor = Honey,
    activeTrackColor = Pine,
    inactiveTrackColor = Oat
)

@Composable
internal fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
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

internal fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Kiosk mode (or no browser installed) may block this — operator should
        // visit the URL on a phone instead. The URL is also shown as text in
        // the section help body so it's recoverable.
    }
}

internal fun copyUriToInternal(
    context: Context,
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
