package com.snapcabin.ui.screens.getready

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.camera.CameraManager
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GetReadyViewModel @Inject constructor(
    val cameraManager: CameraManager,
    settingsManager: SettingsManager
) : ViewModel() {

    val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    /** Real stored settings (null until first load) — bind the camera from THIS,
     *  never from [settings], whose initial value is defaults. */
    val loadedSettings: StateFlow<BoothSettings?> = settingsManager.loaded
}
