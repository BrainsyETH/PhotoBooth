package com.snapcabin.ui.screens.gif

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            val frameToShow = uiState.frames.getOrNull(uiState.previewFrameIndex)
            if (frameToShow != null) {
                Image(
                    bitmap = frameToShow.asImageBitmap(),
                    contentDescription = "GIF frame preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Take photos to create a GIF",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Espresso.copy(alpha = 0.5f)
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
                        CircularProgressIndicator(color = CabinAccent)
                        Spacer(modifier = Modifier.height(Spacing.s))
                        Text("Creating GIF...", color = Espresso)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .width(Sidebar.width)
                .fillMaxHeight()
                .background(CabinSurface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(text = "CREATE GIF")

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "Frames: ${uiState.frames.size} / ${uiState.maxFrames}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Espresso
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                if (uiState.frames.size < uiState.maxFrames) {
                    BigButton(
                        text = "TAKE FRAME",
                        onClick = onTakeMore,
                        variant = BigButtonVariant.Accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.s))
                }

                if (uiState.frames.isNotEmpty()) {
                    BigButton(
                        text = "UNDO LAST",
                        onClick = { viewModel.removeLastFrame() },
                        variant = BigButtonVariant.Surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = "Speed: ${uiState.delayMs}ms per frame",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Espresso
                )
                Slider(
                    value = uiState.delayMs.toFloat(),
                    onValueChange = { viewModel.setDelay(it.toInt()) },
                    valueRange = 100f..2000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = CabinAccent,
                        activeTrackColor = CabinPrimary,
                        inactiveTrackColor = Oat
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                BigButton(
                    text = "CANCEL",
                    onClick = onCancel,
                    variant = BigButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                BigButton(
                    text = "CREATE GIF",
                    onClick = { viewModel.encodeGif(context) },
                    variant = BigButtonVariant.Secondary,
                    enabled = uiState.frames.size >= 2 && !uiState.isEncoding,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    LaunchedEffect(uiState.gifFile) {
        uiState.gifFile?.let { onDone(it) }
    }
}
