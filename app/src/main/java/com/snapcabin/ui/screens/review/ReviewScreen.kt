package com.snapcabin.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.collage.CollageLayout
import com.snapcabin.collage.CollageRenderer
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.filter.WatermarkRenderer
import com.snapcabin.settings.BoothSettings
import com.snapcabin.ui.components.BrandingPreviewOverlay
import com.snapcabin.ui.screens.capture.CaptureMode
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Pine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ReviewScreen(
    mode: CaptureMode,
    photos: List<Bitmap>,
    autoAcceptSeconds: Int = 10,
    onAccept: (pickedIndex: Int) -> Unit,
    onRetake: () -> Unit,
    settings: BoothSettings = BoothSettings()
) {
    val total = photos.size
    val isEmpty = photos.isEmpty()
    val initialPick = if (total > 0) total / 2 else 0
    var picked by remember { mutableIntStateOf(initialPick) }

    var remaining by remember { mutableStateOf(autoAcceptSeconds.toFloat()) }
    var paused by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // The auto-accept timer and the USE THIS ONE button can race — a guest
    // tapping as the countdown hits zero used to navigate to SHARE twice,
    // stacking two Share screens and double-stamping the watermark. Accept
    // exactly once.
    var accepted by remember { mutableStateOf(false) }
    val acceptOnce: (Int) -> Unit = { idx ->
        if (!accepted) {
            accepted = true
            onAccept(idx)
        }
    }

    LaunchedEffect(autoAcceptSeconds, paused, isEmpty) {
        // Never auto-accept when there's nothing to accept — that used to push
        // the guest to a Share screen with no photo and dead buttons.
        if (paused || autoAcceptSeconds <= 0 || isEmpty) return@LaunchedEffect
        startTime = System.currentTimeMillis()
        while (!paused && remaining > 0f) {
            delay(100)
            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
            remaining = (autoAcceptSeconds - elapsed).coerceAtLeast(0f)
        }
        if (!paused && remaining <= 0f) {
            acceptOnce(picked)
        }
    }

    val pauseTimer: () -> Unit = { paused = true }

    if (isEmpty) {
        EmptyCaptureRecovery(onRetake = onRetake)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pauseTimer()
                        tryAwaitRelease()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, start = 56.dp, end = 56.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (mode) {
                    CaptureMode.Single -> SinglePreview(photos = photos, picked = picked, settings = settings)
                    CaptureMode.Collage -> CollagePreview(photos = photos, settings = settings)
                    CaptureMode.Gif -> GifPreview(photos = photos, settings = settings)
                }
            }

            // Triptych row only for Single
            if (mode == CaptureMode.Single && photos.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    photos.forEachIndexed { i, bmp ->
                        ShotThumb(
                            bitmap = bmp,
                            index = i,
                            selected = i == picked,
                            onClick = {
                                picked = i
                                pauseTimer()
                            }
                        )
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Retake all
                Box(modifier = Modifier.weight(1f)) {
                    RetakeButton(onClick = onRetake)
                }

                // Center caption. The single-photo "tap a shot above" copy only
                // makes sense when there's actually a row of shots to tap.
                val caption = when (mode) {
                    CaptureMode.Single ->
                        if (photos.size > 1) stringResource(R.string.review_caption_single)
                        else stringResource(R.string.review_caption_single_one)
                    CaptureMode.Collage -> stringResource(R.string.review_caption_collage)
                    CaptureMode.Gif -> stringResource(R.string.review_caption_gif)
                }
                Text(
                    text = caption,
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    letterSpacing = 0.04f.em,
                    color = Espresso.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Right: Use this one
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    UseThisButton(
                        onClick = { acceptOnce(picked) },
                        progress = if (paused || autoAcceptSeconds <= 0) null
                            else ((autoAcceptSeconds - remaining) / autoAcceptSeconds).coerceIn(0f, 1f),
                        remainingSeconds = remaining
                    )
                }
            }
        }

    }
}

/**
 * Shown when capture produced nothing (every shot failed, or the guest skipped
 * before any frame). One warm, obvious path back to the camera — never a
 * dead-end Share screen.
 */
@Composable
private fun EmptyCaptureRecovery(onRetake: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "Hmm, that didn't take.",
                style = MaterialTheme.typography.headlineMedium,
                color = Espresso,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "No worries — let's give it another go.",
                fontFamily = HankenGrotesk,
                fontSize = 18.sp,
                color = Espresso.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .height(96.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(Pine)
                    .clickable(onClick = onRetake)
                    .padding(horizontal = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LET'S TRY AGAIN",
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 0.08f.em,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SinglePreview(photos: List<Bitmap>, picked: Int, settings: BoothSettings) {
    val photo = photos.getOrNull(picked)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(4f / 3f)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(3.dp, Pine, RoundedCornerShape(16.dp))
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        BrandingPreviewOverlay(
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Pine.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "SHOT ${picked + 1}",
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.22f.em,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CollagePreview(photos: List<Bitmap>, settings: BoothSettings) {
    var rendered by remember { mutableStateOf<Bitmap?>(null) }

    // The collage renders 16:9. Branding is baked into the assembled bitmap
    // with the exact renderer the Share pipeline uses, so the preview matches
    // the final image — no separately-positioned overlay floating in the
    // letterbox whitespace.
    LaunchedEffect(
        photos,
        settings.customBorderPath,
        settings.customOverlayPath,
        settings.overlayPlacement,
        settings.overlayCorner,
        settings.overlaySizePct,
        settings.watermarkEnabled,
        settings.watermarkText
    ) {
        rendered = null
        if (photos.size >= 2) {
            rendered = withContext(Dispatchers.Default) {
                val collage = CollageRenderer.render(photos, CollageLayout.GRID_2X2)
                val branded = CustomBrandingRenderer.apply(
                    source = collage,
                    borderPath = settings.customBorderPath,
                    overlayPath = settings.customOverlayPath,
                    overlayPlacement = settings.overlayPlacement,
                    overlayCorner = settings.overlayCorner,
                    overlaySizePct = settings.overlaySizePct
                )
                if (settings.watermarkEnabled && settings.watermarkText.isNotBlank()) {
                    WatermarkRenderer.apply(branded, settings.watermarkText)
                } else {
                    branded
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(16f / 9f)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        val bmp = rendered
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "Assembling…",
                fontFamily = HankenGrotesk,
                fontSize = 16.sp,
                color = Espresso.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // Branding is baked into `rendered` above — no separate overlay.
    }
}

@Composable
private fun GifPreview(photos: List<Bitmap>, settings: BoothSettings) {
    var frame by remember { mutableIntStateOf(0) }
    // Animate at the SAME cadence the delivered GIF will use, so the preview
    // doesn't promise a speed the final file won't keep.
    val frameDelayMs = settings.gifFrameDelayMs.coerceIn(60, 2000).toLong()
    LaunchedEffect(photos, frameDelayMs) {
        if (photos.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(frameDelayMs)
            frame = (frame + 1) % photos.size
        }
    }

    val infinite = rememberInfiniteTransition(label = "gif-rec")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gif-rec-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(4f / 3f)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(3.dp, Pine, RoundedCornerShape(16.dp))
    ) {
        photos.getOrNull(frame)?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        BrandingPreviewOverlay(
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Honey.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Clay.copy(alpha = pulseAlpha))
            )
            Text(
                text = "PLAYING · ${frame + 1} / ${photos.size}",
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.22f.em,
                color = Espresso
            )
        }
    }
}

@Composable
private fun ShotThumb(
    bitmap: Bitmap,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val offsetY = if (selected) (-4).dp else 0.dp
    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .size(width = 200.dp, height = 150.dp)
            .shadow(
                elevation = if (selected) 6.dp else 2.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Cream)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Pine else CabinLine,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) Pine else Espresso.copy(alpha = 0.78f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (selected) "PICK · SHOT ${index + 1}" else "SHOT ${index + 1}",
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.22f.em,
                color = Color.White
            )
        }
    }
}

@Composable
private fun RetakeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(84.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.review_retake_all).uppercase(),
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.08f.em,
            color = Espresso
        )
    }
}

@Composable
private fun UseThisButton(
    onClick: () -> Unit,
    progress: Float?,
    remainingSeconds: Float
) {
    Box(
        modifier = Modifier
            .height(84.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(Pine)
            .clickable(onClick = onClick)
            .drawBehind {
                if (progress != null) {
                    val sweepWidth = size.width * progress
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Honey.copy(alpha = 0f),
                                Honey.copy(alpha = 0.28f)
                            ),
                            startX = 0f,
                            endX = sweepWidth.coerceAtLeast(1f)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(sweepWidth, size.height)
                    )
                }
            }
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.review_use_this).uppercase(),
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.08f.em,
                color = Color.White
            )
            if (progress != null && remainingSeconds > 0f) {
                Text(
                    text = "${kotlin.math.ceil(remainingSeconds.toDouble()).toInt()}s",
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.08f.em,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
