package com.snapcabin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.settings.BoothSettings

/**
 * Renders the *full* configured branding — border + overlay, both placement
 * modes — over a 4:3 photo preview as a transparent overlay. Used on the
 * Review screen so guests see how their photo will be branded before
 * accepting, without waiting for the Share screen to bake it in.
 *
 * Composes the layers directly with Compose Images instead of building an
 * intermediate bitmap, so each layer renders at its native source resolution
 * and stays sharp at any screen size.
 *
 * Differs from [BrandingLiveOverlay]:
 *  - Includes the border (Review previews show the final composition; a frame
 *    is welcome there, while during framing it would obscure the live view).
 *  - Supports the stretched overlay placement in addition to corner.
 */
@Composable
fun BrandingPreviewOverlay(
    settings: BoothSettings,
    modifier: Modifier = Modifier
) {
    val hasBorder = settings.customBorderPath.isNotBlank()
    val hasOverlay = settings.customOverlayPath.isNotBlank()
    if (!hasBorder && !hasOverlay) return

    val border = remember(settings.customBorderPath) {
        if (hasBorder) CustomBrandingRenderer.loadOverlayForPreview(settings.customBorderPath) else null
    }
    val overlay = remember(settings.customOverlayPath) {
        if (hasOverlay) CustomBrandingRenderer.loadOverlayForPreview(settings.customOverlayPath) else null
    }
    if (border == null && overlay == null) return

    Box(modifier = modifier) {
        // Border stretches edge-to-edge over the 4:3 preview area.
        border?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        overlay?.let { ov ->
            if (settings.overlayPlacement.equals("corner", ignoreCase = true)) {
                val sizePct = settings.overlaySizePct.coerceIn(5, 60)
                val cornerAlignment = when (settings.overlayCorner.lowercase()) {
                    "tl" -> Alignment.TopStart
                    "tr" -> Alignment.TopEnd
                    "bl" -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val marginDp = maxWidth * CornerMarginFraction
                    Box(modifier = Modifier.fillMaxSize().padding(marginDp)) {
                        Image(
                            bitmap = ov.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(sizePct / 100f)
                                .align(cornerAlignment)
                        )
                    }
                }
            } else {
                Image(
                    bitmap = ov.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private const val CornerMarginFraction = 0.04f
