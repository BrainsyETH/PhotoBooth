package com.snapcabin.ui.screens.collage

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.collage.CollageLayout
import com.snapcabin.collage.CollageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CollageUiState(
    val photos: List<Bitmap> = emptyList(),
    val selectedLayout: CollageLayout = CollageLayout.SINGLE,
    val previewBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val maxPhotos: Int = 1
)

@HiltViewModel
class CollageViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CollageUiState())
    val uiState: StateFlow<CollageUiState> = _uiState.asStateFlow()

    fun addPhoto(bitmap: Bitmap) {
        val current = _uiState.value.photos.toMutableList()
        val max = _uiState.value.selectedLayout.photoCount
        if (current.size < max) {
            current.add(bitmap)
            _uiState.value = _uiState.value.copy(photos = current)
            renderPreview()
        }
    }

    fun selectLayout(layout: CollageLayout) {
        _uiState.value = _uiState.value.copy(
            selectedLayout = layout,
            maxPhotos = layout.photoCount
        )
        renderPreview()
    }

    fun removeLastPhoto() {
        val current = _uiState.value.photos.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(current.lastIndex)
            _uiState.value = _uiState.value.copy(photos = current)
            renderPreview()
        }
    }

    fun getCollageBitmap(): Bitmap? = _uiState.value.previewBitmap

    fun needsMorePhotos(): Boolean {
        return _uiState.value.photos.size < _uiState.value.selectedLayout.photoCount
    }

    fun reset() {
        _uiState.value = CollageUiState()
    }

    private fun renderPreview() {
        val photos = _uiState.value.photos
        if (photos.isEmpty()) {
            _uiState.value = _uiState.value.copy(previewBitmap = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = withContext(Dispatchers.Default) {
                CollageRenderer.render(photos, _uiState.value.selectedLayout)
            }
            _uiState.value = _uiState.value.copy(
                previewBitmap = result,
                isProcessing = false
            )
        }
    }
}
