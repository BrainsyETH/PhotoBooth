package com.photobooth.ui.screens.gif

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.photobooth.ui.components.BigButton
import com.photobooth.ui.theme.BoothAccent
import com.photobooth.ui.theme.BoothPrimary
import com.photobooth.ui.theme.BoothSecondary
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun GifScreen(
    initialFrame: Bitmap?,
    onTakeMore: () -> Unit,
    onDone: (File?) -> Unit,
    onCancel: () -> Unit,
    viewModel: GifViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialFrame) {
        if (initialFrame != null && uiState.frames.isEmpty()) {
            viewModel.addFrame(initialFrame)
        }
    }

    // Animate preview
    LaunchedEffect(uiState.frames.size) {
        while (uiState.frames.size >= 2) {
            delay(uiState.delayMs.toLong())
            viewModel.advancePreview()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left: frame preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val frameToShow = uiState.frames.getOrNull(uiState.previewFrameIndex)
            if (frameToShow != null) {
                Image(
                    bitmap = frameToShow.asImageBitmap(),
                    contentDescription = "GIF frame preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Take photos to create a GIF",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            if (uiState.isEncoding) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BoothAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Creating GIF...", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Right: controls
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CREATE GIF",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Frames: ${uiState.frames.size} / ${uiState.maxFrames}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.frames.size < uiState.maxFrames) {
                    BigButton(
                        text = "TAKE FRAME",
                        onClick = onTakeMore,
                        containerColor = BoothAccent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.frames.isNotEmpty()) {
                    BigButton(
                        text = "UNDO LAST",
                        onClick = { viewModel.removeLastFrame() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Speed control
                Text(
                    text = "Speed: ${uiState.delayMs}ms per frame",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    value = uiState.delayMs.toFloat(),
                    onValueChange = { viewModel.setDelay(it.toInt()) },
                    valueRange = 100f..2000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = BoothAccent,
                        activeTrackColor = BoothPrimary
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigButton(
                    text = "CANCEL",
                    onClick = onCancel,
                    containerColor = MaterialTheme.colorScheme.surface
                )
                BigButton(
                    text = "CREATE GIF",
                    onClick = {
                        viewModel.encodeGif(context)
                    },
                    containerColor = BoothSecondary,
                    enabled = uiState.frames.size >= 2 && !uiState.isEncoding
                )
            }
        }
    }

    // When GIF is ready, notify parent
    LaunchedEffect(uiState.gifFile) {
        uiState.gifFile?.let { onDone(it) }
    }
}
