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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.theme.BoothAccent
import com.snapcabin.ui.theme.BoothPrimary
import com.snapcabin.ui.theme.BoothSecondary

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
        // Left: photo grid or selected photo
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.selectedPhoto != null && uiState.fullBitmap != null) {
                // Full photo view
                Image(
                    bitmap = uiState.fullBitmap!!.asImageBitmap(),
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(color = BoothAccent)
            } else if (uiState.photos.isEmpty()) {
                Text(
                    text = "No photos yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.photos) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Photo from ${DateUtils.getRelativeTimeSpanString(photo.timestamp * 1000)}",
                            modifier = Modifier
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectPhoto(photo) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Right: controls
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GALLERY",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.photos.size} photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Column {
                if (uiState.selectedPhoto != null) {
                    BigButton(
                        text = "USE THIS PHOTO",
                        onClick = {
                            uiState.fullBitmap?.let { onPhotoSelected(it) }
                        },
                        containerColor = BoothAccent,
                        enabled = uiState.fullBitmap != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BigButton(
                        text = "BACK TO GRID",
                        onClick = { viewModel.clearSelection() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                BigButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    containerColor = BoothSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
