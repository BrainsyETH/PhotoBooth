package com.snapcabin.ui.screens.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snapcabin.ui.components.BrandingLiveOverlay
import com.snapcabin.ui.components.CoachLine
import com.snapcabin.ui.components.FlashOverlay
import com.snapcabin.ui.components.LookHereIndicator
import com.snapcabin.ui.components.lensPositionFrom
import com.snapcabin.ui.components.IndicatorMode
import com.snapcabin.ui.components.ShotIndicator
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk

@Composable
fun CaptureScreen(
    mode: CaptureMode,
    onDone: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !uiState.isFinished && uiState.shotsTaken == 0) {
            viewModel.startBurst(mode)
        }
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onDone()
        }
    }

    val step = uiState.currentStep
    val vignetteAlpha by animateFloatAsState(
        targetValue = when (step) {
            is CaptureStep.Count -> 0.7f
            is CaptureStep.Intro -> 0.4f
            is CaptureStep.Between -> 0.45f
            else -> 0.45f
        },
        animationSpec = tween(durationMillis = 400),
        label = "vignette"
    )

    val flashColor = remember(settings.flashColor) {
        settings.flashColor.toColorOrCream()
    }

    val indicatorMode = when (mode) {
        CaptureMode.Single -> IndicatorMode.Single
        CaptureMode.Collage -> IndicatorMode.Collage
        CaptureMode.Gif -> IndicatorMode.Gif
    }
    val kindLabel = when (mode) {
        CaptureMode.Single -> "Single Photo"
        CaptureMode.Collage -> "Collage"
        CaptureMode.Gif -> "GIF"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                        viewModel.cameraManager.bindCamera(
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            useFront = settings.useFrontCamera,
                            cameraId = settings.cameraId,
                            mirror = settings.mirrorImage,
                            maxResolution = settings.photoResolution.maxDimension
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Warm radial vignette using espresso, opacity ramped per step.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Espresso.copy(alpha = 0f),
                                Espresso.copy(alpha = vignetteAlpha)
                            ),
                            radius = 1400f
                        )
                    )
            )

            // Branding overlay — corner logo sits over the live preview as a
            // composition guide so guests can see where the logo will land.
            // Hidden for collage: its final 16:9 frame places the logo
            // differently from a single 4:3 capture, so the guide would lie.
            if (mode != CaptureMode.Collage) {
                BrandingLiveOverlay(
                    settings = settings,
                    modifier = Modifier.fillMaxSize()
                )
            }

            ShotIndicator(
                mode = indicatorMode,
                shotCount = uiState.shotsTaken,
                totalShots = uiState.totalShots,
                kindLabel = kindLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 24.dp)
            )

            // "Look here" lens pointer — only during the count so it doesn't
            // distract while the guest is just framing.
            if (step is CaptureStep.Count) {
                LookHereIndicator(
                    position = lensPositionFrom(settings.cameraLensPosition)
                )
            }

            // Center stage — content varies per step
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val s = step) {
                    is CaptureStep.Intro -> {
                        if (s.coach.isNotBlank()) {
                            CoachLine(text = s.coach)
                        }
                    }
                    is CaptureStep.Count -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (s.coach.isNotBlank()) {
                                Text(
                                    text = s.coach,
                                    style = TextStyle(
                                        fontFamily = FrankRuhlLibre,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 38.sp,
                                        lineHeight = 42.sp,
                                        color = Cream.copy(alpha = 0.94f),
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.55f),
                                            blurRadius = 24f
                                        )
                                    )
                                )
                            }
                            Text(
                                text = s.n.toString(),
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Assertive
                                },
                                style = TextStyle(
                                    fontFamily = FrankRuhlLibre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (mode == CaptureMode.Gif) 200.sp else 280.sp,
                                    lineHeight = if (mode == CaptureMode.Gif) 200.sp else 280.sp,
                                    color = Cream,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        blurRadius = 60f
                                    )
                                )
                            )
                        }
                    }
                    is CaptureStep.Between -> {
                        if (s.coach.isNotBlank()) {
                            CoachLine(text = s.coach)
                        }
                    }
                    is CaptureStep.Flash, CaptureStep.Done -> Unit
                }
            }

            // Skip pill — bottom-left. Scrim-backed so it stays legible over a
            // bright camera feed, with a finger-sized hit area.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 22.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Espresso.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                    .clickable { viewModel.skipBurst() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SKIP",
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.22f.em,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }

            FlashOverlay(trigger = uiState.showFlash, color = flashColor)

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        } else {
            Text(
                text = "Camera permission is required",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

internal fun String.toColorOrCream(): Color {
    return try {
        val hex = if (startsWith("#")) substring(1) else this
        val value = when (hex.length) {
            6 -> 0xFF000000L or hex.toLong(16)
            8 -> hex.toLong(16)
            else -> return Cream
        }
        Color(value.toInt())
    } catch (e: Exception) {
        Cream
    }
}
