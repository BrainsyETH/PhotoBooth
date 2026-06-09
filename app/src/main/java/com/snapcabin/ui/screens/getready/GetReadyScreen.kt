package com.snapcabin.ui.screens.getready

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snapcabin.R
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.BrandingLiveOverlay
import com.snapcabin.ui.components.FramingGuide
import com.snapcabin.ui.screens.capture.CaptureMode
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Walnut

@Composable
fun GetReadyScreen(
    mode: CaptureMode,
    onStart: () -> Unit,
    onBack: () -> Unit,
    viewModel: GetReadyViewModel = hiltViewModel()
) {
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

    val eyebrow = when (mode) {
        CaptureMode.Single -> stringResource(R.string.getready_card_eyebrow_single)
        CaptureMode.Collage -> stringResource(R.string.getready_card_eyebrow_collage)
        CaptureMode.Gif -> stringResource(R.string.getready_card_eyebrow_gif)
    }
    val body = when (mode) {
        CaptureMode.Single -> stringResource(R.string.getready_card_body_single)
        CaptureMode.Collage -> stringResource(
            R.string.getready_card_body_collage,
            settings.collageShotCount
        )
        CaptureMode.Gif -> stringResource(
            R.string.getready_card_body_gif,
            settings.gifFrameCount
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onStart() }
    ) {
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
        }

        // Soft vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Espresso.copy(alpha = 0f),
                            Espresso.copy(alpha = 0.55f)
                        ),
                        radius = 1400f
                    )
                )
        )

        // Branding overlay — shows the corner logo over the live preview as an
        // alignment guide. No-op when no overlay is configured or in stretch mode.
        BrandingLiveOverlay(
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )

        if (settings.framingGuideEnabled) {
            FramingGuide()

            // Framing OK pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Cream.copy(alpha = 0.94f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Pine)
                )
                Text(
                    text = stringResource(R.string.getready_framing_ok).uppercase(),
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.18f.em,
                    color = Espresso
                )
            }
        }

        // Bottom instructions card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 32.dp, end = 32.dp)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(24.dp))
                .clickable { /* swallow taps on the card without triggering start */ }
                .padding(start = 36.dp, end = 36.dp, top = 28.dp, bottom = 30.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    fontFamily = HankenGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.28f.em,
                    color = Walnut
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.getready_card_headline),
                    fontFamily = FrankRuhlLibre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.015f).em,
                    color = Espresso
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    fontFamily = HankenGrotesk,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    color = Espresso.copy(alpha = 0.72f)
                )
            }
            BigButton(
                text = stringResource(R.string.getready_ready),
                onClick = onStart,
                variant = BigButtonVariant.Primary
            )
        }

        // Back link, bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 22.dp)
                .clickable { onBack() }
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.getready_change_mode).uppercase(),
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.22f.em,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
