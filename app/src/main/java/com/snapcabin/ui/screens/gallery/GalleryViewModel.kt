package com.snapcabin.ui.screens.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GalleryPhoto(
    val uri: String,
    val timestamp: Long,
    val thumbnail: Bitmap? = null
)

data class GalleryUiState(
    val photos: List<GalleryPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val selectedPhoto: GalleryPhoto? = null,
    val fullBitmap: Bitmap? = null
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "GalleryViewModel"
    }

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val photos = withContext(Dispatchers.IO) {
                loadSnapCabinPhotos()
            }
            _uiState.value = _uiState.value.copy(
                photos = photos,
                isLoading = false
            )
        }
    }

    fun selectPhoto(photo: GalleryPhoto) {
        _uiState.value = _uiState.value.copy(selectedPhoto = photo)
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadFullPhoto(photo.uri)
            }
            _uiState.value = _uiState.value.copy(fullBitmap = bitmap)
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPhoto = null, fullBitmap = null)
    }

    private fun loadSnapCabinPhotos(): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.RELATIVE_PATH
            )

            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%SnapCabin%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext() && photos.size < 100) {
                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()

                    photos.add(GalleryPhoto(uri = uri, timestamp = date))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load gallery photos", e)
        }
        return photos
    }

    private fun loadFullPhoto(uriString: String): Bitmap? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load full photo", e)
            null
        }
    }
}
