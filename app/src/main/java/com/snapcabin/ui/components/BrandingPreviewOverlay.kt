package com.snapcabin.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.settings.BoothSettings

/**
 * Renders the *full* configured branding — border + overlay, both placement
 * modes — over a 4:3 photo preview as a transparent layer. Used on the Review
 * screen so guests see how their photo will be branded before accepting,
 * without waiting for the Share screen to bake it in.
 *
 * Differs from [BrandingLiveOverlay]:
 *  - Includes the border (Review previews show the final composition; a frame
 *    is welcome there, while during framing it would obscure the live view).
 *  - Includes the stretched overlay (not just corner placement).
 *
 * Renders to an 800x600 transparent canvas and lets Image FillBounds it over
 * a parent that's also 4:3, so border edges and corner logos align pixel-for-
 * pixel with the photo behind it.
 */
@Composable
fun BrandingPreviewOverlay(
    settings: BoothSettings,
    modifier: Modifier = Modifier
) {
    val hasBorder = settings.customBorderPath.isNotBlank()
    val hasOverlay = settings.customOverlayPath.isNotBlank()
    if (!hasBorder && !hasOverlay) return

    val bitmap = remember(
        settings.customBorderPath,
        settings.customOverlayPath,
        settings.overlayPlacement,
        settings.overlayCorner,
        settings.overlaySizePct
    ) {
        val src = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        CustomBrandingRenderer.apply(
            source = src,
            borderPath = settings.customBorderPath,
            overlayPath = settings.customOverlayPath,
            overlayPlacement = settings.overlayPlacement,
            overlayCorner = settings.overlayCorner,
            overlaySizePct = settings.overlaySizePct
        )
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}
