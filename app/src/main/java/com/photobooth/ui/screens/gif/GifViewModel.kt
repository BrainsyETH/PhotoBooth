package com.photobooth.ui.screens.gif

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.gif.GifEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class GifUiState(
    val frames: List<Bitmap> = emptyList(),
    val maxFrames: Int = 6,
    val delayMs: Int = 500,
    val isEncoding: Boolean = false,
    val gifFile: File? = null,
    val previewFrameIndex: Int = 0
)

@HiltViewModel
class GifViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GifUiState())
    val uiState: StateFlow<GifUiState> = _uiState.asStateFlow()

    private val encoder = GifEncoder()

    fun addFrame(bitmap: Bitmap) {
        val current = _uiState.value.frames.toMutableList()
        if (current.size < _uiState.value.maxFrames) {
            current.add(bitmap)
            _uiState.value = _uiState.value.copy(frames = current)
        }
    }

    fun removeLastFrame() {
        val current = _uiState.value.frames.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(current.lastIndex)
            _uiState.value = _uiState.value.copy(frames = current)
        }
    }

    fun setDelay(delayMs: Int) {
        _uiState.value = _uiState.value.copy(delayMs = delayMs.coerceIn(100, 2000))
    }

    fun needsMoreFrames(): Boolean = _uiState.value.frames.size < 2

    fun encodeGif(context: Context) {
        val frames = _uiState.value.frames
        if (frames.size < 2) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEncoding = true)

            val file = withContext(Dispatchers.Default) {
                // Scale frames to reasonable GIF size
                val gifW = 480
                val scale = gifW.toFloat() / frames[0].width
                val gifH = (frames[0].height * scale).toInt()

                val scaledFrames = frames.map { frame ->
                    Bitmap.createScaledBitmap(frame, gifW, gifH, true)
                }

                val outputFile = File(context.cacheDir, "photobooth_${System.currentTimeMillis()}.gif")
                val success = encoder.encode(scaledFrames, _uiState.value.delayMs, outputFile)
                if (success) outputFile else null
            }

            _uiState.value = _uiState.value.copy(
                isEncoding = false,
                gifFile = file
            )
        }
    }

    fun advancePreview() {
        val frames = _uiState.value.frames
        if (frames.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                previewFrameIndex = (_uiState.value.previewFrameIndex + 1) % frames.size
            )
        }
    }

    fun reset() {
        _uiState.value = GifUiState()
    }
}
