package com.photobooth.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.share.LocalPhotoServer
import com.photobooth.share.PhotoSaver
import com.photobooth.share.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShareUiState(
    val photo: Bitmap? = null,
    val qrCodeBitmap: Bitmap? = null,
    val savedPath: String? = null,
    val isSaving: Boolean = false,
    val shareUrl: String? = null,
    val message: String? = null
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val photoSaver: PhotoSaver,
    private val qrCodeGenerator: QrCodeGenerator,
    private val localPhotoServer: LocalPhotoServer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun setPhoto(bitmap: Bitmap, context: Context) {
        _uiState.value = _uiState.value.copy(photo = bitmap)
        startLocalServer(bitmap, context)
    }

    fun saveToGallery(context: Context) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val path = withContext(Dispatchers.IO) {
                photoSaver.saveToGallery(context, photo)
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedPath = path,
                message = if (path != null) "Saved to gallery!" else "Failed to save"
            )
        }
    }

    fun shareViaIntent(context: Context) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                photoSaver.saveToCacheForSharing(context, photo)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Photo"))
        }
    }

    private fun startLocalServer(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    localPhotoServer.servePhoto(bitmap)
                }
                val ip = getLocalIpAddress(context)
                if (ip != null) {
                    val url = localPhotoServer.getPhotoUrl(ip)
                    val qr = withContext(Dispatchers.Default) {
                        qrCodeGenerator.generate(url)
                    }
                    _uiState.value = _uiState.value.copy(
                        shareUrl = url,
                        qrCodeBitmap = qr
                    )
                }
            } catch (e: Exception) {
                // Server failed to start, QR sharing won't be available
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ip = wifiManager?.connectionInfo?.ipAddress ?: return null
        if (ip == 0) return null
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        super.onCleared()
        localPhotoServer.stopServing()
    }
}
