package com.snapcabin.ui.screens.share

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snapcabin.R
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSecondary
import kotlinx.coroutines.delay

@Composable
fun ShareScreen(
    photo: Bitmap?,
    onDone: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(photo) {
        photo?.let { viewModel.setPhoto(it, context) }
    }

    // Auto-dismiss message
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(2000)
            viewModel.clearMessage()
        }
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            photo?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.share_photo_desc),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Right: share options (scrollable for all the buttons)
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.share_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code
                uiState.qrCodeBitmap?.let { qr ->
                    Text(
                        text = stringResource(R.string.share_scan_download),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = stringResource(R.string.share_qr_desc),
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    uiState.shareUrl?.let { url ->
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = CabinAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Primary actions
                BigButton(
                    text = stringResource(R.string.share_save_gallery),
                    onClick = { viewModel.saveToGallery(context) },
                    containerColor = CabinPrimary,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                BigButton(
                    text = stringResource(R.string.share_button),
                    onClick = { viewModel.shareViaIntent(context) },
                    containerColor = CabinSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Print
                BigButton(
                    text = "PRINT",
                    onClick = { viewModel.printPhoto(context) },
                    containerColor = Color(0xFF5D4037),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                BigButton(
                    text = "EMAIL",
                    onClick = { viewModel.shareViaEmail(context) },
                    containerColor = Color(0xFF2980B9),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Message/SMS
                BigButton(
                    text = "MESSAGE",
                    onClick = { viewModel.shareViaSms(context) },
                    containerColor = Color(0xFF27AE60),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Done button + message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.message?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = msg)
                    }
                }

                BigButton(
                    text = stringResource(R.string.share_done),
                    onClick = onDone,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
