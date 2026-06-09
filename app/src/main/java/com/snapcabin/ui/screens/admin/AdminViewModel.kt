package com.snapcabin.ui.screens.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.PhotoResolution
import com.snapcabin.settings.SettingsManager
import com.snapcabin.share.ResendEmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraInfo(
    val id: String,
    val facing: String,
    val isExternal: Boolean
)

enum class TestEmailStatus { Idle, Sending, Sent, Failed }

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val resendEmailSender: ResendEmailSender,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _testEmailStatus = MutableStateFlow(TestEmailStatus.Idle)
    val testEmailStatus: StateFlow<TestEmailStatus> = _testEmailStatus.asStateFlow()

    private val _testEmailMessage = MutableStateFlow("")
    val testEmailMessage: StateFlow<String> = _testEmailMessage.asStateFlow()

    val settings: StateFlow<BoothSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoothSettings())

    private val _pinVerified = MutableStateFlow(false)
    val pinVerified: StateFlow<Boolean> = _pinVerified.asStateFlow()

    private val _availableCameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    val availableCameras: StateFlow<List<CameraInfo>> = _availableCameras.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // Re-enumerate when a USB camera is plugged or unplugged
            detectCameras()
        }
    }

    init {
        detectCameras()
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        // RECEIVER_NOT_EXPORTED requires Tiramisu+; use the no-flag form for compatibility
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(usbReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(usbReceiver) } catch (_: Exception) { }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val matches = enteredPin == settings.value.adminPin
        _pinVerified.value = matches
        return matches
    }

    fun updateSetting(transform: BoothSettings.() -> BoothSettings) {
        viewModelScope.launch {
            settingsManager.update(transform)
        }
    }

    /**
     * Sends a small placeholder JPEG to the given address using whatever's
     * currently saved in DataStore (so the operator can verify the API key
     * and From address work end-to-end before the event starts).
     */
    fun sendResendTestEmail(toAddress: String) {
        val s = settings.value
        _testEmailStatus.value = TestEmailStatus.Sending
        _testEmailMessage.value = ""
        viewModelScope.launch {
            val placeholder = buildPlaceholderBitmap()
            val result = resendEmailSender.send(
                apiKey = s.resendApiKey,
                fromAddress = s.resendFromAddress,
                replyToAddress = s.resendReplyToAddress,
                toAddress = toAddress.trim(),
                subject = "SnapCabin test email",
                htmlBody = "<p style=\"font-family:Helvetica,Arial,sans-serif;\">" +
                    "This is a test from the SnapCabin admin screen. If you got it, " +
                    "your Resend setup is working." +
                    "</p>",
                photo = placeholder
            )
            when (result) {
                is ResendEmailSender.Result.Ok -> {
                    _testEmailStatus.value = TestEmailStatus.Sent
                    _testEmailMessage.value = "Sent. Check your inbox."
                }
                is ResendEmailSender.Result.Err -> {
                    _testEmailStatus.value = TestEmailStatus.Failed
                    _testEmailMessage.value = result.message
                }
            }
        }
    }

    fun resetTestEmailState() {
        _testEmailStatus.value = TestEmailStatus.Idle
        _testEmailMessage.value = ""
    }

    private fun buildPlaceholderBitmap(): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(0x9C, 0xAF, 0x88)) // sage
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x3A, 0x2E, 0x20)
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("SnapCabin", size / 2f, size / 2f - 8f, paint)
        paint.textSize = 22f
        paint.isFakeBoldText = false
        canvas.drawText("Test email", size / 2f, size / 2f + 28f, paint)
        return bmp
    }

    fun detectCameras() {
        try {
            val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
            val cameras = systemCameraManager.cameraIdList.map { id ->
                val characteristics = systemCameraManager.getCameraCharacteristics(id)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }
                val isExternal = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_EXTERNAL
                CameraInfo(id = id, facing = facing, isExternal = isExternal)
            }
            _availableCameras.value = cameras
        } catch (e: Exception) {
            _availableCameras.value = emptyList()
        }
    }
}
