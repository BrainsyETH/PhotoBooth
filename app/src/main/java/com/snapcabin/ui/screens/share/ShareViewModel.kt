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
import com.snapcabin.share.EmailSmsSharer
import com.snapcabin.share.PhotoPrinter
import com.snapcabin.share.PhotoSaver
import com.snapcabin.share.QrCodeGenerator
import com.snapcabin.share.TwilioSmsSender
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
    private val emailSmsSharer: EmailSmsSharer,
    private val twilioSmsSender: TwilioSmsSender,
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
        // New photo — invalidate the previous upload + per-session counter.
        cachedPublicUrl = null
        twilioSendsThisSession = 0
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

            // Eager Cloudinary upload so QR + SMS reuse the same URL. Without
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

    fun shareViaEmail(context: Context) {
        val photo = _uiState.value.photo ?: return
        emailSmsSharer.shareViaEmail(context, photo)
    }

    fun shareViaSms(context: Context) {
        val photo = _uiState.value.photo ?: return
        emailSmsSharer.shareViaSms(context, photo)
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

    // ────── Twilio SMS ──────
    private var twilioSendsThisSession: Int = 0
    /** Cached Cloudinary URL for the current photo so we don't re-upload per SMS. */
    private var cachedPublicUrl: String? = null
    /** Per-event count of SMS sent to each phone number. Resets when the current event changes. */
    private val perPhoneSendCounts: MutableMap<String, Int> = mutableMapOf()
    private var perPhoneCountsForEvent: String = ""

    fun sendViaTwilio(rawPhoneNumber: String) {
        val s = settings.value
        if (!s.twilioEnabled) {
            _uiState.value = _uiState.value.copy(message = "Twilio SMS isn't enabled.")
            return
        }
        if (twilioSendsThisSession >= s.twilioMaxPerSession.coerceAtLeast(1)) {
            _uiState.value = _uiState.value.copy(message = "SMS limit reached for this session.")
            return
        }
        val to = normalizeToE164(rawPhoneNumber)
        if (to == null) {
            _uiState.value = _uiState.value.copy(message = "Enter a valid phone number (e.g. +15551234567).")
            return
        }
        if (perPhoneCountsForEvent != s.currentEventSlug) {
            perPhoneSendCounts.clear()
            perPhoneCountsForEvent = s.currentEventSlug
        }
        val alreadySentToThisNumber = perPhoneSendCounts[to] ?: 0
        if (alreadySentToThisNumber >= s.twilioMaxPerNumber.coerceAtLeast(1)) {
            _uiState.value = _uiState.value.copy(
                message = "Already sent ${alreadySentToThisNumber} times to that number this event."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Sending to $to…")

            val publicUrl = resolvePublicPhotoUrl(s)

            val body = buildString {
                append("Your photo is ready")
                if (s.eventName.isNotBlank()) append(" from ").append(s.eventName)
                append("!")
                if (publicUrl != null) append(" ").append(publicUrl)
            }
            // MMS only when we actually have a public URL.
            val mediaUrl = if (publicUrl != null) publicUrl else null

            val result = twilioSmsSender.send(
                accountSid = s.twilioAccountSid,
                authToken = s.twilioAuthToken,
                fromE164 = s.twilioFromNumber,
                toE164 = to,
                body = body,
                mediaUrl = mediaUrl
            )
            when (result) {
                is TwilioSmsSender.Result.Ok -> {
                    twilioSendsThisSession++
                    perPhoneSendCounts[to] = (perPhoneSendCounts[to] ?: 0) + 1
                    appendToSendLog(s, "sms", SendLog.maskPhone(to), "ok", note = "")
                    _uiState.value = _uiState.value.copy(message = "Sent to $to ✓")
                }
                is TwilioSmsSender.Result.Err -> {
                    appendToSendLog(s, "sms", SendLog.maskPhone(to), "err", note = result.message)
                    _uiState.value = _uiState.value.copy(message = result.message)
                }
            }
        }
    }

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

    /**
     * Returns a publicly-reachable URL for the current photo, uploading to
     * Cloudinary on demand if the eager upload didn't already cache one.
     * Falls back to an admin-supplied public host if set, otherwise null
     * (in which case Twilio sends a text-only SMS).
     */
    private suspend fun resolvePublicPhotoUrl(s: BoothSettings): String? {
        cachedPublicUrl?.let { return it }

        if (s.cloudinaryEnabled && s.cloudinaryCloudName.isNotBlank() &&
            s.cloudinaryUploadPreset.isNotBlank()
        ) {
            val photo = _uiState.value.photo
            if (photo != null) {
                val result = cloudinaryUploader.upload(
                    cloudName = s.cloudinaryCloudName,
                    uploadPreset = s.cloudinaryUploadPreset,
                    bitmap = photo,
                    folder = s.currentEventSlug.takeIf { it.isNotBlank() }?.let { "events/$it" }
                )
                when (result) {
                    is CloudinaryUploader.Result.Ok -> {
                        cachedPublicUrl = result.secureUrl
                        return result.secureUrl
                    }
                    is CloudinaryUploader.Result.Err -> {
                        _uiState.value = _uiState.value.copy(message = result.message)
                    }
                }
            }
        }

        val base = s.twilioPhotoUrlBase.trim().trimEnd('/')
        if (base.isNotEmpty()) return "$base/photo.jpg"

        return null
    }

    private fun normalizeToE164(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("+")) {
            val digits = trimmed.drop(1).filter { it.isDigit() }
            val candidate = "+$digits"
            return if (twilioSmsSender.isValidE164(candidate)) candidate else null
        }
        val digits = trimmed.filter { it.isDigit() }
        val candidate = when (digits.length) {
            10 -> "+1$digits"
            11 -> if (digits.startsWith("1")) "+$digits" else null
            else -> null
        }
        return if (candidate != null && twilioSmsSender.isValidE164(candidate)) candidate else null
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
