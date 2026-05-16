package com.snapcabin.ui.screens.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.camera.CameraManager
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.SettingsManager
import com.snapcabin.ui.components.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CaptureStep {
    data class Intro(val coach: String, val durationMs: Long) : CaptureStep()
    data class Count(val n: Int, val coach: String, val durationMs: Long) : CaptureStep()
    data class Flash(val shotIndex: Int, val durationMs: Long) : CaptureStep()
    data class Between(val coach: String, val durationMs: Long) : CaptureStep()
    object Done : CaptureStep()
}

data class CaptureUiState(
    val mode: CaptureMode = CaptureMode.Single,
    val currentStep: CaptureStep = CaptureStep.Intro("", 0),
    val photos: List<Bitmap> = emptyList(),
    val shotsTaken: Int = 0,
    val totalShots: Int = 3,
    val showFlash: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false,
    val capturedPhoto: Bitmap? = null
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

    private var burstJob: Job? = null

    init {
        viewModelScope.launch {
            settingsManager.settings.collect { s ->
                soundManager.soundEnabled = s.soundEnabled
                soundManager.shutterEnabled = s.shutterSoundEnabled
                soundManager.countdownBeepEnabled = s.countdownBeepEnabled
            }
        }
    }

    fun startBurst(mode: CaptureMode) {
        if (burstJob?.isActive == true) return
        if (_uiState.value.isFinished) return

        val s = settings.value
        val coachingOn = s.coachingEnabled
        val totalShots = when (mode) {
            CaptureMode.Single -> s.singleShotBurstCount.coerceAtLeast(1)
            CaptureMode.Collage -> s.collageShotCount.coerceAtLeast(1)
            CaptureMode.Gif -> s.gifFrameCount.coerceAtLeast(2)
        }

        _uiState.value = _uiState.value.copy(
            mode = mode,
            totalShots = totalShots,
            shotsTaken = 0,
            photos = emptyList(),
            isFinished = false,
            error = null
        )

        val steps = buildSteps(mode, totalShots, coachingOn)

        burstJob = viewModelScope.launch {
            try {
                for (step in steps) {
                    _uiState.value = _uiState.value.copy(currentStep = step)
                    when (step) {
                        is CaptureStep.Intro -> {
                            delay(step.durationMs)
                        }
                        is CaptureStep.Count -> {
                            // Beep on count tick
                            if (step.n == 1) {
                                soundManager.playCountdownFinalBeep()
                            } else {
                                soundManager.playCountdownBeep()
                            }
                            delay(step.durationMs)
                        }
                        is CaptureStep.Flash -> {
                            _uiState.value = _uiState.value.copy(showFlash = true, isCapturing = true)
                            soundManager.playShutter()
                            try {
                                val photo = cameraManager.takePhoto()
                                val photos = _uiState.value.photos + photo
                                _uiState.value = _uiState.value.copy(
                                    photos = photos,
                                    shotsTaken = photos.size,
                                    capturedPhoto = photo
                                )
                            } catch (e: Exception) {
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to capture photo: ${e.message}"
                                )
                            }
                            delay(step.durationMs)
                            _uiState.value = _uiState.value.copy(showFlash = false, isCapturing = false)
                        }
                        is CaptureStep.Between -> {
                            delay(step.durationMs)
                        }
                        CaptureStep.Done -> {
                            // Handled below
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    currentStep = CaptureStep.Done,
                    isFinished = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Burst failed: ${e.message}",
                    isFinished = true,
                    currentStep = CaptureStep.Done
                )
            }
        }
    }

    fun skipBurst() {
        burstJob?.cancel()
        _uiState.value = _uiState.value.copy(
            currentStep = CaptureStep.Done,
            isFinished = true,
            showFlash = false,
            isCapturing = false
        )
    }

    fun resetCapture() {
        burstJob?.cancel()
        _uiState.value = CaptureUiState()
    }

    fun getPhotos(): List<Bitmap> = _uiState.value.photos

    fun getPickedPhoto(index: Int): Bitmap? = _uiState.value.photos.getOrNull(index)

    fun setActiveSinglePhoto(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(capturedPhoto = bitmap)
    }

    private fun buildSteps(mode: CaptureMode, totalShots: Int, coachingOn: Boolean): List<CaptureStep> {
        fun coach(s: String): String = if (coachingOn) s else ""

        val steps = mutableListOf<CaptureStep>()

        if (mode == CaptureMode.Gif) {
            steps += CaptureStep.Intro(coach("Stay loose — we're rolling"), 900)
            steps += CaptureStep.Count(3, coach("Recording in"), 700)
            steps += CaptureStep.Count(2, "", 700)
            steps += CaptureStep.Count(1, "", 700)
            for (i in 1..totalShots) {
                steps += CaptureStep.Flash(i, 220)
                val between = if (i < totalShots) "" else coach("That's a wrap.")
                steps += CaptureStep.Between(between, 360)
            }
            steps += CaptureStep.Done
            return steps
        }

        val lines = listOf(
            listOf("Look right here", "Big smile", "Hold"),
            listOf("Try a silly face", "Now", "Still"),
            listOf("Look at each other", "Big laugh", "Hold"),
            listOf("Last one — natural", "Smile", "Hold")
        )
        val intro = if (mode == CaptureMode.Collage) coach("Building your collage.") else coach("Here we go.")
        steps += CaptureStep.Intro(intro, 950)

        for (s in 0 until totalShots) {
            val line = lines.getOrElse(s) { lines[0] }
            steps += CaptureStep.Count(3, coach(line[0]), 850)
            steps += CaptureStep.Count(2, coach(line[1]), 850)
            steps += CaptureStep.Count(1, coach(line[2]), 850)
            steps += CaptureStep.Flash(s + 1, 320)
            if (s < totalShots - 1) {
                val between = if (mode == CaptureMode.Collage) {
                    coach("Frame ${s + 2} of $totalShots.")
                } else {
                    coach("Lovely. One more.")
                }
                steps += CaptureStep.Between(between, 950)
            }
        }
        steps += CaptureStep.Done
        return steps
    }
}
