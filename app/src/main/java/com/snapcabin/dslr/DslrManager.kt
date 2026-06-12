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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    /**
     * The capture sequence ran but no image materialized (AF refusal, missing
     * card, unsupported ops). The USB session itself is still healthy — callers
     * must NOT treat this like a transport death.
     */
    class NoPhotoException(message: String) : java.io.IOException(message)

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
    private var deviceInfo: PtpDeviceInfo? = null
    private var permissionReceiver: BroadcastReceiver? = null

    /** One capture (test or booth) on the wire at a time. */
    private val captureMutex = Mutex()

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
                _state.value = State.Connected(openTransport(device))
            } catch (e: Exception) {
                Log.e(TAG, "DSLR connect failed", e)
                disconnect()
                _state.value = State.Error(e.message ?: "Connection failed.")
            }
        }
    }

    /** Open the USB device, claim PTP, open a session, identify. IO thread only. */
    private fun openTransport(device: UsbDevice): PtpDeviceInfo {
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
        deviceInfo = info
        Log.i(TAG, "Connected to ${info.manufacturer} ${info.model} (serial ${info.serialNumber}), " +
            "${Ptp.vendorName(info.vendorExtensionId)} ext, EOS-remote=${info.supportsEosRemote} " +
            "(staged=${info.supportsStagedRelease} oneShot=${info.supportsOneShotRelease})")

        // Canon needs "PC connection" + event mode on before it will
        // accept shutter-release commands. Best-effort; capture re-asserts.
        if (info.isCanon && info.supportsEosRemote) {
            prepareRemote(t, "connect")
            drainEvents(t, "connect")
        }
        return info
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
            captureMutex.withLock {
                try {
                    _diagnostics.value = emptyList()
                    diag("— capture start —")
                    // Generous timeouts: this is a diagnostic, let it look hard.
                    val jpeg = captureOnce(t, eventTimeoutMs = 8_000, diffTimeoutMs = 15_000)
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
    }

    /**
     * Booth-flow capture: the guest-facing path behind "Use DSLR for photos".
     * The tablet keeps showing its own (front) camera as the live preview; this
     * fires the DSLR and returns the downloaded frame, scaled to [maxDimension]
     * (a 24 MP JPEG decoded full-size is ~100 MB of ARGB — it must be sampled
     * down before it enters the Bitmap pipeline).
     *
     * Throws on any failure so the caller can fall back to the tablet camera —
     * a booth must never strand guests mid-countdown. Timeouts are tighter than
     * the admin test's: a guest is standing there waiting.
     */
    suspend fun captureBoothPhoto(maxDimension: Int): android.graphics.Bitmap =
        withContext(Dispatchers.IO) {
            captureMutex.withLock {
                val t = transport ?: reconnectForCapture()
                _capture.value = Capture.Busy("Booth capture")
                try {
                    val jpeg = captureOnce(t, eventTimeoutMs = 6_000, diffTimeoutMs = 6_000)
                    decodeScaled(jpeg, maxDimension)
                        ?: throw NoPhotoException("Couldn't decode the DSLR photo.")
                } catch (e: NoPhotoException) {
                    throw e // session is fine; the shot just didn't happen
                } catch (e: java.io.IOException) {
                    // Bulk transfer death usually means the camera unplugged or
                    // powered off — drop the session so the next shot reconnects.
                    disconnect()
                    _state.value = State.Error(e.message ?: "DSLR connection lost.")
                    throw e
                } finally {
                    if (_capture.value is Capture.Busy) _capture.value = Capture.Idle
                }
            }
        }

    /**
     * The booth can reach capture with no live session (app restarted, camera
     * auto-powered-off and back). If the DSLR is present and USB permission is
     * still held — it persists until unplug — reconnect silently.
     */
    private fun reconnectForCapture(): PtpTransport {
        val device = findPtpDevice()
            ?: throw java.io.IOException("DSLR not detected on USB.")
        if (!usbManager.hasPermission(device)) {
            throw java.io.IOException("No USB permission for the DSLR — reconnect it in Admin → Camera.")
        }
        val info = openTransport(device)
        _state.value = State.Connected(info)
        return transport ?: throw java.io.IOException("DSLR connection failed.")
    }

    /** Full capture sequence on an open transport: prepare → fire → locate → download. */
    private fun captureOnce(t: PtpTransport, eventTimeoutMs: Long, diffTimeoutMs: Long): ByteArray {
        _capture.value = Capture.Busy("Preparing camera…")
        prepareRemote(t, "capture")

        // Route captures to the SD CARD. In PC-remote mode many EOS
        // bodies default to holding the frame in camera RAM for the
        // host to collect (announced only via events) — observed on
        // the 850D as "shutter fires, card object count never moves."
        // On the card, the new photo MUST appear as a new object
        // handle, so the diff fallback works even with zero events.
        setEosProperty(
            t, Ptp.DPC_EOS_CAPTURE_DESTINATION, Ptp.EOS_DEST_CARD,
            label = "CaptureDestination=card"
        )
        drainEvents(t, "pre-shutter")

        // Snapshot the card's object handles BEFORE the shutter so we
        // can detect the new image by diff even if EOS events never
        // arrive (some bodies queue events differently).
        val handlesBefore = listObjectHandles(t)
        diag("pre-shutter handles: ${handlesBefore?.size ?: "GetObjectHandles unavailable"}")

        pendingObjectHandle = null // anything seen before the shutter is stale
        fireShutter(t)

        _capture.value = Capture.Busy("Waiting for the photo…")
        // The busy-retry pumps inside fireShutter may have already consumed the
        // ObjectAdded event — check the stash before polling for a fresh one.
        val handle = pendingObjectHandle
            ?: pollForObject(t, timeoutMs = eventTimeoutMs)
            ?: pollForNewHandle(t, handlesBefore, timeoutMs = diffTimeoutMs)
            ?: throw NoPhotoException(
                "Shutter fired but the new image never appeared (no EOS event, no new " +
                    "object handle). If autofocus can't lock, the camera silently refuses " +
                    "to fire — set the lens to MF, check a card is in and not full, and " +
                    "that image quality includes JPEG."
            )

        _capture.value = Capture.Busy("Downloading photo…")
        val jpeg = downloadObject(t, handle)
        diag("downloaded ${jpeg.size} bytes from 0x${handle.toString(16)}")
        return jpeg
    }

    /**
     * Trigger the shutter using whichever release ops this body actually
     * advertises — older Rebels only have the one-shot 0x910F; newer bodies
     * have the staged 0x9128/0x9129 pair.
     *
     * Staged release works like a finger on the button: half-press (AF),
     * settle, full-press, release both. The one-shot (3,0) variant returned ok
     * but never completed an exposure on the 850D — with AF priority, a release
     * without focus lock silently refuses to fire (the AF motor noise
     * masquerades as a shutter). If the full-press is REFUSED outright, fall
     * back to 0x910F when available rather than waiting on a photo that will
     * never exist.
     */
    private fun fireShutter(t: PtpTransport) {
        val info = deviceInfo
        val staged = info?.supportsStagedRelease != false // unknown → try staged first
        val oneShot = info?.supportsOneShotRelease == true

        if (staged) {
            _capture.value = Capture.Busy("Focusing…")
            releaseOp(t, "ReleaseOn(1) half-press", Ptp.OP_EOS_REMOTE_RELEASE_ON, intArrayOf(1, 0))
            // Hold the half-press while AF works, keeping events flowing — the
            // body answers DeviceBusy (0x2019) to a full-press until focus
            // settles, and a backed-up event queue keeps some bodies busy.
            repeat(4) {
                pumpEvents(t)
                Thread.sleep(200)
            }
            _capture.value = Capture.Busy("Firing shutter…")
            val full = releaseOp(t, "ReleaseOn(2) full-press", Ptp.OP_EOS_REMOTE_RELEASE_ON, intArrayOf(2, 0))
            releaseOp(t, "ReleaseOff(2)", Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(2))
            releaseOp(t, "ReleaseOff(1)", Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(1))
            if (full.ok) return
            if (full.code == Ptp.RESP_DEVICE_BUSY) {
                diag("full-press busy through every retry — AF likely can't lock; set the lens to MF")
            }
            diag("full-press refused (0x${full.code.toString(16)})" +
                if (oneShot) " — trying one-shot 0x910F" else "")
            if (!oneShot) return // poll anyway; the trace has the refusal on record
        } else {
            diag("body doesn't advertise 0x9128 staged release" +
                if (oneShot) " — using one-shot 0x910F" else "")
        }

        if (oneShot) {
            _capture.value = Capture.Busy("Firing shutter…")
            releaseOp(t, "RemoteRelease(0x910F)", Ptp.OP_EOS_REMOTE_RELEASE, IntArray(0))
        } else if (!staged) {
            throw NoPhotoException(
                "This camera advertises neither EOS release operation (0x9128/0x910F) — " +
                    "USB remote capture isn't possible on this body."
            )
        }
    }

    /**
     * Run a release op, retrying while the camera answers DeviceBusy (0x2019).
     * EOS bodies report busy to a full-press while AF is still settling and to
     * any release while the previous frame is writing — gphoto2 treats busy as
     * "ask again", not failure (observed live: treating the first 0x2019 as
     * fatal is exactly why the shutter never fired). Events are pumped between
     * attempts so a clogged queue can't keep the body busy forever.
     */
    private fun releaseOp(t: PtpTransport, label: String, op: Int, params: IntArray): PtpTransport.PtpResponse {
        var resp = t.transact(op, params).first
        var retries = 0
        while (resp.code == Ptp.RESP_DEVICE_BUSY && retries < 10) {
            retries++
            pumpEvents(t)
            Thread.sleep(200)
            resp = t.transact(op, params).first
        }
        diag("$label → rc=0x${resp.code.toString(16)}${if (resp.ok) " ok" else " FAIL"}" +
            if (retries > 0) " ($retries busy retries)" else "")
        return resp
    }

    /**
     * One GetEvent, parsed — NOT discarded. The busy-retry loops drain events
     * as a side effect, and the ObjectAdded announcement for the new photo can
     * arrive there (e.g. while ReleaseOff retries during the card write);
     * dropping it would turn a successful exposure into a "no photo" timeout.
     */
    private var pendingObjectHandle: Int? = null
    private fun pumpEvents(t: PtpTransport) {
        runCatching {
            val (_, data) = t.transact(Ptp.OP_EOS_GET_EVENT)
            if (data != null && data.size > 8) {
                parseEventsForObjectHandle(data)?.let { pendingObjectHandle = it }
            }
        }
    }

    /** Decode a JPEG to at most [maxDimension] on its long edge, sampling down in-decoder. */
    private fun decodeScaled(jpeg: ByteArray, maxDimension: Int): android.graphics.Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        if (maxDimension < Int.MAX_VALUE) {
            var longEdge = maxOf(bounds.outWidth, bounds.outHeight)
            while (longEdge / 2 >= maxDimension) {
                sample *= 2
                longEdge /= 2
            }
        }
        val opts = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
            inMutable = true // downstream branding stamps in-place
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        return android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts)
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

    /**
     * Set a Canon EOS device property. SetDevicePropValueEx carries the value
     * in the DATA phase: u32 totalSize · u32 propCode · u32 value.
     */
    private fun setEosProperty(t: PtpTransport, prop: Int, value: Int, label: String) {
        runCatching {
            val data = ByteArray(12)
            PtpBytes.writeU32(data, 0, 12)
            PtpBytes.writeU32(data, 4, prop)
            PtpBytes.writeU32(data, 8, value)
            logResp(label, t.transact(Ptp.OP_EOS_SET_DEVICE_PROP_VALUE, IntArray(0), data))
        }.onFailure { diag("$label threw: ${it.message}") }
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
            // Surface what the camera is actually sending: first polls + ~every
            // 2s thereafter, in the on-screen trace.
            if (polls < 3 || polls % 13 == 0) {
                diag("poll[$polls]: ${data?.size ?: -1}B rc=0x${resp.code.toString(16)}")
            }
            polls++
            if (data != null && data.size > 8) {
                parseEventsForObjectHandle(data)?.let { return it }
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
            if (size < 8 || off + size > data.size) {
                // Don't bail silently: "data arrived but looks malformed to the
                // parser" must be distinguishable from "no events at all".
                if (off == 0) {
                    diag("unparseable event data (${data.size}B): ${hex(data, 0, 16)}")
                }
                break
            }
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
        deviceInfo = null
        _capture.value = Capture.Idle
    }
}
