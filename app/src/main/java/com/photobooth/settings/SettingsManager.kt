package com.photobooth.settings

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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photobooth_settings")

data class BoothSettings(
    // Camera
    val useFrontCamera: Boolean = true,
    val cameraId: String = "", // Empty = auto-detect, or specific camera ID for external cameras
    val mirrorFrontCamera: Boolean = true,
    val photoResolution: PhotoResolution = PhotoResolution.FULL,

    // Capture
    val countdownSeconds: Int = 3,
    val autoCapture: Boolean = false, // Auto-capture after countdown (no button press)

    // Output
    val autoSaveToGallery: Boolean = false,
    val outputQuality: Int = 95, // JPEG quality 1-100
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "",

    // Sharing
    val enableQrSharing: Boolean = true,
    val enableLocalServer: Boolean = true,
    val serverPort: Int = 8080,

    // Kiosk
    val kioskModeEnabled: Boolean = false,
    val adminPin: String = "1234",
    val inactivityTimeoutSeconds: Int = 60,

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
        val MIRROR_FRONT = booleanPreferencesKey("mirror_front_camera")
        val PHOTO_RESOLUTION = stringPreferencesKey("photo_resolution")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val AUTO_CAPTURE = booleanPreferencesKey("auto_capture")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_gallery")
        val OUTPUT_QUALITY = intPreferencesKey("output_quality")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        val ENABLE_QR = booleanPreferencesKey("enable_qr_sharing")
        val ENABLE_SERVER = booleanPreferencesKey("enable_local_server")
        val SERVER_PORT = intPreferencesKey("server_port")
        val KIOSK_MODE = booleanPreferencesKey("kiosk_mode")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val INACTIVITY_TIMEOUT = intPreferencesKey("inactivity_timeout")
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
            mirrorFrontCamera = prefs[Keys.MIRROR_FRONT] ?: true,
            photoResolution = prefs[Keys.PHOTO_RESOLUTION]?.let {
                try { PhotoResolution.valueOf(it) } catch (e: Exception) { PhotoResolution.FULL }
            } ?: PhotoResolution.FULL,
            countdownSeconds = prefs[Keys.COUNTDOWN_SECONDS] ?: 3,
            autoCapture = prefs[Keys.AUTO_CAPTURE] ?: false,
            autoSaveToGallery = prefs[Keys.AUTO_SAVE] ?: false,
            outputQuality = prefs[Keys.OUTPUT_QUALITY] ?: 95,
            watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: false,
            watermarkText = prefs[Keys.WATERMARK_TEXT] ?: "",
            enableQrSharing = prefs[Keys.ENABLE_QR] ?: true,
            enableLocalServer = prefs[Keys.ENABLE_SERVER] ?: true,
            serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
            kioskModeEnabled = prefs[Keys.KIOSK_MODE] ?: false,
            adminPin = prefs[Keys.ADMIN_PIN] ?: "1234",
            inactivityTimeoutSeconds = prefs[Keys.INACTIVITY_TIMEOUT] ?: 60,
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
                mirrorFrontCamera = prefs[Keys.MIRROR_FRONT] ?: true,
                countdownSeconds = prefs[Keys.COUNTDOWN_SECONDS] ?: 3,
                autoCapture = prefs[Keys.AUTO_CAPTURE] ?: false,
                autoSaveToGallery = prefs[Keys.AUTO_SAVE] ?: false,
                outputQuality = prefs[Keys.OUTPUT_QUALITY] ?: 95,
                watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: false,
                watermarkText = prefs[Keys.WATERMARK_TEXT] ?: "",
                enableQrSharing = prefs[Keys.ENABLE_QR] ?: true,
                enableLocalServer = prefs[Keys.ENABLE_SERVER] ?: true,
                serverPort = prefs[Keys.SERVER_PORT] ?: 8080,
                kioskModeEnabled = prefs[Keys.KIOSK_MODE] ?: false,
                adminPin = prefs[Keys.ADMIN_PIN] ?: "1234",
                inactivityTimeoutSeconds = prefs[Keys.INACTIVITY_TIMEOUT] ?: 60,
                screenBrightness = prefs[Keys.SCREEN_BRIGHTNESS] ?: 1.0f,
                showFlashEffect = prefs[Keys.SHOW_FLASH] ?: true,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                shutterSoundEnabled = prefs[Keys.SHUTTER_SOUND] ?: true,
                countdownBeepEnabled = prefs[Keys.COUNTDOWN_BEEP] ?: true
            )

            val updated = current.transform()

            prefs[Keys.USE_FRONT_CAMERA] = updated.useFrontCamera
            prefs[Keys.CAMERA_ID] = updated.cameraId
            prefs[Keys.MIRROR_FRONT] = updated.mirrorFrontCamera
            prefs[Keys.PHOTO_RESOLUTION] = updated.photoResolution.name
            prefs[Keys.COUNTDOWN_SECONDS] = updated.countdownSeconds
            prefs[Keys.AUTO_CAPTURE] = updated.autoCapture
            prefs[Keys.AUTO_SAVE] = updated.autoSaveToGallery
            prefs[Keys.OUTPUT_QUALITY] = updated.outputQuality
            prefs[Keys.WATERMARK_ENABLED] = updated.watermarkEnabled
            prefs[Keys.WATERMARK_TEXT] = updated.watermarkText
            prefs[Keys.ENABLE_QR] = updated.enableQrSharing
            prefs[Keys.ENABLE_SERVER] = updated.enableLocalServer
            prefs[Keys.SERVER_PORT] = updated.serverPort
            prefs[Keys.KIOSK_MODE] = updated.kioskModeEnabled
            prefs[Keys.ADMIN_PIN] = updated.adminPin
            prefs[Keys.INACTIVITY_TIMEOUT] = updated.inactivityTimeoutSeconds
            prefs[Keys.SCREEN_BRIGHTNESS] = updated.screenBrightness
            prefs[Keys.SHOW_FLASH] = updated.showFlashEffect
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.SHUTTER_SOUND] = updated.shutterSoundEnabled
            prefs[Keys.COUNTDOWN_BEEP] = updated.countdownBeepEnabled
        }
    }
}
