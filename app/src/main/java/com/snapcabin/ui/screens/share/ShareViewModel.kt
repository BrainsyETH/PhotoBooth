package com.snapcabin.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.event.SendLog
import com.snapcabin.event.SendLogEntry
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.filter.WatermarkRenderer
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.SettingsManager
import com.snapcabin.share.CloudinaryUploader
import com.snapcabin.share.PhotoPrinter
import com.snapcabin.share.PhotoSaver
import com.snapcabin.share.QrCodeGenerator
import com.snapcabin.share.ResendEmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShareUiState(
    val photo: Bitmap? = null,
    val qrCodeBitmap: Bitmap? = null,
    val savedPath: String? = null,
    val isSaving: Boolean = false,
    /** True while the Cloudinary upload is in flight. */
    val isUploading: Boolean = false,
    /** Public Cloudinary URL once the upload succeeds. Empty when Cloudinary is off. */
    val shareUrl: String? = null,
    /** Thank-you overlay is rendered when this is true. */
    val endingSession: Boolean = false,
    val message: String? = null
)

/**
 * One-shot navigation events. The NavGraph collects these and reacts; the
 * ViewModel is the single source of truth for "we're done, go home."
 */
sealed interface ShareEvent {
    data object SessionEnded : ShareEvent
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val photoSaver: PhotoSaver,
    private val qrCodeGenerator: QrCodeGenerator,
    private val settingsManager: SettingsManager,
    private val photoPrinter: PhotoPrinter,
    private val resendEmailSender: ResendEmailSender,
    private val cloudinaryUploader: CloudinaryUploader
) : ViewModel() {

    companion object {
        private const val TAG = "ShareViewModel"
        /** How long the Thank You overlay stays before we navigate back to Attract. */
        private const val THANK_YOU_DURATION_MS = 3_000L
    }

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /**
     * Single-shot event channel for navigation. Channel + receiveAsFlow ensures
     * each event is delivered exactly once, even across configuration changes.
     */
    private val _events = Channel<ShareEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    /** Guards against double-ending if Done is tapped twice or races with the timer. */
    private var sessionEndScheduled: Boolean = false

    fun setPhoto(bitmap: Bitmap, context: Context) {
        // New photo — invalidate previous upload + per-session counter.
        cachedPublicUrl = null
        emailSendsThisSession = 0
        sessionEndScheduled = false
        viewModelScope.launch {
            // Bake in admin-configured branding (border + overlay PNGs, then watermark text)
            val processedPhoto = withContext(Dispatchers.Default) {
                val s = settings.value
                val branded = CustomBrandingRenderer.apply(
                    source = bitmap,
                    borderPath = s.customBorderPath,
                    overlayPath = s.customOverlayPath
                )
                if (s.watermarkEnabled && s.watermarkText.isNotBlank()) {
                    WatermarkRenderer.apply(branded, s.watermarkText)
                } else {
                    branded
                }
            }

            _uiState.value = _uiState.value.copy(photo = processedPhoto)

            // Auto-save if enabled.
            if (settings.value.autoSaveToGallery) {
                saveToGallery(context)
            }

            // Eager Cloudinary upload so QR sharing has a URL ready. Without
            // Cloudinary the QR section stays hidden — guests can still use
            // Save / Share / Print / Email.
            val s = settings.value
            if (s.cloudinaryEnabled && s.cloudinaryCloudName.isNotBlank() &&
                s.cloudinaryUploadPreset.isNotBlank()
            ) {
                uploadToCloudinary(s, processedPhoto)
            }
        }
    }

    private suspend fun uploadToCloudinary(s: BoothSettings, photo: Bitmap) {
        _uiState.value = _uiState.value.copy(isUploading = true)
        val result = cloudinaryUploader.upload(
            cloudName = s.cloudinaryCloudName,
            uploadPreset = s.cloudinaryUploadPreset,
            bitmap = photo,
            folder = s.currentEventSlug.takeIf { it.isNotBlank() }?.let { "events/$it" }
        )
        when (result) {
            is CloudinaryUploader.Result.Ok -> {
                cachedPublicUrl = result.secureUrl
                val qr = withContext(Dispatchers.Default) {
                    qrCodeGenerator.generate(result.secureUrl)
                }
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    shareUrl = result.secureUrl,
                    qrCodeBitmap = qr
                )
            }
            is CloudinaryUploader.Result.Err -> {
                Log.w(TAG, "Cloudinary upload failed: ${result.message}")
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    message = result.message
                )
            }
        }
    }

    fun saveToGallery(context: Context) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val quality = settings.value.outputQuality
            val path = withContext(Dispatchers.IO) {
                photoSaver.saveToGallery(context, photo, quality = quality)
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedPath = path,
                message = if (path != null) "Saved to gallery!" else "Failed to save"
            )
        }
    }

    fun shareViaIntent(context: Context) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                photoSaver.saveToCacheForSharing(context, photo)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Photo"))
        }
    }

    fun printPhoto(context: Context) {
        val photo = _uiState.value.photo ?: return
        photoPrinter.print(context, photo)
    }

    /**
     * The Done button (and any other "we're done here" caller) goes through
     * here. We show the Thank You overlay, then emit SessionEnded once. Calling
     * this more than once is a no-op so the user can spam Done safely.
     */
    fun endSession() {
        if (sessionEndScheduled) return
        sessionEndScheduled = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(endingSession = true)
            kotlinx.coroutines.delay(THANK_YOU_DURATION_MS)
            _events.send(ShareEvent.SessionEnded)
        }
    }

    // ────── Resend email ──────
    private var emailSendsThisSession: Int = 0
    /** Cached Cloudinary URL for the current photo (optional, included in the email body when present). */
    private var cachedPublicUrl: String? = null
    /** Per-event count of emails sent to each address. Resets when the current event changes. */
    private val perAddressSendCounts: MutableMap<String, Int> = mutableMapOf()
    private var perAddressCountsForEvent: String = ""

    fun sendViaEmail(rawAddress: String) {
        val s = settings.value
        if (!s.resendEnabled) {
            _uiState.value = _uiState.value.copy(message = "Email delivery isn't enabled.")
            return
        }
        if (emailSendsThisSession >= s.resendMaxPerSession.coerceAtLeast(1)) {
            _uiState.value = _uiState.value.copy(message = "Email limit reached for this session.")
            return
        }
        val to = rawAddress.trim()
        if (!resendEmailSender.isValidEmail(to)) {
            _uiState.value = _uiState.value.copy(message = "Enter a valid email address.")
            return
        }
        if (perAddressCountsForEvent != s.currentEventSlug) {
            perAddressSendCounts.clear()
            perAddressCountsForEvent = s.currentEventSlug
        }
        val alreadySentToThisAddress = perAddressSendCounts[to] ?: 0
        if (alreadySentToThisAddress >= s.resendMaxPerAddress.coerceAtLeast(1)) {
            _uiState.value = _uiState.value.copy(
                message = "Already sent $alreadySentToThisAddress times to that address this event."
            )
            return
        }

        val photo = _uiState.value.photo
        if (photo == null) {
            _uiState.value = _uiState.value.copy(message = "Photo isn't ready yet.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Sending to $to…")

            val htmlBody = buildHtmlBody(s, cachedPublicUrl)
            val subject = s.resendSubject.ifBlank { "Your photo from the booth" }

            val result = resendEmailSender.send(
                apiKey = s.resendApiKey,
                fromAddress = s.resendFromAddress,
                toAddress = to,
                subject = subject,
                htmlBody = htmlBody,
                photo = photo
            )
            when (result) {
                is ResendEmailSender.Result.Ok -> {
                    emailSendsThisSession++
                    perAddressSendCounts[to] = (perAddressSendCounts[to] ?: 0) + 1
                    appendToSendLog(s, "email", SendLog.maskEmail(to), "ok", note = "")
                    _uiState.value = _uiState.value.copy(message = "Sent to $to ✓")
                }
                is ResendEmailSender.Result.Err -> {
                    appendToSendLog(s, "email", SendLog.maskEmail(to), "err", note = result.message)
                    _uiState.value = _uiState.value.copy(message = result.message)
                }
            }
        }
    }

    private fun buildHtmlBody(s: BoothSettings, publicUrl: String?): String {
        val eventLine = if (s.eventName.isNotBlank()) {
            " from <strong>${escapeHtml(s.eventName)}</strong>"
        } else {
            ""
        }
        val linkLine = publicUrl?.let {
            "<p style=\"color:#7a6a4f;font-size:14px;\">If the attachment doesn't come through, here's a link: <a href=\"${escapeHtml(it)}\">${escapeHtml(it)}</a></p>"
        }.orEmpty()
        return """
            <div style="font-family:Helvetica,Arial,sans-serif;color:#3a2e20;">
              <p>Your photo is attached$eventLine.</p>
              <p>Save it, share it, treasure it.</p>
              $linkLine
            </div>
        """.trimIndent()
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun appendToSendLog(
        s: BoothSettings,
        channel: String,
        recipientMasked: String,
        status: String,
        note: String
    ) {
        val entry = SendLogEntry(
            timestampMs = System.currentTimeMillis(),
            eventSlug = s.currentEventSlug.ifEmpty { "unassigned" },
            channel = channel,
            recipientMasked = recipientMasked,
            status = status,
            note = note
        )
        viewModelScope.launch {
            settingsManager.update {
                copy(sendLogJson = SendLog.append(sendLogJson, entry))
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
