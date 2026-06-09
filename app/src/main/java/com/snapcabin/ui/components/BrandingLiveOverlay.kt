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
 * Draws the configured corner logo on top of a live camera preview as an
 * alignment guide. Sized at 4:3 — the camera's native capture aspect — and
 * fit (letterboxed) inside the screen so the on-screen corner of the logo
 * matches where it will land in the final photo.
 *
 * Intentional limitations:
 *  - Only the corner overlay is shown live. A full-frame stretched border
 *    or overlay would obscure the framing area and make composition harder,
 *    so those are previewed only in the BRANDING settings card.
 *  - The preview camera scaleType is FILL_CENTER, which crops a bit on the
 *    sides to fill the screen; the letterboxed 4:3 box mirrors what will
 *    actually be captured.
 */
@Composable
fun BrandingLiveOverlay(
    settings: BoothSettings,
    modifier: Modifier = Modifier
) {
    if (settings.customOverlayPath.isBlank()) return
    if (!settings.overlayPlacement.equals("corner", ignoreCase = true)) return

    val bitmap = remember(
        settings.customOverlayPath,
        settings.overlayCorner,
        settings.overlaySizePct
    ) {
        // A transparent 4:3 canvas to composite the logo onto. After
        // CustomBrandingRenderer.apply(), only the logo region is opaque, so
        // the rest of the camera preview shows through. We skip the border
        // (passed as empty path) because a full-frame border in the live
        // preview would obscure the framing area.
        val src = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        CustomBrandingRenderer.apply(
            source = src,
            borderPath = "",
            overlayPath = settings.customOverlayPath,
            overlayPlacement = "corner",
            overlayCorner = settings.overlayCorner,
            overlaySizePct = settings.overlaySizePct
        )
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
