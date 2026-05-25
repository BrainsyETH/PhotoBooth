package com.snapcabin.session

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin wrapper so NavGraph composables can read and mutate the
 * @ActivityRetainedScoped CaptureSession via hiltViewModel(). The
 * session itself is the source of truth; this view-model exists only
 * because composables consume DI through hiltViewModel(), not direct
 * @Inject parameters.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val captureSession: CaptureSession
) : ViewModel() {

    val state = captureSession.state

    fun setActivePhoto(bitmap: Bitmap?) = captureSession.setActivePhoto(bitmap)

    fun reset() = captureSession.reset()
}
