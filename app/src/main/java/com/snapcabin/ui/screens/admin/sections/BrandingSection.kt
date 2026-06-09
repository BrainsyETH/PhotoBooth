package com.snapcabin.ui.screens.admin.sections

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.screens.admin.adminSliderColors
import com.snapcabin.ui.screens.admin.adminSwitchColors
import com.snapcabin.ui.screens.admin.adminTextFieldColors
import com.snapcabin.ui.screens.admin.copyUriToInternal
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
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

        BrandingPreview(settings = settings)

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

        if (settings.customOverlayPath.isNotEmpty()) {
            OverlayPlacementControls(settings = settings, viewModel = viewModel)
        }
    }
}

@Composable
private fun BrandingPreview(settings: BoothSettings) {
    // A neutral 4:3 stand-in photo (gradient + a faux subject) so the operator
    // can see how the border/logo compose — including whether a corner logo
    // overlaps the subject — without taking a real photo.
    val source = remember { buildPreviewSource() }
    val composed = remember(
        settings.customBorderPath,
        settings.customOverlayPath,
        settings.overlayPlacement,
        settings.overlayCorner,
        settings.overlaySizePct
    ) {
        CustomBrandingRenderer.apply(
            source = source,
            borderPath = settings.customBorderPath,
            overlayPath = settings.customOverlayPath,
            overlayPlacement = settings.overlayPlacement,
            overlayCorner = settings.overlayCorner,
            overlaySizePct = settings.overlaySizePct
        )
    }
    val hasBranding = settings.customBorderPath.isNotEmpty() || settings.customOverlayPath.isNotEmpty()

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
            text = "PREVIEW",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Espresso.copy(alpha = 0.6f)
        )
        Image(
            bitmap = composed.asImageBitmap(),
            contentDescription = "Branding preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(Radii.xs))
        )
        Text(
            text = if (hasBranding) {
                "Roughly how a 4:3 photo will look. Real photos vary in aspect ratio."
            } else {
                "Upload a border or logo below to see it composed here."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Espresso.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun OverlayPlacementControls(
    settings: BoothSettings,
    viewModel: AdminViewModel
) {
    val isCorner = settings.overlayPlacement.equals("corner", ignoreCase = true)

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
            text = "LOGO PLACEMENT",
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Espresso.copy(alpha = 0.6f)
        )

        SettingRow("Place logo in a corner") {
            Switch(
                checked = isCorner,
                onCheckedChange = { v ->
                    viewModel.updateSetting {
                        copy(overlayPlacement = if (v) "corner" else "stretch")
                    }
                },
                colors = adminSwitchColors()
            )
        }

        if (isCorner) {
            Text(
                text = "Corner",
                style = MaterialTheme.typography.bodyMedium,
                color = Espresso.copy(alpha = 0.75f)
            )
            CornerPicker(
                selected = settings.overlayCorner,
                onSelect = { c -> viewModel.updateSetting { copy(overlayCorner = c) } }
            )
            SettingRow("Logo size: ${settings.overlaySizePct}% of width") {
                Slider(
                    value = settings.overlaySizePct.toFloat(),
                    onValueChange = { v ->
                        viewModel.updateSetting { copy(overlaySizePct = v.toInt()) }
                    },
                    valueRange = 8f..40f,
                    steps = 31,
                    modifier = Modifier.width(180.dp),
                    colors = adminSliderColors()
                )
            }
            Text(
                text = "Upload a tight crop of your logo (transparent PNG). It keeps its shape — no need to pre-pad it to the full frame.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "Stretched across the whole photo. Use a full-frame PNG that's transparent where the photo should show through.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CornerPicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            CornerCell("Top L", "tl", selected, onSelect, Modifier.weight(1f))
            CornerCell("Top R", "tr", selected, onSelect, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            CornerCell("Bottom L", "bl", selected, onSelect, Modifier.weight(1f))
            CornerCell("Bottom R", "br", selected, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CornerCell(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = selected.equals(value, ignoreCase = true)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radii.xs))
            .background(if (active) Pine else Cream)
            .border(1.dp, if (active) Pine else CabinLine, RoundedCornerShape(Radii.xs))
            .clickable { onSelect(value) }
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (active) Color.White else Espresso.copy(alpha = 0.8f)
        )
    }
}

/** A neutral placeholder "photo" for the branding preview: a soft sage→cream
 *  gradient with a faux subject (head + shoulders) so corner logos can be
 *  judged against where a person would stand. */
private fun buildPreviewSource(): android.graphics.Bitmap {
    val w = 600
    val h = 450
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bg = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            android.graphics.Color.rgb(0xC9, 0xD4, 0xBC),
            android.graphics.Color.rgb(0xF3, 0xEA, 0xD8),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

    val subject = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 0x3A, 0x2E, 0x20)
    }
    // Head
    canvas.drawCircle(w / 2f, h * 0.42f, h * 0.15f, subject)
    // Shoulders
    canvas.drawRoundRect(
        RectF(w / 2f - h * 0.27f, h * 0.62f, w / 2f + h * 0.27f, h.toFloat() + 60f),
        90f, 90f, subject
    )
    return bmp
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
