package com.photobooth.ui.screens.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.camera.CameraManager
import com.photobooth.settings.BoothSettings
import com.photobooth.settings.SettingsManager
import com.photobooth.ui.components.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureUiState(
    val isCountingDown: Boolean = false,
    val countdownValue: Int = 3,
    val capturedPhoto: Bitmap? = null,
    val isCapturing: Boolean = false,
    val showFlash: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    val cameraManager: CameraManager,
    private val settingsManager: SettingsManager,
    val soundManager: SoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    init {
        // Sync sound settings
        viewModelScope.launch {
            settingsManager.settings.collect { s ->
                soundManager.soundEnabled = s.soundEnabled
                soundManager.shutterEnabled = s.shutterSoundEnabled
                soundManager.countdownBeepEnabled = s.countdownBeepEnabled
            }
        }
    }

    fun startCountdown() {
        if (_uiState.value.isCountingDown || _uiState.value.isCapturing) return

        val countdownFrom = settings.value.countdownSeconds

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCountingDown = true,
                error = null,
                countdownValue = countdownFrom
            )

            for (i in countdownFrom downTo 1) {
                _uiState.value = _uiState.value.copy(countdownValue = i)
                if (i == 1) {
                    soundManager.playCountdownFinalBeep()
                } else {
                    soundManager.playCountdownBeep()
                }
                delay(1000)
            }

            _uiState.value = _uiState.value.copy(isCountingDown = false, isCapturing = true)

            // Flash effect
            if (settings.value.showFlashEffect) {
                _uiState.value = _uiState.value.copy(showFlash = true)
            }

            try {
                soundManager.playShutter()
                val photo = cameraManager.takePhoto()
                _uiState.value = _uiState.value.copy(
                    capturedPhoto = photo,
                    isCapturing = false,
                    showFlash = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    showFlash = false,
                    error = "Failed to capture photo: ${e.message}"
                )
            }
        }
    }

    fun resetCapture() {
        _uiState.value = CaptureUiState()
    }

    fun getCapturedPhoto(): Bitmap? = _uiState.value.capturedPhoto
}
