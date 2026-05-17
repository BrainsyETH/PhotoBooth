package com.snapcabin.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    val customBorderPath: String = "", // File path to custom border/frame image
    val customOverlayPath: String = "", // File path to custom overlay image
    val eventName: String = "",        // Shown as the Attract headline
    val attractSubtext: String = "A photo booth in the woods", // Tagline under the event name

    // Sharing
    val enableQrSharing: Boolean = true,
    val enableLocalServer: Boolean = true,
    val enableSaveToGallery: Boolean = true,
    val enableShareIntent: Boolean = true,
    val enablePrint: Boolean = true,
    val enableEmail: Boolean = true,
    val enableSms: Boolean = true,
    val serverPort: Int = 8080,

    // Twilio SMS (optional — disabled by default)
    val twilioEnabled: Boolean = false,
    val twilioAccountSid: String = "",
    val twilioAuthToken: String = "",
    val twilioFromNumber: String = "",
    val twilioPhotoUrlBase: String = "",     // optional public URL prefix; falls back to LAN URL
    val twilioMaxPerSession: Int = 10,       // rate limit to prevent abuse

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
        val TWILIO_ENABLED = booleanPreferencesKey("twilio_enabled")
        val TWILIO_ACCOUNT_SID = stringPreferencesKey("twilio_account_sid")
        val TWILIO_AUTH_TOKEN = stringPreferencesKey("twilio_auth_token")
        val TWILIO_FROM_NUMBER = stringPreferencesKey("twilio_from_number")
        val TWILIO_PHOTO_URL_BASE = stringPreferencesKey("twilio_photo_url_base")
        val TWILIO_MAX_PER_SESSION = intPreferencesKey("twilio_max_per_session")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_gallery")
        val OUTPUT_QUALITY = intPreferencesKey("output_quality")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        val CUSTOM_BORDER_PATH = stringPreferencesKey("custom_border_path")
        val CUSTOM_OVERLAY_PATH = stringPreferencesKey("custom_overlay_path")
        val EVENT_NAME = stringPreferencesKey("event_name")
        val ATTRACT_SUBTEXT = stringPreferencesKey("attract_subtext")
        val ENABLE_QR = booleanPreferencesKey("enable_qr_sharing")
        val ENABLE_SERVER = booleanPreferencesKey("enable_local_server")
        val ENABLE_SAVE_GALLERY = booleanPreferencesKey("enable_save_gallery")
        val ENABLE_SHARE_INTENT = booleanPreferencesKey("enable_share_intent")
        val ENABLE_PRINT = booleanPreferencesKey("enable_print")
        val ENABLE_EMAIL = booleanPreferencesKey("enable_email")
        val ENABLE_SMS = booleanPreferencesKey("enable_sms")
        val SERVER_PORT = intPreferencesKey("server_port")
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

    val settings: Flow<BoothSettings> = context.dataStore.data.map { prefs ->
        BoothSettings(
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
            eventName = prefs[Keys.EVENT_NAME] ?: "",
            attractSubtext = prefs[Keys.ATTRACT_SUBTEXT] ?: "A photo booth in the woods",
            enableQrSharing = prefs[Keys.ENABLE_QR] ?: true,
            enableLocalServer = prefs[Keys.ENABLE_SERVER] ?: true,
            enableSaveToGallery = prefs[Keys.ENABLE_SAVE_GALLERY] ?: true,
            enableShareIntent = prefs[Keys.ENABLE_SHARE_INTENT] ?: true,
            enablePrint = prefs[Keys.ENABLE_PRINT] ?: true,
            enableEmail = prefs[Keys.ENABLE_EMAIL] ?: true,
            enableSms = prefs[Keys.ENABLE_SMS] ?: true,
            serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
            twilioEnabled = prefs[Keys.TWILIO_ENABLED] ?: false,
            twilioAccountSid = prefs[Keys.TWILIO_ACCOUNT_SID] ?: "",
            twilioAuthToken = prefs[Keys.TWILIO_AUTH_TOKEN] ?: "",
            twilioFromNumber = prefs[Keys.TWILIO_FROM_NUMBER] ?: "",
            twilioPhotoUrlBase = prefs[Keys.TWILIO_PHOTO_URL_BASE] ?: "",
            twilioMaxPerSession = prefs[Keys.TWILIO_MAX_PER_SESSION] ?: 10,
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
    }

    suspend fun update(transform: BoothSettings.() -> BoothSettings) {
        // Read current, transform, then write
        context.dataStore.edit { prefs ->
            val current = BoothSettings(
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
                eventName = prefs[Keys.EVENT_NAME] ?: "",
                attractSubtext = prefs[Keys.ATTRACT_SUBTEXT] ?: "A photo booth in the woods",
                enableQrSharing = prefs[Keys.ENABLE_QR] ?: true,
                enableLocalServer = prefs[Keys.ENABLE_SERVER] ?: true,
                enableSaveToGallery = prefs[Keys.ENABLE_SAVE_GALLERY] ?: true,
                enableShareIntent = prefs[Keys.ENABLE_SHARE_INTENT] ?: true,
                enablePrint = prefs[Keys.ENABLE_PRINT] ?: true,
                enableEmail = prefs[Keys.ENABLE_EMAIL] ?: true,
                enableSms = prefs[Keys.ENABLE_SMS] ?: true,
                serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
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

            val updated = current.transform()

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
            prefs[Keys.TWILIO_ENABLED] = updated.twilioEnabled
            prefs[Keys.TWILIO_ACCOUNT_SID] = updated.twilioAccountSid
            prefs[Keys.TWILIO_AUTH_TOKEN] = updated.twilioAuthToken
            prefs[Keys.TWILIO_FROM_NUMBER] = updated.twilioFromNumber
            prefs[Keys.TWILIO_PHOTO_URL_BASE] = updated.twilioPhotoUrlBase
            prefs[Keys.TWILIO_MAX_PER_SESSION] = updated.twilioMaxPerSession
            prefs[Keys.AUTO_SAVE] = updated.autoSaveToGallery
            prefs[Keys.OUTPUT_QUALITY] = updated.outputQuality
            prefs[Keys.WATERMARK_ENABLED] = updated.watermarkEnabled
            prefs[Keys.WATERMARK_TEXT] = updated.watermarkText
            prefs[Keys.CUSTOM_BORDER_PATH] = updated.customBorderPath
            prefs[Keys.CUSTOM_OVERLAY_PATH] = updated.customOverlayPath
            prefs[Keys.EVENT_NAME] = updated.eventName
            prefs[Keys.ATTRACT_SUBTEXT] = updated.attractSubtext
            prefs[Keys.ENABLE_QR] = updated.enableQrSharing
            prefs[Keys.ENABLE_SERVER] = updated.enableLocalServer
            prefs[Keys.ENABLE_SAVE_GALLERY] = updated.enableSaveToGallery
            prefs[Keys.ENABLE_SHARE_INTENT] = updated.enableShareIntent
            prefs[Keys.ENABLE_PRINT] = updated.enablePrint
            prefs[Keys.ENABLE_EMAIL] = updated.enableEmail
            prefs[Keys.ENABLE_SMS] = updated.enableSms
            prefs[Keys.SERVER_PORT] = updated.serverPort
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
