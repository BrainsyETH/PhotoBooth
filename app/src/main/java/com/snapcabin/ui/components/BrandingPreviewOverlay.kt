package com.snapcabin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.settings.BoothSettings
import kotlin.math.roundToInt

/**
 * Renders the *full* configured branding — border + overlay, both placement
 * modes — over a photo preview box as a transparent layer. Used on the Review
 * screen so guests see how their photo will be branded before accepting.
 *
 * Unlike the live-preview overlay, the parent box here IS the photo frame
 * (the Review previews are 4:3 boxes showing the photo edge-to-edge), so the
 * corner logo is positioned with the exact same proportional math the final
 * composite uses: width = sizePct% of frame width, inset = 4% of frame width.
 *
 * Differs from [BrandingLiveOverlay]:
 *  - Includes the border (Review shows the final composition; during live
 *    framing a full-frame border would obscure the view).
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
        // Border stretches edge-to-edge over the preview area.
        border?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        overlay?.let { ov ->
            if (settings.overlayPlacement.equals("corner", ignoreCase = true) &&
                ov.width > 0 && ov.height > 0
            ) {
                val sizePct = settings.overlaySizePct.coerceIn(5, 60)
                val corner = settings.overlayCorner.lowercase()
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = constraints.maxWidth.toFloat()
                    val h = constraints.maxHeight.toFloat()
                    if (w <= 0f || h <= 0f) return@BoxWithConstraints

                    // Mirrors CustomBrandingRenderer.drawCornerOverlay exactly.
                    val logoW = w * sizePct / 100f
                    val logoH = logoW * ov.height / ov.width
                    val margin = w * 0.04f
                    val x = when (corner) {
                        "tl", "bl" -> margin
                        else -> w - margin - logoW
                    }
                    val y = when (corner) {
                        "tl", "tr" -> margin
                        else -> h - margin - logoH
                    }

                    val density = LocalDensity.current
                    Image(
                        bitmap = ov.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .width(with(density) { logoW.toDp() })
                            .height(with(density) { logoH.toDp() })
                    )
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
