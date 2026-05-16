package com.snapcabin.ui.screens.collage

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.snapcabin.collage.CollageLayout
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing

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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.previewBitmap != null) {
                Image(
                    bitmap = uiState.previewBitmap!!.asImageBitmap(),
                    contentDescription = "Collage preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "Select a layout and add photos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Espresso.copy(alpha = 0.5f)
                )
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(color = CabinAccent)
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
                Eyebrow(text = "LAYOUT")
                Spacer(modifier = Modifier.height(Spacing.sm))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    items(CollageLayout.entries) { layout ->
                        LayoutChip(
                            layout = layout,
                            isSelected = uiState.selectedLayout == layout,
                            onClick = { viewModel.selectLayout(layout) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = "Photos: ${uiState.photos.size} / ${uiState.selectedLayout.photoCount}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Espresso
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                if (viewModel.needsMorePhotos()) {
                    BigButton(
                        text = "TAKE PHOTO (${uiState.selectedLayout.photoCount - uiState.photos.size} more)",
                        onClick = onTakeMore,
                        variant = BigButtonVariant.Accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.s))
                    BigButton(
                        text = "UNDO LAST",
                        onClick = { viewModel.removeLastPhoto() },
                        variant = BigButtonVariant.Surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    text = "USE COLLAGE",
                    onClick = { onDone(viewModel.getCollageBitmap()) },
                    variant = BigButtonVariant.Secondary,
                    enabled = uiState.previewBitmap != null,
                    modifier = Modifier.weight(1f)
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
    val bgColor = if (isSelected) Pine else Cream
    val outline = if (isSelected) Pine else CabinLineStrong
    val textColor = if (isSelected) Color.White else Espresso

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radii.s))
            .background(bgColor)
            .border(1.dp, outline, RoundedCornerShape(Radii.s))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .border(
                        width = 2.dp,
                        color = Honey,
                        shape = RoundedCornerShape(Radii.s - 2.dp)
                    )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = layout.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "${layout.photoCount} photo${if (layout.photoCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
