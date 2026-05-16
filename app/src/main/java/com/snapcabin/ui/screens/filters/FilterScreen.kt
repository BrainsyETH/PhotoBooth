package com.snapcabin.ui.screens.filters

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snapcabin.R
import com.snapcabin.filter.PhotoFilter
import com.snapcabin.filter.PhotoOverlay
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.ChipSelectable
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing

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
        // Left: photo preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            uiState.previewPhoto?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview with filter",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(color = CabinAccent)
            }
        }

        // Right: sidebar
        Column(
            modifier = Modifier
                .width(Sidebar.width)
                .fillMaxHeight()
                .background(CabinSurface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.weight(1f)) {
                Column {
                    Eyebrow(text = stringResource(R.string.filter_title))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
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
                }

                Column {
                    Eyebrow(text = stringResource(R.string.filter_overlays))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        items(PhotoOverlay.entries) { overlay ->
                            ChipSelectable(
                                text = overlay.displayName,
                                selected = uiState.selectedOverlay == overlay,
                                onClick = { viewModel.selectOverlay(overlay) }
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                BigButton(
                    text = stringResource(R.string.filter_back),
                    onClick = onBack,
                    variant = BigButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                BigButton(
                    text = stringResource(R.string.filter_done),
                    onClick = onDone,
                    variant = BigButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
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
    val borderColor = if (isSelected) Pine else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(Radii.xs))
                .border(borderWidth, borderColor, RoundedCornerShape(Radii.xs))
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
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Pine else Espresso.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
    }
}
