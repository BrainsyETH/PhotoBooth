package com.photobooth.ui.screens.filters

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photobooth.filter.PhotoFilter
import com.photobooth.filter.PhotoOverlay
import com.photobooth.ui.components.BigButton
import com.photobooth.ui.theme.BoothAccent
import com.photobooth.ui.theme.BoothPrimary
import com.photobooth.ui.theme.BoothSecondary

@Composable
fun FilterScreen(
    photo: Bitmap?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(photo) {
        photo?.let { viewModel.setOriginalPhoto(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left side: photo preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            uiState.previewPhoto?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview with filter",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(color = BoothAccent)
            }
        }

        // Right side: filter & overlay selectors + buttons
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Filters section
                Text(
                    text = "FILTERS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PhotoFilter.entries) { filter ->
                        FilterThumbnail(
                            filter = filter,
                            thumbnail = uiState.filterThumbnails[filter],
                            isSelected = uiState.selectedFilter == filter,
                            onClick = { viewModel.selectFilter(filter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overlays section
                Text(
                    text = "OVERLAYS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PhotoOverlay.entries) { overlay ->
                        OverlayChip(
                            overlay = overlay,
                            isSelected = uiState.selectedOverlay == overlay,
                            onClick = { viewModel.selectOverlay(overlay) }
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigButton(
                    text = "BACK",
                    onClick = onBack,
                    containerColor = MaterialTheme.colorScheme.surface
                )
                BigButton(
                    text = "DONE",
                    onClick = onDone,
                    containerColor = BoothSecondary
                )
            }
        }
    }
}

@Composable
private fun FilterThumbnail(
    filter: PhotoFilter,
    thumbnail: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) BoothAccent else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = filter.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) BoothAccent else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun OverlayChip(
    overlay: PhotoOverlay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) BoothPrimary else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = overlay.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
