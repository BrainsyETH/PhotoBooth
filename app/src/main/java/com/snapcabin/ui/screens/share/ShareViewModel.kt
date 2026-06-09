package com.snapcabin.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.collage.CollageLayout
import com.snapcabin.collage.CollageRenderer
import com.snapcabin.event.SendLog
import com.snapcabin.event.SendLogEntry
import com.snapcabin.filter.CustomBrandingRenderer
import com.snapcabin.filter.WatermarkRenderer
import com.snapcabin.gif.GifEncoder
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShareUiState(
    /** The still shown in the preview pane. For a GIF this is a representative frame. */
    val photo: Bitmap? = null,
    /** Encoded animated GIF bytes when the session output is a GIF; null for stills. */
    val gifBytes: ByteArray? = null,
    val qrCodeBitmap: Bitmap? = null,
    val savedPath: String? = null,
    val isSaving: Boolean = false,
    /** True while the Cloudinary upload is in flight. */
    val isUploading: Boolean = false,
    /** Public Cloudinary URL once the upload succeeds. Empty when Cloudinary is off. */
    val shareUrl: String? = null,
    /** True when a configured Cloudinary upload failed (e.g. offline). Drives the retry affordance. */
    val uploadFailed: Boolean = false,
    /** Thank-you overlay is rendered when this is true. */
    val endingSession: Boolean = false,
    val message: String? = null
)

/**
 * What the booth is about to hand the guest. The Share pipeline brands, encodes,
 * and uploads each kind off the main thread — so a slow collage render or a GIF
 * encode never janks the UI thread (the old code rendered collages inline on the
 * accept tap).
 */
sealed interface ShareInput {
    /** A single already-picked still (Single mode); branding still applies. */
    data class Still(val bitmap: Bitmap) : ShareInput
    /** Raw frames to assemble into a 2x2 collage, then brand. */
    data class Collage(val frames: List<Bitmap>) : ShareInput
    /** Raw frames to brand per-frame and encode into an animated GIF. */
    data class AnimatedGif(val frames: List<Bitmap>) : ShareInput
}

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

    /**
     * Hand the session output to the Share pipeline. Branding, collage assembly,
     * and GIF encoding all happen on [Dispatchers.Default]; the Cloudinary
     * upload (and its retry) is set up once the artifact is ready.
     */
    fun setInput(input: ShareInput, context: Context) {
        // New output — invalidate previous upload + per-session counter.
        cachedPublicUrl = null
        emailSendsThisSession = 0
        sessionEndScheduled = false
        pendingUpload = null
        viewModelScope.launch {
            // Snapshot the REAL stored settings before touching the photo.
            // This ViewModel is created fresh on Share entry and its stateIn
            // starts at BoothSettings() defaults; this races the first
            // DataStore emission. Reading settings.value here used to brand
            // with empty paths (no logo, no watermark) and then upload that
            // unbranded photo once the late-loading settings enabled
            // Cloudinary. first() suspends the few ms until the stored
            // values are actually available, and one snapshot keeps the
            // whole pipeline consistent.
            val s = settingsManager.settings.first()

            val prepared = withContext(Dispatchers.Default) { prepareOutput(input, s) }

            _uiState.value = _uiState.value.copy(
                photo = prepared.preview,
                gifBytes = prepared.gifBytes
            )

            // Auto-save if enabled.
            if (s.autoSaveToGallery) {
                saveToGallery(context)
            }

            // Eager Cloudinary upload so QR sharing has a URL ready. Without
            // Cloudinary the QR section stays hidden — guests can still use
            // Save / Share / Print / Email.
            if (s.cloudinaryEnabled && s.cloudinaryCloudName.isNotBlank() &&
                s.cloudinaryUploadPreset.isNotBlank()
            ) {
                val cloudName = s.cloudinaryCloudName
                val preset = s.cloudinaryUploadPreset
                val folder = s.currentEventSlug.takeIf { it.isNotBlank() }?.let { "events/$it" }
                val gif = prepared.gifBytes
                val preview = prepared.preview
                pendingUpload = if (gif != null) {
                    { cloudinaryUploader.uploadBytes(cloudName, preset, gif, "photo.gif", "image/gif", folder) }
                } else if (preview != null) {
                    { cloudinaryUploader.upload(cloudName, preset, preview, folder) }
                } else {
                    null
                }
                runUpload()
            }
        }
    }

    private data class PreparedOutput(val preview: Bitmap?, val gifBytes: ByteArray?)

    /** Longest edge of a GIF frame — keeps the encode fast and the file emailable. */
    private val gifMaxDimension = 640

    private fun prepareOutput(input: ShareInput, s: BoothSettings): PreparedOutput = when (input) {
        is ShareInput.Still -> PreparedOutput(brand(input.bitmap, s), null)
        is ShareInput.Collage -> {
            val collage = CollageRenderer.render(input.frames, CollageLayout.GRID_2X2)
            PreparedOutput(brand(collage, s), null)
        }
        is ShareInput.AnimatedGif -> {
            val brandedFrames = input.frames.map { scaleToMax(brand(it, s), gifMaxDimension) }
            val preview = brandedFrames.firstOrNull()
            val bytes = try {
                if (brandedFrames.isNotEmpty()) {
                    GifEncoder().encodeToBytes(
                        frames = brandedFrames,
                        delayMs = s.gifFrameDelayMs.coerceIn(60, 2000)
                    )
                } else null
            } catch (t: Throwable) {
                // A GIF encode failure must not strand the guest — fall back to
                // delivering the first frame as a still.
                Log.e(TAG, "GIF encode failed; falling back to a still frame", t)
                null
            }
            PreparedOutput(preview, bytes)
        }
    }

    /**
     * Bakes in admin-configured branding (border + overlay PNGs, then watermark text).
     * Failure-isolated: any decode/out-of-memory issue falls back to the raw capture
     * so a bad branding asset can never crash the booth mid-session.
     */
    private fun brand(source: Bitmap, s: BoothSettings): Bitmap = try {
        val branded = CustomBrandingRenderer.apply(
            source = source,
            borderPath = s.customBorderPath,
            overlayPath = s.customOverlayPath,
            overlayPlacement = s.overlayPlacement,
            overlayCorner = s.overlayCorner,
            overlaySizePct = s.overlaySizePct
        )
        if (s.watermarkEnabled && s.watermarkText.isNotBlank()) {
            WatermarkRenderer.apply(branded, s.watermarkText)
        } else {
            branded
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Photo processing failed; using the raw capture", t)
        source
    }

    private fun scaleToMax(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /** The most recent upload task, re-runnable for the retry button. */
    private var pendingUpload: (suspend () -> CloudinaryUploader.Result)? = null

    private suspend fun runUpload() {
        val task = pendingUpload ?: return
        _uiState.value = _uiState.value.copy(isUploading = true, uploadFailed = false)
        when (val result = task()) {
            is CloudinaryUploader.Result.Ok -> {
                cachedPublicUrl = result.secureUrl
                val qr = withContext(Dispatchers.Default) {
                    qrCodeGenerator.generate(result.secureUrl)
                }
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadFailed = false,
                    shareUrl = result.secureUrl,
                    qrCodeBitmap = qr
                )
            }
            is CloudinaryUploader.Result.Err -> {
                // No transient snackbar here — the QR slot shows a persistent
                // "couldn't upload, tap to retry" state instead, which a guest
                // can actually read and act on at arm's length.
                Log.w(TAG, "Cloudinary upload failed: ${result.message}")
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadFailed = true
                )
            }
        }
    }

    /** Re-attempt the upload (offline-recovery affordance on the QR tile). */
    fun retryUpload() {
        viewModelScope.launch { runUpload() }
    }

    fun saveToGallery(context: Context) {
        val gif = _uiState.value.gifBytes
        val photo = _uiState.value.photo
        if (gif == null && photo == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val quality = settings.value.outputQuality
            val path = withContext(Dispatchers.IO) {
                if (gif != null) {
                    photoSaver.saveBytesToGallery(context, gif, "image/gif", "gif")
                } else {
                    photoSaver.saveToGallery(context, photo!!, quality = quality)
                }
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedPath = path,
                message = if (path != null) "Saved to gallery!" else "Couldn't save — check storage space."
            )
        }
    }

    fun shareViaIntent(context: Context) {
        val gif = _uiState.value.gifBytes
        val photo = _uiState.value.photo
        if (gif == null && photo == null) return
        viewModelScope.launch {
            val isGif = gif != null
            val file = withContext(Dispatchers.IO) {
                if (gif != null) {
                    photoSaver.saveBytesToCacheForSharing(context, gif, "gif")
                } else {
                    photoSaver.saveToCacheForSharing(context, photo!!)
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isGif) "image/gif" else "image/jpeg"
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
            val subject = renderSubject(s)

            val gif = _uiState.value.gifBytes
            val result = if (gif != null) {
                resendEmailSender.send(
                    apiKey = s.resendApiKey,
                    fromAddress = s.resendFromAddress,
                    replyToAddress = s.resendReplyToAddress,
                    toAddress = to,
                    subject = subject,
                    htmlBody = htmlBody,
                    attachmentBytes = gif,
                    attachmentFilename = "photo.gif",
                    attachmentContentType = "image/gif"
                )
            } else {
                resendEmailSender.send(
                    apiKey = s.resendApiKey,
                    fromAddress = s.resendFromAddress,
                    replyToAddress = s.resendReplyToAddress,
                    toAddress = to,
                    subject = subject,
                    htmlBody = htmlBody,
                    photo = photo
                )
            }
            when (result) {
                is ResendEmailSender.Result.Ok -> {
                    emailSendsThisSession++
                    perAddressSendCounts[to] = (perAddressSendCounts[to] ?: 0) + 1
                    appendToSendLog(s, "email", SendLog.maskEmail(to), "ok", note = "")
                    _uiState.value = _uiState.value.copy(message = "Sent to $to ✓")
                }
                is ResendEmailSender.Result.Err -> {
                    appendToSendLog(s, "email", SendLog.maskEmail(to), "err", note = result.message)
                    val msg = if (result.isQuotaError && cachedPublicUrl != null) {
                        "${result.message} Guests can still scan the QR."
                    } else {
                        result.message
                    }
                    _uiState.value = _uiState.value.copy(message = msg)
                }
            }
        }
    }

    private fun renderSubject(s: BoothSettings): String {
        val template = s.resendSubject.ifBlank { "Your photo from {event}" }
        val event = s.eventName.ifBlank { "the booth" }
        return template.replace("{event}", event)
    }

    private fun buildHtmlBody(s: BoothSettings, publicUrl: String?): String {
        // Operator-authored body. {event} expands to the event name; the text
        // is HTML-escaped (it's plain text from a kiosk field, not trusted
        // markup) and blank lines become separate paragraphs.
        val event = s.eventName.ifBlank { "the booth" }
        val rawBody = s.resendBodyText
            .ifBlank { "Your photo is attached — save it, share it, treasure it." }
            .replace("{event}", event)
        val paragraphs = rawBody
            .split(Regex("\\n[ \\t]*\\n"))         // blank line → paragraph break
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n") { para ->
                val withBreaks = escapeHtml(para).replace("\n", "<br>")
                "  <p>$withBreaks</p>"
            }
        val linkLine = publicUrl?.let {
            "  <p style=\"color:#7a6a4f;font-size:14px;\">If the attachment doesn't come through, here's a link: <a href=\"${escapeHtml(it)}\">${escapeHtml(it)}</a></p>"
        }.orEmpty()
        return """
            <div style="font-family:Helvetica,Arial,sans-serif;color:#3a2e20;">
            $paragraphs
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
