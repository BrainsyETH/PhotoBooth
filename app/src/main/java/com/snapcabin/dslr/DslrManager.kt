package com.snapcabin.dslr

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Native DSLR control over USB (PTP) — the path that lets a Canon/Nikon work
 * WITHOUT an HDMI capture stick, the way dedicated booth apps do it.
 *
 * MILESTONE 1 (this slice): detect a PTP camera, get USB permission, open a
 * session, and read the camera model. That proves the transport works on a
 * given tablet + camera before we build live view (`EOS_GetViewFinderData`) and
 * remote capture (`EOS_RemoteRelease`) on top of the same [PtpTransport].
 *
 * The connection (session + claimed interface) is held open after a successful
 * connect so the later milestones can reuse it.
 */
class DslrManager(private val context: Context) {

    companion object {
        private const val TAG = "DslrManager"
        private const val ACTION_USB_PERMISSION = "com.snapcabin.dslr.USB_PERMISSION"
    }

    sealed class State {
        /** No PTP camera plugged in. */
        data object NoCamera : State()
        /** A PTP camera is present but not yet connected. */
        data class Detected(val productName: String?) : State()
        data object Connecting : State()
        data class Connected(val info: PtpDeviceInfo) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.NoCamera)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usbManager get() = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var transport: PtpTransport? = null
    private var permissionReceiver: BroadcastReceiver? = null

    /** Refresh detection (call when entering the camera screen / on USB attach). */
    fun refresh() {
        if (_state.value is State.Connected || _state.value is State.Connecting) return
        val device = findPtpDevice()
        _state.value = if (device == null) State.NoCamera else State.Detected(device.productName)
    }

    /** First PTP (still-image class) device the tablet currently sees. */
    fun findPtpDevice(): UsbDevice? = try {
        usbManager.deviceList.values.firstOrNull { device ->
            (0 until device.interfaceCount).any {
                device.getInterface(it).interfaceClass == Ptp.USB_CLASS_STILL_IMAGE
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "USB enumeration failed", e)
        null
    }

    /** Begin connecting: request USB permission if needed, then open + query. */
    fun connect() {
        val device = findPtpDevice()
        if (device == null) {
            _state.value = State.NoCamera
            return
        }
        _state.value = State.Connecting
        if (usbManager.hasPermission(device)) {
            openAndQuery(device)
        } else {
            requestPermission(device)
        }
    }

    private fun requestPermission(device: UsbDevice) {
        registerPermissionReceiver()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), flags
        )
        usbManager.requestPermission(device, intent)
    }

    private fun registerPermissionReceiver() {
        if (permissionReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_USB_PERMISSION) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val device: UsbDevice? =
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (granted && device != null) {
                    openAndQuery(device)
                } else {
                    _state.value = State.Error(
                        "USB permission denied. Reconnect the camera and tap Allow."
                    )
                }
            }
        }
        permissionReceiver = receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }

    private fun openAndQuery(device: UsbDevice) {
        scope.launch {
            try {
                val connection = usbManager.openDevice(device)
                    ?: throw IllegalStateException("Couldn't open the USB device.")
                val t = PtpTransport.open(device, connection)
                if (t == null) {
                    connection.close()
                    throw IllegalStateException("No usable PTP interface on this camera.")
                }
                transport = t

                // OpenSession, then GetDeviceInfo (model/serial live in its dataset).
                val (openResp, _) = t.transact(Ptp.OP_OPEN_SESSION, intArrayOf(Ptp.SESSION_ID))
                if (!openResp.ok) {
                    throw IllegalStateException("OpenSession failed (0x${openResp.code.toString(16)}).")
                }
                val (infoResp, infoData) = t.transact(Ptp.OP_GET_DEVICE_INFO)
                if (!infoResp.ok || infoData == null) {
                    throw IllegalStateException("GetDeviceInfo failed (0x${infoResp.code.toString(16)}).")
                }
                val info = PtpDeviceInfo.parse(infoData)
                Log.i(TAG, "Connected to ${info.manufacturer} ${info.model} (serial ${info.serialNumber}), " +
                    "${Ptp.vendorName(info.vendorExtensionId)} ext, EOS-remote=${info.supportsEosRemote}")
                _state.value = State.Connected(info)
            } catch (e: Exception) {
                Log.e(TAG, "DSLR connect failed", e)
                disconnect()
                _state.value = State.Error(e.message ?: "Connection failed.")
            }
        }
    }

    fun disconnect() {
        try {
            transport?.transact(Ptp.OP_CLOSE_SESSION)
        } catch (_: Exception) {
        }
        transport?.close()
        transport = null
    }
}
