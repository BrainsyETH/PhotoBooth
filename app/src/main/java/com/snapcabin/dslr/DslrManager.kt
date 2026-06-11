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

    sealed class Capture {
        data object Idle : Capture()
        data class Busy(val phase: String) : Capture()
        data class Done(val jpeg: ByteArray, val bitmap: android.graphics.Bitmap?) : Capture()
        data class Failed(val message: String) : Capture()
    }

    private val _state = MutableStateFlow<State>(State.NoCamera)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _capture = MutableStateFlow<Capture>(Capture.Idle)
    val capture: StateFlow<Capture> = _capture.asStateFlow()

    /**
     * Human-readable trace of the last capture attempt, shown on-screen in admin
     * so the operator can report what happened without pulling logcat over adb
     * (hard when the camera occupies the USB port).
     */
    private val _diagnostics = MutableStateFlow<List<String>>(emptyList())
    val diagnostics: StateFlow<List<String>> = _diagnostics.asStateFlow()

    private fun diag(msg: String) {
        Log.d(TAG, msg)
        _diagnostics.value = (_diagnostics.value + msg).takeLast(60)
    }

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

                // Canon needs "PC connection" + event mode on before it will
                // accept shutter-release commands. Best-effort; capture re-asserts.
                if (info.isCanon && info.supportsEosRemote) {
                    prepareRemote(t, "connect")
                    drainEvents(t, "connect")
                }

                _state.value = State.Connected(info)
            } catch (e: Exception) {
                Log.e(TAG, "DSLR connect failed", e)
                disconnect()
                _state.value = State.Error(e.message ?: "Connection failed.")
            }
        }
    }

    /**
     * MILESTONE 2 — take one photo on the DSLR and download it. Canon EOS flow:
     * ensure remote+event mode → trigger shutter (RemoteReleaseOn/Off) → poll
     * GetEvent for the "object added / ready to transfer" event → download via
     * GetPartialObject → TransferComplete. Every phase logs, so a single run
     * reveals exactly where any camera-specific tuning is needed.
     */
    fun captureTestPhoto() {
        val t = transport
        if (t == null) {
            _capture.value = Capture.Failed("Not connected.")
            return
        }
        if (_capture.value is Capture.Busy) return
        scope.launch {
            try {
                _diagnostics.value = emptyList()
                diag("— capture start —")
                _capture.value = Capture.Busy("Preparing camera…")
                prepareRemote(t, "capture")
                drainEvents(t, "pre-shutter")

                // Snapshot the card's object handles BEFORE the shutter so we
                // can detect the new image by diff even if EOS events never
                // arrive (some bodies queue events differently).
                val handlesBefore = listObjectHandles(t)
                diag("pre-shutter handles: ${handlesBefore?.size ?: "GetObjectHandles unavailable"}")

                _capture.value = Capture.Busy("Firing shutter…")
                // Full press (AF + shutter), then release.
                logResp("RemoteReleaseOn(3,0)", t.transact(Ptp.OP_EOS_REMOTE_RELEASE_ON, intArrayOf(3, 0)))
                logResp("RemoteReleaseOff(3)", t.transact(Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(3)))

                _capture.value = Capture.Busy("Waiting for the photo…")
                val handle = pollForObject(t, timeoutMs = 8_000)
                    ?: pollForNewHandle(t, handlesBefore, timeoutMs = 15_000)
                    ?: throw java.io.IOException(
                        "Shutter fired but the new image never appeared (no EOS event, no new " +
                            "object handle). Check the lens is on AF, a card is in and not full, " +
                            "and image quality includes JPEG — then see logcat."
                    )

                _capture.value = Capture.Busy("Downloading photo…")
                val jpeg = downloadObject(t, handle)
                diag("downloaded ${jpeg.size} bytes from 0x${handle.toString(16)}")
                val bmp = try {
                    android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                } catch (e: Exception) {
                    null
                }
                _capture.value = Capture.Done(jpeg, bmp)
            } catch (e: Exception) {
                Log.e(TAG, "DSLR capture failed", e)
                _capture.value = Capture.Failed(e.message ?: "Capture failed.")
            }
        }
    }

    fun clearCapture() {
        if (_capture.value !is Capture.Busy) _capture.value = Capture.Idle
    }

    /**
     * Put the camera in PC-remote + event mode, LOGGING the response codes —
     * a silent SetEventMode failure looks exactly like "shutter fires but no
     * events ever arrive", which is undiagnosable without these lines.
     */
    private fun prepareRemote(t: PtpTransport, label: String) {
        runCatching {
            logResp("[$label] SetRemoteMode(1)", t.transact(Ptp.OP_EOS_SET_REMOTE_MODE, intArrayOf(1)))
            logResp("[$label] SetEventMode(1)", t.transact(Ptp.OP_EOS_SET_EVENT_MODE, intArrayOf(1)))
        }.onFailure { Log.w(TAG, "[$label] remote-mode setup threw", it) }
    }

    /** Drain queued/stale events (incl. the big initial dump after SetEventMode). */
    private fun drainEvents(t: PtpTransport, label: String) {
        var total = 0
        runCatching {
            repeat(8) {
                val (_, data) = t.transact(Ptp.OP_EOS_GET_EVENT)
                val n = data?.size ?: 0
                total += n
                if (data == null || data.size <= 8) return@runCatching
            }
        }.onFailure { diag("[$label] drain threw: ${it.message}") }
        diag("[$label] drained $total event bytes")
    }

    /** Poll GetEvent until an object-added / ready-to-transfer event names a handle. */
    private fun pollForObject(t: PtpTransport, timeoutMs: Long): Int? {
        val deadline = System.currentTimeMillis() + timeoutMs
        var polls = 0
        while (System.currentTimeMillis() < deadline) {
            val (resp, data) = t.transact(Ptp.OP_EOS_GET_EVENT)
            if (data != null && data.size > 8) {
                parseEventsForObjectHandle(data)?.let { return it }
            }
            // Heartbeat (~once/sec) so the log shows whether events flow at all.
            if (polls++ % 7 == 0) {
                Log.v(TAG, "GetEvent poll: ${data?.size ?: -1} bytes rc=0x${resp.code.toString(16)}")
            }
            Thread.sleep(150)
        }
        return null
    }

    /**
     * Vendor-neutral fallback: list every object handle on the camera and
     * return the first one that wasn't there before the shutter. Standard PTP
     * (GetObjectHandles), so it works even when EOS event delivery doesn't.
     */
    private fun listObjectHandles(t: PtpTransport): Set<Long>? = try {
        // 0xFFFFFFFF = all storages; 0,0 = any format, any association.
        val (resp, data) = t.transact(Ptp.OP_GET_OBJECT_HANDLES, intArrayOf(-1, 0, 0))
        if (!resp.ok || data == null || data.size < 4) {
            diag("GetObjectHandles rc=0x${resp.code.toString(16)} dataLen=${data?.size ?: -1}")
            null
        } else {
            val n = PtpBytes.readU32(data, 0).toInt().coerceIn(0, 1_000_000)
            val handles = HashSet<Long>(n * 2)
            for (i in 0 until n) {
                if (4 + i * 4 + 4 <= data.size) handles.add(PtpBytes.readU32(data, 4 + i * 4))
            }
            handles
        }
    } catch (e: Exception) {
        Log.w(TAG, "GetObjectHandles failed", e)
        null
    }

    private fun pollForNewHandle(t: PtpTransport, before: Set<Long>?, timeoutMs: Long): Int? {
        if (before == null) {
            diag("no event + no handle snapshot → can't diff")
            return null
        }
        diag("no EOS event; diffing object handles…")
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val now = listObjectHandles(t) ?: return null
            val fresh = now.firstOrNull { it !in before }
            if (fresh != null) {
                diag("new object via diff: 0x${fresh.toString(16)}")
                return fresh.toInt()
            }
            Thread.sleep(400)
        }
        diag("handle diff found nothing new after ${timeoutMs}ms")
        return null
    }

    /**
     * Walk the GetEvent record stream (each: u32 size · u32 eventCode · payload),
     * logging every record, and return the object handle from the first
     * ObjectAdded / RequestObjectTransfer record (handle = first u32 of payload).
     */
    private fun parseEventsForObjectHandle(data: ByteArray): Int? {
        var off = 0
        while (off + 8 <= data.size) {
            val size = PtpBytes.readU32(data, off).toInt()
            if (size < 8 || off + size > data.size) break
            val code = PtpBytes.readU32(data, off + 4).toInt()
            val payloadStart = off + 8
            val previewLen = minOf(size - 8, 24)
            diag("event 0x${code.toString(16)} sz=$size ${hex(data, payloadStart, previewLen)}")
            when (code) {
                Ptp.EC_EOS_OBJECT_ADDED_EX, Ptp.EC_EOS_OBJECT_ADDED_EX64,
                Ptp.EC_EOS_REQUEST_OBJECT_TRANSFER, Ptp.EC_EOS_REQUEST_OBJECT_TRANSFER64 -> {
                    if (size >= 12) return PtpBytes.readU32(data, payloadStart).toInt()
                }
            }
            off += size
        }
        return null
    }

    /** Download a Canon EOS object in chunks via GetPartialObject, then ack. */
    private fun downloadObject(t: PtpTransport, handle: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val chunk = 0x200000 // 2 MB
        var offset = 0
        while (true) {
            val (resp, data) = t.transact(
                Ptp.OP_EOS_GET_PARTIAL_OBJECT, intArrayOf(handle, offset, chunk)
            )
            if (data == null || data.isEmpty()) {
                if (!resp.ok) Log.w(TAG, "GetPartialObject rc=0x${resp.code.toString(16)} at offset $offset")
                break
            }
            out.write(data)
            offset += data.size
            if (data.size < chunk) break // final short chunk
        }
        runCatching { t.transact(Ptp.OP_EOS_TRANSFER_COMPLETE, intArrayOf(handle)) }

        if (out.size() == 0) {
            // Card objects on some bodies only answer the STANDARD GetObject.
            Log.w(TAG, "EOS GetPartialObject returned nothing; trying standard GetObject")
            val (resp, data) = t.transact(Ptp.OP_GET_OBJECT, intArrayOf(handle))
            Log.d(TAG, "GetObject rc=0x${resp.code.toString(16)} dataLen=${data?.size ?: -1}")
            if (data != null) return data
        }
        return out.toByteArray()
    }

    private fun logResp(label: String, r: Pair<PtpTransport.PtpResponse, ByteArray?>) {
        diag("$label → rc=0x${r.first.code.toString(16)}${if (r.first.ok) " ok" else " FAIL"}")
    }

    private fun hex(b: ByteArray, start: Int, len: Int): String {
        val sb = StringBuilder()
        for (i in start until minOf(start + len, b.size)) {
            sb.append(String.format("%02x ", b[i]))
        }
        return sb.toString().trim()
    }

    fun disconnect() {
        try {
            transport?.transact(Ptp.OP_CLOSE_SESSION)
        } catch (_: Exception) {
        }
        transport?.close()
        transport = null
        _capture.value = Capture.Idle
    }
}
