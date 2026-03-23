package com.snapcabin.ui.screens.admin

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.PhotoResolution
import com.snapcabin.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraInfo(
    val id: String,
    val facing: String,
    val isExternal: Boolean
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    private val _pinVerified = MutableStateFlow(false)
    val pinVerified: StateFlow<Boolean> = _pinVerified.asStateFlow()

    private val _availableCameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    val availableCameras: StateFlow<List<CameraInfo>> = _availableCameras.asStateFlow()

    init {
        detectCameras()
    }

    fun verifyPin(enteredPin: String): Boolean {
        val matches = enteredPin == settings.value.adminPin
        _pinVerified.value = matches
        return matches
    }

    fun updateSetting(transform: BoothSettings.() -> BoothSettings) {
        viewModelScope.launch {
            settingsManager.update(transform)
        }
    }

    fun detectCameras() {
        try {
            val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
            val cameras = systemCameraManager.cameraIdList.map { id ->
                val characteristics = systemCameraManager.getCameraCharacteristics(id)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                val isExternal = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_EXTERNAL
                CameraInfo(id = id, facing = facing, isExternal = isExternal)
            }
            _availableCameras.value = cameras
        } catch (e: Exception) {
            _availableCameras.value = emptyList()
        }
    }
}
