package com.snapcabin.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers "the operator captured a real test photo with THIS camera setup"
 * for the life of the process, so the Get Started checklist doesn't forget it
 * every time the operator leaves admin to try a guest session.
 *
 * Deliberately in-memory: after a reboot or app restart the operator should
 * prove the camera again. It is also cleared whenever a USB *video* device is
 * plugged or unplugged while the app runs — even with admin closed — because
 * that can change which camera guests actually get. DSLR (PTP) and other USB
 * traffic doesn't clear it; the DSLR has its own verification in DslrManager.
 */
@Singleton
class CameraTestStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val _testedKey = MutableStateFlow<List<String>?>(null)

    /** The camera configuration key that last passed a test photo, or null. */
    val testedKey: StateFlow<List<String>?> = _testedKey.asStateFlow()

    fun record(key: List<String>) { _testedKey.value = key }

    fun clear() { _testedKey.value = null }

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val device: UsbDevice? =
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                // Unknown device → be conservative and require a fresh test.
                val isVideo = device == null || (0 until device.interfaceCount).any {
                    device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_VIDEO
                }
                if (isVideo) clear()
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        // Protected system broadcasts — exempt from the API 34 export-flag rule
        // (same pattern as DslrManager and AdminViewModel).
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter)
    }
}
