package com.snapcabin.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snapcabin_settings")

data class BoothSettings(
    // Camera
    val useFrontCamera: Boolean = true,
    val cameraId: String = "", // Empty = auto-detect, or specific camera ID for external cameras
    val mirrorImage: Boolean = true,
    val photoResolution: PhotoResolution = PhotoResolution.FULL,

    // Capture
    val countdownSeconds: Int = 3,
    val autoCapture: Boolean = false, // Auto-capture after countdown (no button press)
    val collageShotCount: Int = 4,
    val gifFrameCount: Int = 6,
    val gifFrameDelayMs: Int = 250,
    val coachingEnabled: Boolean = true,
    val framingGuideEnabled: Boolean = true,
    val flashColor: String = "#FDFAF1",
    val cameraLensPosition: String = "top", // top | bottom | left | right | none
    val posePromptsCollage: String = "",    // ||-delimited; empty = use defaults
    val posePromptsGif: String = "",        // ||-delimited; empty = use defaults

    // Output
    val autoSaveToGallery: Boolean = false,
    val outputQuality: Int = 95, // JPEG quality 1-100
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "",

    // Branding
    val customBorderPath: String = "", // File path to custom border/frame image (always full-frame)
    val customOverlayPath: String = "", // File path to custom overlay/logo image
    val overlayPlacement: String = "stretch", // "stretch" (full-frame) | "corner"
    val overlayCorner: String = "br",  // tl | tr | bl | br — used when placement = corner
    val overlaySizePct: Int = 20,      // logo width as % of photo width, used when placement = corner
    val eventName: String = "",        // Shown as the Attract headline
    val attractSubtext: String = "A photo booth in the woods", // Tagline under the event name
    val showCustomLogoOnAttract: Boolean = false, // Swap the SnapCabin logo on the welcome screen for the custom logo

    // Sharing. The guest Email button is gated solely by resendEnabled —
    // one switch, in EMAIL DELIVERY, not two that must agree.
    val enableQrSharing: Boolean = true,
    val enableSaveToGallery: Boolean = true,
    val enableShareIntent: Boolean = false,
    val enablePrint: Boolean = false, // off by default: PRINT with no printer dumps guests into the Android print dialog

    // Resend email (optional — disabled by default). Resend's HTTP API delivers
    // the photo as a JPEG attachment over WiFi. No phone numbers, no carrier.
    val resendEnabled: Boolean = false,
    val resendApiKey: String = "",
    val resendFromAddress: String = "",       // e.g. "SnapCabin <booth@yourdomain.com>"
    val resendReplyToAddress: String = "",    // optional; lands guest replies in the host's inbox
    val resendSubject: String = "Your photo from {event}", // {event} expands to the event name
    val resendBodyText: String = "Your photo is attached — save it, share it, treasure it.", // {event} expands; blank lines become paragraphs
    val resendMaxPerSession: Int = 3,         // rate limit to prevent abuse
    val resendMaxPerAddress: Int = 3,         // per-recipient cap (anti-spam, per event)
    val resendVerifiedAt: Long = 0L,          // set when a test email succeeds; cleared on credential edit

    // Cloudinary (public photo hosting for QR sharing — unsigned upload preset)
    val cloudinaryEnabled: Boolean = false,
    val cloudinaryCloudName: String = "",
    val cloudinaryUploadPreset: String = "",
    val cloudinaryVerifiedAt: Long = 0L,      // set when a test upload succeeds; cleared on credential edit

    // Event lifecycle — each event scopes Cloudinary folder + audit + rate limits
    val currentEventName: String = "",
    val currentEventSlug: String = "",
    val currentEventStartedAt: Long = 0L,
    val sendLogJson: String = "[]",          // capped at 500 entries; auditable from admin

    // Admin UX
    val getStartedCollapsed: Boolean = false, // operator minimized the GET STARTED checklist

    // Modes
    val enableSinglePhotoMode: Boolean = true,
    val enableCollageMode: Boolean = true,
    val enableGifMode: Boolean = true,

    // Kiosk
    val kioskModeEnabled: Boolean = false,
    val adminPin: String = "1234",
    val inactivityTimeoutSeconds: Int = 60,

    // Review
    val reviewAutoAcceptSeconds: Int = 10, // 0 = disabled

    // Display
    val screenBrightness: Float = 1.0f,
    val showFlashEffect: Boolean = true,

    // Sound
    val soundEnabled: Boolean = true,
    val shutterSoundEnabled: Boolean = true,
    val countdownBeepEnabled: Boolean = true
)

enum class PhotoResolution(val label: String, val maxDimension: Int) {
    LOW("Low (720p)", 1280),
    MEDIUM("Medium (1080p)", 1920),
    FULL("Full", Int.MAX_VALUE)
}

@Singleton
class SettingsManager @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
        val CAMERA_ID = stringPreferencesKey("camera_id")
        val MIRROR_IMAGE = booleanPreferencesKey("mirror_image")
        val PHOTO_RESOLUTION = stringPreferencesKey("photo_resolution")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val AUTO_CAPTURE = booleanPreferencesKey("auto_capture")
        val COLLAGE_SHOT_COUNT = intPreferencesKey("collage_shot_count")
        val GIF_FRAME_COUNT = intPreferencesKey("gif_frame_count")
        val GIF_FRAME_DELAY_MS = intPreferencesKey("gif_frame_delay_ms")
        val COACHING_ENABLED = booleanPreferencesKey("coaching_enabled")
        val FRAMING_GUIDE_ENABLED = booleanPreferencesKey("framing_guide_enabled")
        val FLASH_COLOR = stringPreferencesKey("flash_color")
        val CAMERA_LENS_POSITION = stringPreferencesKey("camera_lens_position")
        val POSE_PROMPTS_COLLAGE = stringPreferencesKey("pose_prompts_collage")
        val POSE_PROMPTS_GIF = stringPreferencesKey("pose_prompts_gif")
        val RESEND_ENABLED = booleanPreferencesKey("resend_enabled")
        val RESEND_API_KEY = stringPreferencesKey("resend_api_key")
        val RESEND_FROM_ADDRESS = stringPreferencesKey("resend_from_address")
        val RESEND_REPLY_TO_ADDRESS = stringPreferencesKey("resend_reply_to_address")
        val RESEND_SUBJECT = stringPreferencesKey("resend_subject")
        val RESEND_BODY_TEXT = stringPreferencesKey("resend_body_text")
        val RESEND_MAX_PER_SESSION = intPreferencesKey("resend_max_per_session")
        val RESEND_MAX_PER_ADDRESS = intPreferencesKey("resend_max_per_address")
        val RESEND_VERIFIED_AT = longPreferencesKey("resend_verified_at")
        val CLOUDINARY_ENABLED = booleanPreferencesKey("cloudinary_enabled")
        val CLOUDINARY_CLOUD_NAME = stringPreferencesKey("cloudinary_cloud_name")
        val CLOUDINARY_UPLOAD_PRESET = stringPreferencesKey("cloudinary_upload_preset")
        val CLOUDINARY_VERIFIED_AT = longPreferencesKey("cloudinary_verified_at")
        val CURRENT_EVENT_NAME = stringPreferencesKey("current_event_name")
        val CURRENT_EVENT_SLUG = stringPreferencesKey("current_event_slug")
        val CURRENT_EVENT_STARTED_AT = longPreferencesKey("current_event_started_at")
        val SEND_LOG_JSON = stringPreferencesKey("send_log_json")
        val GET_STARTED_COLLAPSED = booleanPreferencesKey("get_started_collapsed")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_gallery")
        val OUTPUT_QUALITY = intPreferencesKey("output_quality")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        val CUSTOM_BORDER_PATH = stringPreferencesKey("custom_border_path")
        val CUSTOM_OVERLAY_PATH = stringPreferencesKey("custom_overlay_path")
        val OVERLAY_PLACEMENT = stringPreferencesKey("overlay_placement")
        val OVERLAY_CORNER = stringPreferencesKey("overlay_corner")
        val OVERLAY_SIZE_PCT = intPreferencesKey("overlay_size_pct")
        val EVENT_NAME = stringPreferencesKey("event_name")
        val ATTRACT_SUBTEXT = stringPreferencesKey("attract_subtext")
        val SHOW_CUSTOM_LOGO_ON_ATTRACT = booleanPreferencesKey("show_custom_logo_on_attract")
        val ENABLE_QR = booleanPreferencesKey("enable_qr_sharing")
        val ENABLE_SAVE_GALLERY = booleanPreferencesKey("enable_save_gallery")
        val ENABLE_SHARE_INTENT = booleanPreferencesKey("enable_share_intent")
        val ENABLE_PRINT = booleanPreferencesKey("enable_print")
        val ENABLE_SINGLE_PHOTO = booleanPreferencesKey("enable_single_photo_mode")
        val ENABLE_COLLAGE = booleanPreferencesKey("enable_collage_mode")
        val ENABLE_GIF = booleanPreferencesKey("enable_gif_mode")
        val KIOSK_MODE = booleanPreferencesKey("kiosk_mode")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val INACTIVITY_TIMEOUT = intPreferencesKey("inactivity_timeout")
        val REVIEW_AUTO_ACCEPT = intPreferencesKey("review_auto_accept_seconds")
        val SCREEN_BRIGHTNESS = floatPreferencesKey("screen_brightness")
        val SHOW_FLASH = booleanPreferencesKey("show_flash_effect")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SHUTTER_SOUND = booleanPreferencesKey("shutter_sound")
        val COUNTDOWN_BEEP = booleanPreferencesKey("countdown_beep")
    }

    private fun readSettings(prefs: Preferences): BoothSettings = BoothSettings(
        useFrontCamera = prefs[Keys.USE_FRONT_CAMERA] ?: true,
        cameraId = prefs[Keys.CAMERA_ID] ?: "",
        mirrorImage = prefs[Keys.MIRROR_IMAGE] ?: true,
        photoResolution = prefs[Keys.PHOTO_RESOLUTION]?.let {
            try { PhotoResolution.valueOf(it) } catch (e: Exception) { PhotoResolution.FULL }
        } ?: PhotoResolution.FULL,
        countdownSeconds = prefs[Keys.COUNTDOWN_SECONDS] ?: 3,
        autoCapture = prefs[Keys.AUTO_CAPTURE] ?: false,
        collageShotCount = prefs[Keys.COLLAGE_SHOT_COUNT] ?: 4,
        gifFrameCount = prefs[Keys.GIF_FRAME_COUNT] ?: 6,
        gifFrameDelayMs = prefs[Keys.GIF_FRAME_DELAY_MS] ?: 250,
        coachingEnabled = prefs[Keys.COACHING_ENABLED] ?: true,
        framingGuideEnabled = prefs[Keys.FRAMING_GUIDE_ENABLED] ?: true,
        flashColor = prefs[Keys.FLASH_COLOR] ?: "#FDFAF1",
        cameraLensPosition = prefs[Keys.CAMERA_LENS_POSITION] ?: "top",
        posePromptsCollage = prefs[Keys.POSE_PROMPTS_COLLAGE] ?: "",
        posePromptsGif = prefs[Keys.POSE_PROMPTS_GIF] ?: "",
        autoSaveToGallery = prefs[Keys.AUTO_SAVE] ?: false,
        outputQuality = prefs[Keys.OUTPUT_QUALITY] ?: 95,
        watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: false,
        watermarkText = prefs[Keys.WATERMARK_TEXT] ?: "",
        customBorderPath = prefs[Keys.CUSTOM_BORDER_PATH] ?: "",
        customOverlayPath = prefs[Keys.CUSTOM_OVERLAY_PATH] ?: "",
        overlayPlacement = prefs[Keys.OVERLAY_PLACEMENT] ?: "stretch",
        overlayCorner = prefs[Keys.OVERLAY_CORNER] ?: "br",
        overlaySizePct = prefs[Keys.OVERLAY_SIZE_PCT] ?: 20,
        eventName = prefs[Keys.EVENT_NAME] ?: "",
        attractSubtext = prefs[Keys.ATTRACT_SUBTEXT] ?: "A photo booth in the woods",
        showCustomLogoOnAttract = prefs[Keys.SHOW_CUSTOM_LOGO_ON_ATTRACT] ?: false,
        enableQrSharing = prefs[Keys.ENABLE_QR] ?: true,
        enableSaveToGallery = prefs[Keys.ENABLE_SAVE_GALLERY] ?: true,
        enableShareIntent = prefs[Keys.ENABLE_SHARE_INTENT] ?: false,
        enablePrint = prefs[Keys.ENABLE_PRINT] ?: false,
        resendEnabled = prefs[Keys.RESEND_ENABLED] ?: false,
        resendApiKey = prefs[Keys.RESEND_API_KEY] ?: "",
        resendFromAddress = prefs[Keys.RESEND_FROM_ADDRESS] ?: "",
        resendReplyToAddress = prefs[Keys.RESEND_REPLY_TO_ADDRESS] ?: "",
        resendSubject = prefs[Keys.RESEND_SUBJECT] ?: "Your photo from {event}",
        resendBodyText = prefs[Keys.RESEND_BODY_TEXT]
            ?: "Your photo is attached — save it, share it, treasure it.",
        resendMaxPerSession = prefs[Keys.RESEND_MAX_PER_SESSION] ?: 3,
        resendMaxPerAddress = prefs[Keys.RESEND_MAX_PER_ADDRESS] ?: 3,
        resendVerifiedAt = prefs[Keys.RESEND_VERIFIED_AT] ?: 0L,
        cloudinaryEnabled = prefs[Keys.CLOUDINARY_ENABLED] ?: false,
        cloudinaryCloudName = prefs[Keys.CLOUDINARY_CLOUD_NAME] ?: "",
        cloudinaryUploadPreset = prefs[Keys.CLOUDINARY_UPLOAD_PRESET] ?: "",
        cloudinaryVerifiedAt = prefs[Keys.CLOUDINARY_VERIFIED_AT] ?: 0L,
        currentEventName = prefs[Keys.CURRENT_EVENT_NAME] ?: "",
        currentEventSlug = prefs[Keys.CURRENT_EVENT_SLUG] ?: "",
        currentEventStartedAt = prefs[Keys.CURRENT_EVENT_STARTED_AT] ?: 0L,
        sendLogJson = prefs[Keys.SEND_LOG_JSON] ?: "[]",
        getStartedCollapsed = prefs[Keys.GET_STARTED_COLLAPSED] ?: false,
        enableSinglePhotoMode = prefs[Keys.ENABLE_SINGLE_PHOTO] ?: true,
        enableCollageMode = prefs[Keys.ENABLE_COLLAGE] ?: true,
        enableGifMode = prefs[Keys.ENABLE_GIF] ?: true,
        kioskModeEnabled = prefs[Keys.KIOSK_MODE] ?: false,
        adminPin = prefs[Keys.ADMIN_PIN] ?: "1234",
        inactivityTimeoutSeconds = prefs[Keys.INACTIVITY_TIMEOUT] ?: 60,
        reviewAutoAcceptSeconds = prefs[Keys.REVIEW_AUTO_ACCEPT] ?: 10,
        screenBrightness = prefs[Keys.SCREEN_BRIGHTNESS] ?: 1.0f,
        showFlashEffect = prefs[Keys.SHOW_FLASH] ?: true,
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        shutterSoundEnabled = prefs[Keys.SHUTTER_SOUND] ?: true,
        countdownBeepEnabled = prefs[Keys.COUNTDOWN_BEEP] ?: true
    )

    val settings: Flow<BoothSettings> = context.dataStore.data.map { prefs ->
        readSettings(prefs)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Hot, app-scoped settings: null only for the first few milliseconds of
     * process life, then always the latest stored values.
     *
     * Exists because per-screen `stateIn(..., BoothSettings())` flows START AT
     * DEFAULTS, and anything that runs once on first composition (like an
     * AndroidView factory binding the camera) captures those defaults before
     * the real DataStore emission arrives. The camera-selection bug — the
     * operator picks an external camera and the booth opens the front camera
     * anyway — was exactly this race. Bind cameras (and any other one-shot
     * effect) from THIS flow, gated on non-null.
     */
    val loaded: StateFlow<BoothSettings?> = settings
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun update(transform: BoothSettings.() -> BoothSettings) {
        context.dataStore.edit { prefs ->
            val updated = readSettings(prefs).transform()

            prefs[Keys.USE_FRONT_CAMERA] = updated.useFrontCamera
            prefs[Keys.CAMERA_ID] = updated.cameraId
            prefs[Keys.MIRROR_IMAGE] = updated.mirrorImage
            prefs[Keys.PHOTO_RESOLUTION] = updated.photoResolution.name
            prefs[Keys.COUNTDOWN_SECONDS] = updated.countdownSeconds
            prefs[Keys.AUTO_CAPTURE] = updated.autoCapture
            prefs[Keys.COLLAGE_SHOT_COUNT] = updated.collageShotCount
            prefs[Keys.GIF_FRAME_COUNT] = updated.gifFrameCount
            prefs[Keys.GIF_FRAME_DELAY_MS] = updated.gifFrameDelayMs
            prefs[Keys.COACHING_ENABLED] = updated.coachingEnabled
            prefs[Keys.FRAMING_GUIDE_ENABLED] = updated.framingGuideEnabled
            prefs[Keys.FLASH_COLOR] = updated.flashColor
            prefs[Keys.CAMERA_LENS_POSITION] = updated.cameraLensPosition
            prefs[Keys.POSE_PROMPTS_COLLAGE] = updated.posePromptsCollage
            prefs[Keys.POSE_PROMPTS_GIF] = updated.posePromptsGif
            prefs[Keys.RESEND_ENABLED] = updated.resendEnabled
            prefs[Keys.RESEND_API_KEY] = updated.resendApiKey
            prefs[Keys.RESEND_FROM_ADDRESS] = updated.resendFromAddress
            prefs[Keys.RESEND_REPLY_TO_ADDRESS] = updated.resendReplyToAddress
            prefs[Keys.RESEND_SUBJECT] = updated.resendSubject
            prefs[Keys.RESEND_BODY_TEXT] = updated.resendBodyText
            prefs[Keys.RESEND_MAX_PER_SESSION] = updated.resendMaxPerSession
            prefs[Keys.RESEND_MAX_PER_ADDRESS] = updated.resendMaxPerAddress
            prefs[Keys.RESEND_VERIFIED_AT] = updated.resendVerifiedAt
            prefs[Keys.CLOUDINARY_ENABLED] = updated.cloudinaryEnabled
            prefs[Keys.CLOUDINARY_CLOUD_NAME] = updated.cloudinaryCloudName
            prefs[Keys.CLOUDINARY_UPLOAD_PRESET] = updated.cloudinaryUploadPreset
            prefs[Keys.CLOUDINARY_VERIFIED_AT] = updated.cloudinaryVerifiedAt
            prefs[Keys.CURRENT_EVENT_NAME] = updated.currentEventName
            prefs[Keys.CURRENT_EVENT_SLUG] = updated.currentEventSlug
            prefs[Keys.CURRENT_EVENT_STARTED_AT] = updated.currentEventStartedAt
            prefs[Keys.SEND_LOG_JSON] = updated.sendLogJson
            prefs[Keys.GET_STARTED_COLLAPSED] = updated.getStartedCollapsed
            prefs[Keys.AUTO_SAVE] = updated.autoSaveToGallery
            prefs[Keys.OUTPUT_QUALITY] = updated.outputQuality
            prefs[Keys.WATERMARK_ENABLED] = updated.watermarkEnabled
            prefs[Keys.WATERMARK_TEXT] = updated.watermarkText
            prefs[Keys.CUSTOM_BORDER_PATH] = updated.customBorderPath
            prefs[Keys.CUSTOM_OVERLAY_PATH] = updated.customOverlayPath
            prefs[Keys.OVERLAY_PLACEMENT] = updated.overlayPlacement
            prefs[Keys.OVERLAY_CORNER] = updated.overlayCorner
            prefs[Keys.OVERLAY_SIZE_PCT] = updated.overlaySizePct
            prefs[Keys.EVENT_NAME] = updated.eventName
            prefs[Keys.ATTRACT_SUBTEXT] = updated.attractSubtext
            prefs[Keys.SHOW_CUSTOM_LOGO_ON_ATTRACT] = updated.showCustomLogoOnAttract
            prefs[Keys.ENABLE_QR] = updated.enableQrSharing
            prefs[Keys.ENABLE_SAVE_GALLERY] = updated.enableSaveToGallery
            prefs[Keys.ENABLE_SHARE_INTENT] = updated.enableShareIntent
            prefs[Keys.ENABLE_PRINT] = updated.enablePrint
            prefs[Keys.ENABLE_SINGLE_PHOTO] = updated.enableSinglePhotoMode
            prefs[Keys.ENABLE_COLLAGE] = updated.enableCollageMode
            prefs[Keys.ENABLE_GIF] = updated.enableGifMode
            prefs[Keys.KIOSK_MODE] = updated.kioskModeEnabled
            prefs[Keys.ADMIN_PIN] = updated.adminPin
            prefs[Keys.INACTIVITY_TIMEOUT] = updated.inactivityTimeoutSeconds
            prefs[Keys.REVIEW_AUTO_ACCEPT] = updated.reviewAutoAcceptSeconds
            prefs[Keys.SCREEN_BRIGHTNESS] = updated.screenBrightness
            prefs[Keys.SHOW_FLASH] = updated.showFlashEffect
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.SHUTTER_SOUND] = updated.shutterSoundEnabled
            prefs[Keys.COUNTDOWN_BEEP] = updated.countdownBeepEnabled
        }
    }
}
