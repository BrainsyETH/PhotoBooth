package com.photobooth.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobooth.filter.WatermarkRenderer
import com.photobooth.settings.BoothSettings
import com.photobooth.settings.SettingsManager
import com.photobooth.share.EmailSmsSharer
import com.photobooth.share.LocalPhotoServer
import com.photobooth.share.PhotoPrinter
import com.photobooth.share.PhotoSaver
import com.photobooth.share.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
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
    private val localPhotoServer: LocalPhotoServer,
    private val settingsManager: SettingsManager,
    private val photoPrinter: PhotoPrinter,
    private val emailSmsSharer: EmailSmsSharer
) : ViewModel() {

    companion object {
        private const val TAG = "ShareViewModel"
    }

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    fun setPhoto(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            // Apply watermark if configured
            val processedPhoto = withContext(Dispatchers.Default) {
                val s = settings.value
                if (s.watermarkEnabled && s.watermarkText.isNotBlank()) {
                    WatermarkRenderer.apply(bitmap, s.watermarkText)
                } else {
                    bitmap
                }
            }

            _uiState.value = _uiState.value.copy(photo = processedPhoto)

            // Auto-save if enabled
            if (settings.value.autoSaveToGallery) {
                saveToGallery(context)
            }

            // Start QR sharing if enabled
            if (settings.value.enableQrSharing && settings.value.enableLocalServer) {
                startLocalServer(processedPhoto, context)
            }
        }
    }

    fun saveToGallery(context: Context) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val quality = settings.value.outputQuality
            val path = withContext(Dispatchers.IO) {
                photoSaver.saveToGallery(context, photo, quality = quality)
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
                Log.w(TAG, "Local server failed to start", e)
            }
        }
    }

    /**
     * Get local IP address using modern ConnectivityManager API (Android 23+)
     * with WifiManager fallback for older Samsung firmware.
     */
    private fun getLocalIpAddress(context: Context): String? {
        // Modern approach: ConnectivityManager (works on Android M+)
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(network)
                linkProperties?.linkAddresses?.forEach { linkAddress ->
                    val address = linkAddress.address
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ConnectivityManager IP lookup failed, trying WifiManager", e)
        }

        // Fallback for older devices / Samsung firmware quirks
        return getLocalIpViaWifiManager(context)
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpViaWifiManager(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ip = wifiManager?.connectionInfo?.ipAddress ?: return null
        if (ip == 0) return null
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }

    fun printPhoto(context: Context) {
        val photo = _uiState.value.photo ?: return
        photoPrinter.print(context, photo)
    }

    fun shareViaEmail(context: Context) {
        val photo = _uiState.value.photo ?: return
        emailSmsSharer.shareViaEmail(context, photo)
    }

    fun shareViaSms(context: Context) {
        val photo = _uiState.value.photo ?: return
        emailSmsSharer.shareViaSms(context, photo)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        super.onCleared()
        localPhotoServer.stopServing()
    }
}
