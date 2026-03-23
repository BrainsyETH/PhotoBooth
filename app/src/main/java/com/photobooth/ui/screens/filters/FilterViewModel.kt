package com.photobooth.ui.screens.filters

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.filter.FilterEngine
import com.photobooth.filter.OverlayRenderer
import com.photobooth.filter.PhotoFilter
import com.photobooth.filter.PhotoOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FilterUiState(
    val originalPhoto: Bitmap? = null,
    val previewPhoto: Bitmap? = null,
    val selectedFilter: PhotoFilter = PhotoFilter.NORMAL,
    val selectedOverlay: PhotoOverlay = PhotoOverlay.NONE,
    val isProcessing: Boolean = false,
    val filterThumbnails: Map<PhotoFilter, Bitmap> = emptyMap()
)

@HiltViewModel
class FilterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    fun setOriginalPhoto(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            originalPhoto = bitmap,
            previewPhoto = bitmap
        )
        generateThumbnails(bitmap)
    }

    fun selectFilter(filter: PhotoFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyEffects()
    }

    fun selectOverlay(overlay: PhotoOverlay) {
        _uiState.value = _uiState.value.copy(selectedOverlay = overlay)
        applyEffects()
    }

    fun getProcessedBitmap(): Bitmap? {
        return _uiState.value.previewPhoto
    }

    private fun applyEffects() {
        val original = _uiState.value.originalPhoto ?: return
        val filter = _uiState.value.selectedFilter
        val overlay = _uiState.value.selectedOverlay

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = withContext(Dispatchers.Default) {
                val filtered = FilterEngine.applyFilter(original, filter)
                OverlayRenderer.applyOverlay(filtered, overlay)
            }
            _uiState.value = _uiState.value.copy(
                previewPhoto = result,
                isProcessing = false
            )
        }
    }

    private fun generateThumbnails(source: Bitmap) {
        viewModelScope.launch {
            val thumbSize = 120
            val scale = minOf(
                thumbSize.toFloat() / source.width,
                thumbSize.toFloat() / source.height
            )
            val thumbW = (source.width * scale).toInt()
            val thumbH = (source.height * scale).toInt()
            val smallBitmap = Bitmap.createScaledBitmap(source, thumbW, thumbH, true)

            val thumbnails = withContext(Dispatchers.Default) {
                PhotoFilter.entries.associateWith { filter ->
                    FilterEngine.applyFilter(smallBitmap, filter)
                }
            }
            _uiState.value = _uiState.value.copy(filterThumbnails = thumbnails)
        }
    }
}
