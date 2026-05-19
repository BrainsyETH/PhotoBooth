package com.snapcabin.ui.screens.share

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snapcabin.R
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.rememberScreenClass
import com.snapcabin.ui.components.scaledDp
import com.snapcabin.ui.components.sidebarWidth
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HoneyDeep
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.ShareDenim
import com.snapcabin.ui.theme.ShareLeaf
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut
import kotlinx.coroutines.delay

@Composable
fun ShareScreen(
    photo: Bitmap?,
    onSessionEnd: () -> Unit,
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

    // The ViewModel is the single source of truth for "we're done." It emits
    // SessionEnded once (Done tap or any other trigger) and the NavGraph
    // navigates back to Attract here.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShareEvent.SessionEnded -> onSessionEnd()
            }
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            delay(2000)
            viewModel.clearMessage()
        }
    }

    val screen = rememberScreenClass()
    val sidebarWidthDp = screen.sidebarWidth().dp
    val titleSize = screen.scaledDp(34).sp
    val qrBoxSize = screen.scaledDp(172).dp
    val qrImageSize = screen.scaledDp(144).dp
    val sidebarPadding = screen.scaledDp(28).dp

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .width(sidebarWidthDp)
                    .fillMaxHeight()
                    .background(CabinSurface)
                    .padding(sidebarPadding),
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
                        fontSize = titleSize,
                        fontFamily = FrankRuhlLibre,
                        fontWeight = FontWeight.Bold,
                        color = Espresso
                    )

                    // QR is now driven by the Cloudinary upload. The host
                    // toggles "QR code" in admin to opt in; the section only
                    // renders once Cloudinary returns a public URL.
                    if (settings.enableQrSharing) {
                        QrSharingBlock(
                            isUploading = uiState.isUploading,
                            qrBitmap = uiState.qrCodeBitmap,
                            shareUrl = uiState.shareUrl,
                            cloudinaryConfigured = settings.cloudinaryEnabled &&
                                settings.cloudinaryCloudName.isNotBlank() &&
                                settings.cloudinaryUploadPreset.isNotBlank(),
                            qrBoxSize = qrBoxSize,
                            qrImageSize = qrImageSize
                        )
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
                        onClick = { viewModel.endSession() },
                        variant = BigButtonVariant.Surface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Thank-you overlay. Renders on top of the share screen instead of
        // pushing a new navigation destination, so there's only one set of
        // navigation transitions per session.
        AnimatedVisibility(
            visible = uiState.endingSession,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ThankYouOverlay()
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
                    Spacer(modifier = Modifier.height(Spacing.s))
                    OutlinedTextField(
                        value = smsPhoneInput,
                        onValueChange = { input ->
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

@Composable
private fun QrSharingBlock(
    isUploading: Boolean,
    qrBitmap: Bitmap?,
    shareUrl: String?,
    cloudinaryConfigured: Boolean,
    qrBoxSize: androidx.compose.ui.unit.Dp,
    qrImageSize: androidx.compose.ui.unit.Dp
) {
    when {
        !cloudinaryConfigured -> {
            // Admin hasn't configured Cloudinary. Show a quiet hint instead of
            // letting the slot disappear; otherwise a host who toggled QR on
            // would think the feature was broken.
            Text(
                text = "QR sharing needs Cloudinary. Add it under TWILIO SMS · CLOUDINARY in admin.",
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.55f)
            )
        }
        isUploading -> {
            Text(
                text = stringResource(R.string.share_scan_download),
                style = MaterialTheme.typography.bodyMedium,
                color = Espresso.copy(alpha = 0.72f)
            )
            Box(
                modifier = Modifier
                    .size(qrBoxSize)
                    .clip(RoundedCornerShape(Radii.s))
                    .background(Color.White)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.s)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Pine, strokeWidth = 3.dp)
            }
            Text(
                text = "Uploading your photo…",
                style = MaterialTheme.typography.labelSmall,
                color = Espresso.copy(alpha = 0.55f)
            )
        }
        qrBitmap != null -> {
            Text(
                text = stringResource(R.string.share_scan_download),
                style = MaterialTheme.typography.bodyMedium,
                color = Espresso.copy(alpha = 0.72f)
            )
            Box(
                modifier = Modifier
                    .size(qrBoxSize)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(Radii.s))
                    .clip(RoundedCornerShape(Radii.s))
                    .background(Color.White)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.share_qr_desc),
                    modifier = Modifier.size(qrImageSize)
                )
            }
            shareUrl?.let { url ->
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = HoneyDeep,
                    maxLines = 2
                )
            }
        }
        else -> {
            // Cloudinary is configured but the upload failed and we never got
            // a URL. The error message already showed in the snackbar; leave
            // the slot blank to avoid double-reporting.
        }
    }
}

@Composable
private fun ThankYouOverlay() {
    val screen = rememberScreenClass()
    val titleSize = screen.scaledDp(140).sp
    val subtitleSize = screen.scaledDp(30).sp

    val backdrop = Brush.radialGradient(
        colors = listOf(CabinSurface, Parchment, Oat),
        radius = 1500f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground)
            .background(backdrop),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Thank you.",
                fontSize = titleSize,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02f).em
            )
            Spacer(modifier = Modifier.height(Spacing.lg + Spacing.xs))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .background(Walnut.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(Spacing.lg + Spacing.xs))
            Text(
                text = "Your photo is ready",
                fontSize = subtitleSize,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = Walnut,
                textAlign = TextAlign.Center
            )
        }
    }
}
