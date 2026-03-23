package com.photobooth.ui.screens.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.photobooth.ui.components.BigButton
import com.photobooth.ui.components.CountdownOverlay
import com.photobooth.ui.components.FlashOverlay
import com.photobooth.ui.theme.BoothSecondary

@Composable
fun CaptureScreen(
    onPhotoCaptured: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
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

    // Navigate when photo is captured
    LaunchedEffect(uiState.capturedPhoto) {
        if (uiState.capturedPhoto != null) {
            onPhotoCaptured()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        // Use scaleType that works well across different screen ratios
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                        viewModel.cameraManager.bindCamera(
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            useFront = settings.useFrontCamera,
                            cameraId = settings.cameraId,
                            mirror = settings.mirrorFrontCamera,
                            maxResolution = settings.photoResolution.maxDimension
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Flash effect overlay
            FlashOverlay(trigger = uiState.showFlash)

            // Countdown overlay
            if (uiState.isCountingDown) {
                CountdownOverlay(count = uiState.countdownValue)
            }

            // Capture button
            if (!uiState.isCountingDown && !uiState.isCapturing && uiState.capturedPhoto == null) {
                BigButton(
                    text = "TAKE PHOTO",
                    onClick = { viewModel.startCountdown() },
                    containerColor = BoothSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }

            // Error message
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
            // Permission not granted
            Text(
                text = "Camera permission is required",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Camera cleanup handled by lifecycle
        }
    }
}
