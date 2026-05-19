package com.snapcabin.ui.screens.admin.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.screens.admin.copyUriToInternal
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun BrandingSection(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        var name by remember { mutableStateOf(settings.eventName) }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                viewModel.updateSetting { copy(eventName = it) }
            },
            label = { Text("Event Name (Attract headline)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )

        var sub by remember { mutableStateOf(settings.attractSubtext) }
        OutlinedTextField(
            value = sub,
            onValueChange = {
                sub = it
                viewModel.updateSetting { copy(attractSubtext = it) }
            },
            label = { Text("Attract Subtext (tagline)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radii.s),
            colors = adminTextFieldColors()
        )

        SettingRow("Watermark") {
            Switch(
                checked = settings.watermarkEnabled,
                onCheckedChange = { v -> viewModel.updateSetting { copy(watermarkEnabled = v) } },
                colors = adminSwitchColors()
            )
        }

        if (settings.watermarkEnabled) {
            var text by remember { mutableStateOf(settings.watermarkText) }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    viewModel.updateSetting { copy(watermarkText = it) }
                },
                label = { Text("Watermark Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )
        }

        ImagePickerBlock(
            title = "Custom Border / Frame",
            currentPath = settings.customBorderPath,
            filename = "custom_border.png",
            onPicked = { path -> viewModel.updateSetting { copy(customBorderPath = path) } },
            onRemove = { viewModel.updateSetting { copy(customBorderPath = "") } }
        )

        ImagePickerBlock(
            title = "Custom Overlay / Logo",
            currentPath = settings.customOverlayPath,
            filename = "custom_overlay.png",
            onPicked = { path -> viewModel.updateSetting { copy(customOverlayPath = path) } },
            onRemove = { viewModel.updateSetting { copy(customOverlayPath = "") } }
        )
    }
}

@Composable
private fun ImagePickerBlock(
    title: String,
    currentPath: String,
    filename: String,
    onPicked: (String) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = copyUriToInternal(context, it, filename)
            file?.let { f -> onPicked(f.absolutePath) }
        }
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
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = Espresso,
                modifier = Modifier.weight(1f)
            )
            BigButton(
                text = if (currentPath.isNotEmpty()) "CHANGE" else "UPLOAD",
                onClick = { launcher.launch("image/*") },
                variant = BigButtonVariant.Primary
            )
        }
        if (currentPath.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.s))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = java.io.File(currentPath)),
                    contentDescription = "$title preview",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(Radii.xs))
                )
                BigButton(
                    text = "REMOVE",
                    onClick = onRemove,
                    variant = BigButtonVariant.Surface
                )
            }
        }
    }
}
