package com.photobooth.ui.screens.collage

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.photobooth.collage.CollageLayout
import com.photobooth.ui.components.BigButton
import com.photobooth.ui.theme.BoothAccent
import com.photobooth.ui.theme.BoothPrimary
import com.photobooth.ui.theme.BoothSecondary

@Composable
fun CollageScreen(
    initialPhoto: Bitmap?,
    onTakeMore: () -> Unit,
    onDone: (Bitmap?) -> Unit,
    onCancel: () -> Unit,
    viewModel: CollageViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialPhoto) {
        if (initialPhoto != null && uiState.photos.isEmpty()) {
            viewModel.addPhoto(initialPhoto)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left: collage preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.previewBitmap != null) {
                Image(
                    bitmap = uiState.previewBitmap!!.asImageBitmap(),
                    contentDescription = "Collage preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Select a layout and add photos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(color = BoothAccent)
            }
        }

        // Right: layout selector + controls
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LAYOUT",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CollageLayout.entries) { layout ->
                        LayoutChip(
                            layout = layout,
                            isSelected = uiState.selectedLayout == layout,
                            onClick = { viewModel.selectLayout(layout) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Photo count indicator
                Text(
                    text = "Photos: ${uiState.photos.size} / ${uiState.selectedLayout.photoCount}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.needsMorePhotos()) {
                    BigButton(
                        text = "TAKE PHOTO (${uiState.selectedLayout.photoCount - uiState.photos.size} more)",
                        onClick = onTakeMore,
                        containerColor = BoothAccent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BigButton(
                        text = "UNDO LAST",
                        onClick = { viewModel.removeLastPhoto() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom buttons
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
                    text = "USE COLLAGE",
                    onClick = { onDone(viewModel.getCollageBitmap()) },
                    containerColor = BoothSecondary,
                    enabled = uiState.previewBitmap != null
                )
            }
        }
    }
}

@Composable
private fun LayoutChip(
    layout: CollageLayout,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) BoothPrimary else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) BoothAccent else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = layout.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "${layout.photoCount} photo${if (layout.photoCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
