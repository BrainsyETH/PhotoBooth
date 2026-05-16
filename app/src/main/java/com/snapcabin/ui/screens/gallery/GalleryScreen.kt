package com.snapcabin.ui.screens.gallery

import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing

@Composable
fun GalleryScreen(
    onPhotoSelected: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    viewModel: GalleryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadPhotos()
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
            if (uiState.selectedPhoto != null && uiState.fullBitmap != null) {
                Image(
                    bitmap = uiState.fullBitmap!!.asImageBitmap(),
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(color = CabinAccent)
            } else if (uiState.photos.isEmpty()) {
                Text(
                    text = "No photos yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Espresso.copy(alpha = 0.5f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(uiState.photos) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Photo from ${DateUtils.getRelativeTimeSpanString(photo.timestamp * 1000)}",
                            modifier = Modifier
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(Radii.xs))
                                .clickable { viewModel.selectPhoto(photo) },
                            contentScale = ContentScale.Crop
                        )
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
                Eyebrow(text = "GALLERY")
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "${uiState.photos.size} photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Espresso.copy(alpha = 0.72f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (uiState.selectedPhoto != null) {
                    BigButton(
                        text = "USE THIS PHOTO",
                        onClick = {
                            uiState.fullBitmap?.let { onPhotoSelected(it) }
                        },
                        variant = BigButtonVariant.Accent,
                        enabled = uiState.fullBitmap != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BigButton(
                        text = "BACK TO GRID",
                        onClick = { viewModel.clearSelection() },
                        variant = BigButtonVariant.Surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BigButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    variant = BigButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
