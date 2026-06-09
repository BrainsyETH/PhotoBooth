package com.snapcabin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
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
 * Draws the configured branding on top of a live camera preview as an alignment
 * guide: the full-frame border (and a stretched overlay), plus a corner logo.
 *
 * Geometry: the preview uses PreviewView.ScaleType.FILL_CENTER, which scales the
 * 4:3 capture frame until it covers the screen. On a wider-than-4:3 screen
 * (every 16:10 tablet) the frame matches the screen width and OVERFLOWS
 * vertically — the top and bottom of the eventual photo are not visible during
 * framing. We compute that on-screen frame rect and lay every branding layer
 * against it with the same proportional math the final composite uses, so what
 * the host sees framing is what lands on the photo. Full-frame layers extend
 * past the screen edges by design (that's the part of the photo not yet
 * visible); the corner logo is clamped into the visible screen so the guide is
 * never cut off.
 *
 * Each PNG is decoded once at native resolution (capped at 2048px) and scaled
 * once by Compose, so it stays sharp.
 */
@Composable
fun BrandingLiveOverlay(
    settings: BoothSettings,
    modifier: Modifier = Modifier
) {
    val hasBorder = settings.customBorderPath.isNotBlank()
    val hasOverlay = settings.customOverlayPath.isNotBlank()
    val isCorner = settings.overlayPlacement.equals("corner", ignoreCase = true)
    val hasCornerLogo = hasOverlay && isCorner
    val hasStretchOverlay = hasOverlay && !isCorner
    if (!hasBorder && !hasCornerLogo && !hasStretchOverlay) return

    val border = remember(settings.customBorderPath) {
        if (hasBorder) CustomBrandingRenderer.loadOverlayForPreview(settings.customBorderPath) else null
    }
    val overlay = remember(settings.customOverlayPath) {
        if (hasOverlay) CustomBrandingRenderer.loadOverlayForPreview(settings.customOverlayPath) else null
    }
    if (border == null && overlay == null) return

    BoxWithConstraints(modifier = modifier) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        if (screenW <= 0f || screenH <= 0f) return@BoxWithConstraints

        // On-screen rect of the 4:3 capture frame under FILL_CENTER.
        val frameRatio = 4f / 3f
        val (frameW, frameH) = if (screenW / screenH >= frameRatio) {
            screenW to screenW / frameRatio   // width-matched, overflows top/bottom
        } else {
            screenH * frameRatio to screenH   // height-matched, overflows left/right
        }
        val frameLeft = (screenW - frameW) / 2f
        val frameTop = (screenH - frameH) / 2f

        val density = LocalDensity.current

        // Full-frame layers sit on the capture frame rect (parts may fall off
        // the overflow edges — that's the photo area not visible while framing).
        border?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset { IntOffset(frameLeft.roundToInt(), frameTop.roundToInt()) }
                    .width(with(density) { frameW.toDp() })
                    .height(with(density) { frameH.toDp() })
            )
        }
        if (hasStretchOverlay) {
            overlay?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(frameLeft.roundToInt(), frameTop.roundToInt()) }
                        .width(with(density) { frameW.toDp() })
                        .height(with(density) { frameH.toDp() })
                )
            }
        }

        // Corner logo: width = sizePct% of the frame width, 4% inset, clamped
        // into the visible screen.
        if (hasCornerLogo && overlay != null && overlay.width > 0 && overlay.height > 0) {
            val sizePct = settings.overlaySizePct.coerceIn(5, 60)
            val corner = settings.overlayCorner.lowercase()
            val logoW = frameW * sizePct / 100f
            val logoH = logoW * overlay.height / overlay.width
            val margin = frameW * 0.04f

            val rawX = when (corner) {
                "tl", "bl" -> frameLeft + margin
                else -> frameLeft + frameW - margin - logoW
            }
            val rawY = when (corner) {
                "tl", "tr" -> frameTop + margin
                else -> frameTop + frameH - margin - logoH
            }
            val x = rawX.coerceIn(0f, (screenW - logoW).coerceAtLeast(0f))
            val y = rawY.coerceIn(0f, (screenH - logoH).coerceAtLeast(0f))

            Image(
                bitmap = overlay.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(with(density) { logoW.toDp() })
                    .height(with(density) { logoH.toDp() })
            )
        }
    }
}
