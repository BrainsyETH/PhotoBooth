package com.snapcabin.ui.screens.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snapcabin.R
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HoneyDeep
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.ShareDenim
import com.snapcabin.ui.theme.ShareLeaf
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun ShareScreen(
    photo: Bitmap?,
    onDone: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showSmsDialog by remember { mutableStateOf(false) }
    var smsPhoneInput by remember { mutableStateOf("") }

    LaunchedEffect(photo) {
        photo?.let { viewModel.setPhoto(it, context) }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Stop the LAN photo-server foreground service when leaving Share.
            com.snapcabin.share.PhotoShareService.stop(context)
        }
    }

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
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            photo?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.share_photo_desc),
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Right: sidebar
        Column(
            modifier = Modifier
                .width(Sidebar.width)
                .fillMaxHeight()
                .background(CabinSurface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_title),
                    fontSize = 34.sp,
                    fontFamily = FrankRuhlLibre,
                    fontWeight = FontWeight.Bold,
                    color = Espresso
                )

                // QR Code block (gated on admin toggle)
                if (settings.enableQrSharing) {
                    uiState.qrCodeBitmap?.let { qr ->
                        Text(
                            text = stringResource(R.string.share_scan_download),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Espresso.copy(alpha = 0.72f)
                        )
                        Box(
                            modifier = Modifier
                                .size(172.dp)
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(Radii.s))
                                .clip(RoundedCornerShape(Radii.s))
                                .background(Color.White)
                                .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = stringResource(R.string.share_qr_desc),
                                modifier = Modifier.size(144.dp)
                            )
                        }
                        uiState.shareUrl?.let { url ->
                            Text(
                                text = url,
                                style = MaterialTheme.typography.labelSmall,
                                color = HoneyDeep
                            )
                        }
                    }
                }

                if (settings.enableSaveToGallery) {
                    BigButton(
                        text = stringResource(R.string.share_save_gallery),
                        onClick = { viewModel.saveToGallery(context) },
                        variant = BigButtonVariant.Primary,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (settings.enableShareIntent) {
                    BigButton(
                        text = stringResource(R.string.share_button),
                        onClick = { viewModel.shareViaIntent(context) },
                        variant = BigButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (settings.enablePrint) {
                    BigButton(
                        text = stringResource(R.string.share_print),
                        onClick = { viewModel.printPhoto(context) },
                        variant = BigButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (settings.enableEmail) {
                    BigButton(
                        text = stringResource(R.string.share_email),
                        onClick = { viewModel.shareViaEmail(context) },
                        containerColor = ShareDenim,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (settings.enableSms && settings.twilioEnabled) {
                    BigButton(
                        text = stringResource(R.string.share_message),
                        onClick = { showSmsDialog = true },
                        containerColor = ShareLeaf,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Done + message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.message?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(bottom = Spacing.s),
                        shape = RoundedCornerShape(Radii.xs)
                    ) {
                        Text(text = msg)
                    }
                }

                BigButton(
                    text = stringResource(R.string.share_done),
                    onClick = onDone,
                    variant = BigButtonVariant.Surface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showSmsDialog) {
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("Text me my photo") },
            text = {
                Column {
                    Text(
                        text = "Enter your mobile number. Standard message rates may apply.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Espresso.copy(alpha = 0.72f)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(Spacing.s))
                    OutlinedTextField(
                        value = smsPhoneInput,
                        onValueChange = { input ->
                            // Allow only digits, +, spaces, dashes, parens; cap length
                            smsPhoneInput = input.filter { c ->
                                c.isDigit() || c == '+' || c == ' ' || c == '-' || c == '(' || c == ')'
                            }.take(20)
                        },
                        label = { Text("Phone (e.g. +15551234567)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.s),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShareLeaf,
                            unfocusedBorderColor = CabinLine,
                            focusedLabelColor = ShareLeaf,
                            cursorColor = ShareLeaf
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendViaTwilio(smsPhoneInput)
                        smsPhoneInput = ""
                        showSmsDialog = false
                    },
                    enabled = smsPhoneInput.isNotBlank()
                ) { Text("SEND") }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
