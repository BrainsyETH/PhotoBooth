package com.photobooth.ui.screens.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.camera.CameraManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureUiState(
    val isCountingDown: Boolean = false,
    val countdownValue: Int = 3,
    val capturedPhoto: Bitmap? = null,
    val isCapturing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    val cameraManager: CameraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun startCountdown() {
        if (_uiState.value.isCountingDown || _uiState.value.isCapturing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCountingDown = true, error = null)

            for (i in 3 downTo 1) {
                _uiState.value = _uiState.value.copy(countdownValue = i)
                delay(1000)
            }

            _uiState.value = _uiState.value.copy(isCountingDown = false, isCapturing = true)

            try {
                val photo = cameraManager.takePhoto()
                _uiState.value = _uiState.value.copy(
                    capturedPhoto = photo,
                    isCapturing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
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
