package com.snapcabin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
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
 * Draws the configured corner logo on top of a live camera preview as an
 * alignment guide. Positioned inside a 4:3 letterboxed area — the camera's
 * native capture aspect — so the on-screen corner matches where the logo will
 * land in the final photo.
 *
 * The logo PNG is decoded once at its native resolution (downsampled only if
 * unreasonably large) and Compose handles positioning and scaling, so the
 * on-screen logo is as sharp as the source artwork — no intermediate bitmap.
 *
 * Intentional limitations:
 *  - Only the corner overlay is shown live. A full-frame stretched border or
 *    overlay would obscure the framing area and make composition harder, so
 *    those are previewed only in the BRANDING settings card.
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

    val logo = remember(settings.customOverlayPath) {
        CustomBrandingRenderer.loadOverlayForPreview(settings.customOverlayPath)
    } ?: return

    val sizePct = settings.overlaySizePct.coerceIn(5, 60)
    val cornerAlignment = when (settings.overlayCorner.lowercase()) {
        "tl" -> Alignment.TopStart
        "tr" -> Alignment.TopEnd
        "bl" -> Alignment.BottomStart
        else -> Alignment.BottomEnd
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Inner 4:3 letterboxed area = the camera's actual capture frame.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(4f / 3f)
        ) {
            // Mirror the renderer's 4% inset so the on-screen position
            // matches the final composite within a couple of pixels.
            val marginDp = maxWidth * CornerMarginFraction
            Box(modifier = Modifier.fillMaxSize().padding(marginDp)) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(sizePct / 100f)
                        .align(cornerAlignment)
                )
            }
        }
    }
}

private const val CornerMarginFraction = 0.04f
