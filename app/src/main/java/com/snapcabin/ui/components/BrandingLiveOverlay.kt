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
 * Draws the configured corner logo on top of a live camera preview as an
 * alignment guide.
 *
 * Geometry: the preview uses PreviewView.ScaleType.FILL_CENTER, which scales
 * the 4:3 capture frame until it covers the screen. On a wider-than-4:3
 * screen (every 16:10 tablet) the frame matches the screen width and
 * OVERFLOWS vertically — the top and bottom of the eventual photo are not
 * visible during framing. We compute that on-screen frame rect, position the
 * logo against it with the same 4% margin the final composite uses, then
 * clamp it into the visible screen so the guide is never cut off by the
 * overflow. Exact pixel parity with the photo is impossible on a non-4:3
 * screen; a visible, slightly-inset guide beats a cropped one.
 *
 * The logo PNG is decoded once at native resolution (capped at 2048px) and
 * scaled exactly once by Compose at the target size, so it stays sharp.
 *
 * Only the corner overlay is shown live — a full-frame border or stretched
 * overlay would obscure the framing area. Those preview in BRANDING instead.
 */
@Composable
fun BrandingLiveOverlay(
    settings: BoothSettings,
    modifier: Modifier = Modifier
) {
    if (settings.customOverlayPath.isBlank()) return
    if (!settings.overlayPlacement.equals("corner", ignoreCase = true)) return

    val logo = remember(settings.customOverlayPath) {
        CustomBrandingRenderer.loadOverlayForPreview(settings.customOverlayPath)
    } ?: return
    if (logo.width <= 0 || logo.height <= 0) return

    val sizePct = settings.overlaySizePct.coerceIn(5, 60)
    val corner = settings.overlayCorner.lowercase()

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

        // Same proportions as CustomBrandingRenderer.drawCornerOverlay.
        val logoW = frameW * sizePct / 100f
        val logoH = logoW * logo.height / logo.width
        val margin = frameW * 0.04f

        val rawX = when (corner) {
            "tl", "bl" -> frameLeft + margin
            else -> frameLeft + frameW - margin - logoW
        }
        val rawY = when (corner) {
            "tl", "tr" -> frameTop + margin
            else -> frameTop + frameH - margin - logoH
        }
        // Clamp into the visible screen — the frame overflow would otherwise
        // push bottom/top corner logos partially off-screen.
        val x = rawX.coerceIn(0f, (screenW - logoW).coerceAtLeast(0f))
        val y = rawY.coerceIn(0f, (screenH - logoH).coerceAtLeast(0f))

        val density = LocalDensity.current
        Image(
            bitmap = logo.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(with(density) { logoW.toDp() })
                .height(with(density) { logoH.toDp() })
        )
    }
}
