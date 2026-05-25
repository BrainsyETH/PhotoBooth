package com.snapcabin.session

import android.graphics.Bitmap
import com.snapcabin.ui.screens.capture.CaptureMode
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Source of truth for "the current capture session." Lives for the lifetime
 * of the Activity (survives configuration changes) so the Review and Share
 * screens can read the photo without having to reach back into the Capture
 * back-stack entry. That reach-back pattern crashed during the goHome
 * navigation transition: CAPTURE was popped before SHARE was disposed and
 * navController.getBackStackEntry(Routes.CAPTURE) threw, which the global
 * crash handler then escalated to a process restart.
 *
 * A session ends when the Attract screen becomes current; at that point we
 * reset() and the activePhoto / photos go back to empty.
 */
@ActivityRetainedScoped
class CaptureSession @Inject constructor() {

    private val _state = MutableStateFlow(CaptureSessionState())
    val state: StateFlow<CaptureSessionState> = _state.asStateFlow()

    /** Called by CaptureViewModel when a burst completes. */
    fun setCaptureResult(mode: CaptureMode, photos: List<Bitmap>) {
        _state.value = _state.value.copy(mode = mode, photos = photos)
    }

    /**
     * Called by Review when the user accepts the photo (single mode, collage
     * mode, or the first GIF frame). The Share screen reads this to display
     * and process the chosen photo.
     */
    fun setActivePhoto(bitmap: Bitmap?) {
        _state.value = _state.value.copy(activePhoto = bitmap)
    }

    /** Called when the user starts a new burst or session ends. */
    fun reset() {
        _state.value = CaptureSessionState()
    }
}

data class CaptureSessionState(
    val mode: CaptureMode = CaptureMode.Single,
    val photos: List<Bitmap> = emptyList(),
    val activePhoto: Bitmap? = null
)
