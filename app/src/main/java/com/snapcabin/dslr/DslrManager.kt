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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

        /**
         * Longest a guest waits on the DSLR after the on-screen flash before
         * the booth gives up and uses the tablet camera. Without a budget the
         * internal retries (busy retries, event poll, handle diff, one
         * reconnect) could stack to 20–30 s of a frozen screen.
         */
        const val BOOTH_SHOT_BUDGET_MS = 8_000L

        /** After this many DSLR misses in a row, stop trying it for [PAUSE_MS]. */
        internal const val FAILURES_BEFORE_PAUSE = 2
        internal const val PAUSE_MS = 3 * 60_000L

        /** Idle connection check: finds a dead pipe BEFORE a guest's shot does. */
        private const val HEARTBEAT_MS = 30_000L
    }

    /**
     * Booth-side DSLR health, shown in admin. When the DSLR keeps missing
     * (autofocus can't lock in a dark room, card full, battery dying), every
     * guest would otherwise wait out the full budget on every single shot all
     * night. After [FAILURES_BEFORE_PAUSE] misses the booth goes straight to
     * the tablet camera for [PAUSE_MS], then tries the DSLR again.
     */
    data class BoothHealth(
        val consecutiveFailures: Int = 0,
        val pausedUntil: Long = 0L,
        val lastFailure: String? = null,
        val dslrShots: Int = 0,
        val fallbacks: Int = 0
    ) {
        fun isPaused(now: Long = System.currentTimeMillis()) = now < pausedUntil

        fun afterSuccess() = copy(consecutiveFailures = 0, pausedUntil = 0L, dslrShots = dslrShots + 1)

        fun afterFailure(reason: String, now: Long): BoothHealth {
            val failures = consecutiveFailures + 1
            return copy(
                consecutiveFailures = failures,
                pausedUntil = if (failures >= FAILURES_BEFORE_PAUSE) now + PAUSE_MS else pausedUntil,
                lastFailure = reason,
                fallbacks = fallbacks + 1
            )
        }

        /** Fresh chance for the DSLR; tonight's counters are kept. */
        fun cleared() = copy(consecutiveFailures = 0, pausedUntil = 0L)
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
     * True once the DSLR has produced a real photo (admin test OR booth shot)
     * on the CURRENT connection. Cleared whenever the transport drops (unplug,
     * auto power-off, release), so setup readiness can't go stale.
     */
    private val _verified = MutableStateFlow(false)
    val verified: StateFlow<Boolean> = _verified.asStateFlow()

    private val _boothHealth = MutableStateFlow(BoothHealth())
    val boothHealth: StateFlow<BoothHealth> = _boothHealth.asStateFlow()

    /** Operator action (or the camera coming back): give the DSLR a fresh chance now. */
    fun resetBoothHealth() {
        _boothHealth.value = _boothHealth.value.cleared()
    }

    private fun recordBoothSuccess() {
        _boothHealth.value = _boothHealth.value.afterSuccess()
    }

    private fun recordBoothFailure(reason: String) {
        val updated = _boothHealth.value.afterFailure(reason, System.currentTimeMillis())
        if (updated.isPaused()) {
            diag("DSLR missed ${updated.consecutiveFailures} shots in a row — using the tablet camera " +
                "for ${PAUSE_MS / 60_000} min")
        }
        _boothHealth.value = updated
    }

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

    init {
        // The held UsbDeviceConnection dies silently when the camera unplugs
        // or powers off (auto power off!) — without this, the app keeps
        // claiming Connected and every capture talks into a dead pipe
        // ("bulk OUT failed at 0/12" on the very first command).
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val device: UsbDevice? =
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                val wasPtp = device != null && (0 until device.interfaceCount).any {
                    device.getInterface(it).interfaceClass == Ptp.USB_CLASS_STILL_IMAGE
                }
                if (!wasPtp) return
                if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                    // Camera powered back on / replugged: show it, and give it
                    // a fresh chance instead of waiting out a failure pause.
                    // The next countdown's arm reconnects if permission is held
                    // (granted automatically via the manifest USB filter once
                    // the operator ticks "Always open SnapCabin").
                    resetBoothHealth()
                    refresh()
                    return
                }
                if (action != UsbManager.ACTION_USB_DEVICE_DETACHED) return
                scope.launch {
                    captureMutex.withLock { dropTransport() }
                    val present = findPtpDevice()
                    _state.value =
                        if (present == null) State.NoCamera else State.Detected(present.productName)
                }
            }
        }
        // Protected system broadcast — exempt from the API 34 flag requirement
        // (same pattern as AdminViewModel's attach/detach receiver).
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            }
        )

        scope.launch {
            while (true) {
                delay(HEARTBEAT_MS)
                heartbeat()
            }
        }
    }

    /**
     * Cheap idle check on a held connection. A camera that powered off, lost
     * its battery or got bumped leaves a dead pipe that otherwise surfaces
     * only at a guest's flash (as a reconnect-and-retry inside the budget).
     * Skips whenever a shot is armed or anything else holds the camera. For
     * Canon it also drains the event queue so it can't back up between shots.
     */
    private suspend fun heartbeat() {
        if (transport == null || armedShot != null) return
        if (!captureMutex.tryLock()) return
        try {
            val t = transport ?: return
            val info = deviceInfo
            if (info != null && info.isCanon && info.supportsEosRemote) {
                t.transact(Ptp.OP_EOS_GET_EVENT)
            } else if (info != null && info.usesStandardCapture && info.supportsNikonCheckEvent) {
                t.transact(Ptp.OP_NIKON_CHECK_EVENT) // also keeps Nikon's event queue drained
            } else {
                t.transact(Ptp.OP_GET_DEVICE_INFO)
            }
        } catch (e: java.io.IOException) {
            diag("heartbeat: connection lost (${e.message}) — will reconnect at the next shot")
            dropTransport()
            val present = findPtpDevice()
            _state.value = if (present == null) State.NoCamera else State.Detected(present.productName)
        } catch (e: Exception) {
            Log.w(TAG, "DSLR heartbeat failed", e)
        } finally {
            captureMutex.unlock()
        }
    }

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
        resetBoothHealth()
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
            "(staged=${info.supportsStagedRelease} oneShot=${info.supportsOneShotRelease}), " +
            "standard=${info.usesStandardCapture}")

        // Canon needs "PC connection" + event mode on before it will
        // accept shutter-release commands. Best-effort; capture re-asserts.
        if (info.isCanon && info.supportsEosRemote) {
            if (openResp.code == Ptp.RESP_SESSION_ALREADY_OPEN) {
                // The previous run died without CloseSession (crash, cable
                // yank) — the body kept its state and may be wedged.
                diag("session was already open — clearing stale state from a previous run")
            }
            prepareRemote(t, "connect")
            if (info.supportsStagedRelease) {
                // A run that died mid-shot can leave the "button" held down:
                // screen black, AF engaged, further releases refused. Clearing
                // a button that isn't held is a harmless no-op.
                runCatching { t.transact(Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(2)) }
                runCatching { t.transact(Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(1)) }
            }
            drainEvents(t, "connect")
        }
        if (info.usesStandardCapture) {
            // Nikon & other non-EOS bodies: no remote-mode handshake. Record
            // which capture/event ops the body offers — the first thing to
            // read in the on-screen trace when a new model misbehaves.
            diag("${info.manufacturer} ${info.model}: standard capture — " +
                "InitiateCapture=${info.supportsInitiateCapture} " +
                "NikonSDRAM=${info.supportsNikonSdramCapture} " +
                "CheckEvent=${info.supportsNikonCheckEvent} " +
                "DeviceReady=${info.supportsNikonDeviceReady} " +
                "eventEndpoint=${t.hasEventEndpoint}")
            drainStandardEvents(t, "connect")
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
        if (_capture.value is Capture.Busy) return
        _capture.value = Capture.Busy("Preparing camera…")
        scope.launch {
            captureMutex.withLock {
                try {
                    _diagnostics.value = emptyList()
                    diag("— capture start —")
                    // Generous timeouts: this is a diagnostic, let it look hard.
                    val jpeg = captureWithReconnect(eventTimeoutMs = 8_000, diffTimeoutMs = 15_000)
                    val bmp = try {
                        android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                    } catch (e: Exception) {
                        null
                    }
                    _capture.value = Capture.Done(jpeg, bmp)
                    if (bmp != null) {
                        _verified.value = true
                        resetBoothHealth() // operator just proved it works
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "DSLR capture failed", e)
                    if (e is java.io.IOException && e !is NoPhotoException) {
                        // Transport is dead and even a fresh reconnect failed —
                        // stop claiming Connected; the UI offers CONNECT DSLR.
                        dropTransport()
                        _state.value = State.Error(
                            "Lost the camera connection — check the cable and power, then tap CONNECT DSLR."
                        )
                    }
                    _capture.value = Capture.Failed(e.message ?: "Capture failed.")
                }
            }
        }
    }

    /**
     * Run the capture sequence, reconnecting fresh and retrying ONCE if the
     * transport turns out to be dead. The held connection dies silently when
     * the camera auto-powers-off or is replugged — the detach broadcast
     * usually catches it, but this is the backstop (caller holds the mutex).
     */
    private fun captureWithReconnect(eventTimeoutMs: Long, diffTimeoutMs: Long): ByteArray {
        return try {
            val t = transport ?: reconnectForCapture()
            captureOnce(t, eventTimeoutMs, diffTimeoutMs)
        } catch (e: NoPhotoException) {
            throw e
        } catch (e: java.io.IOException) {
            diag("transport failed (${e.message}) — reconnecting and retrying…")
            dropTransport()
            captureOnce(reconnectForCapture(), eventTimeoutMs, diffTimeoutMs)
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
    /**
     * @param onShutter called (on an IO thread) the moment the shutter actually
     *   fires, so the booth can flash/beep in sync with the real exposure
     *   instead of a beat before it. Not called if the camera never fires.
     */
    suspend fun captureBoothPhoto(
        maxDimension: Int,
        onShutter: () -> Unit = {}
    ): android.graphics.Bitmap {
        if (_boothHealth.value.isPaused()) {
            throw NoPhotoException("DSLR paused after repeated misses — using the tablet camera.")
        }
        // The capture runs in this manager's own scope: its USB work is
        // blocking and can't be interrupted, but the GUEST can stop waiting.
        val shot = scope.async { captureBoothPhotoLocked(maxDimension, onShutter) }
        val bitmap = try {
            withTimeoutOrNull(BOOTH_SHOT_BUDGET_MS) { shot.await() }
        } catch (e: CancellationException) {
            shot.cancel() // guest skipped / session reset
            throw e
        } catch (e: Exception) {
            recordBoothFailure(e.message ?: e.javaClass.simpleName)
            throw e
        }
        if (bitmap == null) {
            // If it's still queued behind the camera lock, this stops it from
            // firing the shutter seconds later at nobody. Mid-transfer work
            // finishes in the background and its result is dropped.
            shot.cancel()
            recordBoothFailure("no photo within ${BOOTH_SHOT_BUDGET_MS / 1000}s")
            throw NoPhotoException("The DSLR took too long — using the tablet camera.")
        }
        recordBoothSuccess()
        return bitmap
    }

    private suspend fun captureBoothPhotoLocked(
        maxDimension: Int,
        onShutter: () -> Unit
    ): android.graphics.Bitmap =
        withContext(Dispatchers.IO) {
            captureMutex.withLock {
                _capture.value = Capture.Busy("Booth capture")
                try {
                    val jpeg = try {
                        val t = transport ?: reconnectForCapture()
                        val arm = armedShot
                        armedShot = null
                        if (arm != null) {
                            fireArmedShot(t, arm, onShutter)
                        } else {
                            captureOnce(t, eventTimeoutMs = 6_000, diffTimeoutMs = 6_000, onShutter = onShutter)
                        }
                    } catch (e: NoPhotoException) {
                        throw e // session is fine; the shot just didn't happen
                    } catch (e: java.io.IOException) {
                        // Dead pipe (auto power off, replug). One fresh
                        // reconnect + full sequence — a late DSLR photo still
                        // beats the tablet fallback.
                        Log.w(TAG, "DSLR transport failed mid-shot; reconnecting once", e)
                        dropTransport()
                        captureOnce(reconnectForCapture(), eventTimeoutMs = 6_000, diffTimeoutMs = 6_000, onShutter = onShutter)
                    }
                    val bitmap = decodeScaled(jpeg, maxDimension)
                        ?: throw NoPhotoException("Couldn't decode the DSLR photo.")
                    _verified.value = true
                    bitmap
                } catch (e: NoPhotoException) {
                    throw e
                } catch (e: java.io.IOException) {
                    // Reconnect didn't help either — surface it and let the
                    // caller fall back to the tablet camera.
                    dropTransport()
                    _state.value = State.Error(e.message ?: "DSLR connection lost.")
                    throw e
                } finally {
                    if (_capture.value is Capture.Busy) _capture.value = Capture.Idle
                }
            }
        }

    /** Pre-armed shot state: setup is done and (when staged) AF is locked on a held half-press. */
    private data class ArmedShot(val handlesBefore: Set<Long>?, val halfPressed: Boolean)
    private var armedShot: ArmedShot? = null

    /**
     * Pre-arm the next booth shot. Called at the TOP of the on-screen countdown
     * so all the slow work — remote/event mode, destination, event drain,
     * handle snapshot, half-press AF settle — happens while guests watch
     * "3…2…1", and the flash moment only needs the full-press (~200 ms).
     * Without this the exposure landed seconds after the on-screen flash, when
     * everyone had already relaxed.
     *
     * Best-effort: on any failure the armed state is simply not set and the
     * flash-time capture runs the full sequence (late photo beats no photo).
     */
    suspend fun armBoothCapture() {
        if (_boothHealth.value.isPaused()) return // tablet camera handles this shot
        armBoothCaptureLocked()
    }

    private suspend fun armBoothCaptureLocked() = withContext(Dispatchers.IO) {
        captureMutex.withLock {
            try {
                try {
                    armLocked()
                } catch (e: java.io.IOException) {
                    if (e is NoPhotoException) throw e
                    // Dead pipe — reconnect during the countdown so flash time
                    // still only needs the full-press.
                    diag("arm transport failed (${e.message}) — reconnecting…")
                    dropTransport()
                    armLocked()
                }
            } catch (e: Exception) {
                Log.w(TAG, "DSLR arm failed; flash-time capture will run the full sequence", e)
            }
        }
    }

    private fun armLocked() {
        val t = transport ?: reconnectForCapture()
        disarmLocked(t) // release any stale half-press from an abandoned arm
        if (deviceInfo?.usesStandardCapture == true) {
            // Standard/Nikon capture has no half-press to hold. Arming still
            // pays off: any reconnect and the stale-event drain happen during
            // "3…2…1" instead of after the flash.
            diag("— arming for countdown (standard capture) —")
            drainStandardEvents(t, "arm")
            return
        }
        diag("— arming for countdown —")
        prepareRemote(t, "arm")
        setEosProperty(
            t, Ptp.DPC_EOS_CAPTURE_DESTINATION, Ptp.EOS_DEST_CARD,
            label = "CaptureDestination=card"
        )
        drainEvents(t, "arm")
        pendingObjectHandle = null
        val handlesBefore = listObjectHandles(t)
        val staged = deviceInfo?.supportsStagedRelease != false
        if (staged) {
            releaseOp(t, "ReleaseOn(1) half-press (arm)", Ptp.OP_EOS_REMOTE_RELEASE_ON, intArrayOf(1, 0))
        }
        armedShot = ArmedShot(handlesBefore, staged)
    }

    /**
     * Release a held half-press without taking a photo — for countdowns that
     * get skipped, cancelled, or error out between arm and flash. Leaving the
     * button "held" keeps the AF system engaged and drains the battery.
     */
    suspend fun disarmBoothCapture() = withContext(Dispatchers.IO) {
        captureMutex.withLock {
            val t = transport
            if (t == null) {
                armedShot = null
            } else {
                disarmLocked(t)
            }
        }
    }

    private fun disarmLocked(t: PtpTransport) {
        val arm = armedShot ?: return
        armedShot = null
        if (arm.halfPressed) {
            runCatching { releaseOp(t, "ReleaseOff(1) disarm", Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(1)) }
        }
    }

    /** Flash-time completion of a pre-armed shot: full-press, locate, download. */
    private fun fireArmedShot(t: PtpTransport, arm: ArmedShot, onShutter: () -> Unit): ByteArray {
        _capture.value = Capture.Busy("Firing shutter…")
        var fired = false
        if (arm.halfPressed) {
            val full = releaseOp(t, "ReleaseOn(2) full-press", Ptp.OP_EOS_REMOTE_RELEASE_ON, intArrayOf(2, 0))
            if (full.ok) onShutter()
            releaseOp(t, "ReleaseOff(2)", Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(2))
            releaseOp(t, "ReleaseOff(1)", Ptp.OP_EOS_REMOTE_RELEASE_OFF, intArrayOf(1))
            fired = full.ok
        }
        if (!fired && deviceInfo?.supportsOneShotRelease == true) {
            if (releaseOp(t, "RemoteRelease(0x910F)", Ptp.OP_EOS_REMOTE_RELEASE, IntArray(0)).ok) onShutter()
        }

        _capture.value = Capture.Busy("Waiting for the photo…")
        val handle = pendingObjectHandle
            ?: pollForObject(t, timeoutMs = 5_000)
            ?: pollForNewHandle(t, arm.handlesBefore, timeoutMs = 5_000)
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
    private fun captureOnce(
        t: PtpTransport,
        eventTimeoutMs: Long,
        diffTimeoutMs: Long,
        onShutter: () -> Unit = {}
    ): ByteArray {
        if (deviceInfo?.usesStandardCapture == true) {
            return standardCaptureOnce(t, timeoutMs = eventTimeoutMs + diffTimeoutMs, onShutter = onShutter)
        }
        disarmLocked(t) // this path does its own half-press; a stale held one would confuse it
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
        fireShutter(t, onShutter)

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
    private fun fireShutter(t: PtpTransport, onShutter: () -> Unit) {
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
            if (full.ok) onShutter()
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
            if (releaseOp(t, "RemoteRelease(0x910F)", Ptp.OP_EOS_REMOTE_RELEASE, IntArray(0)).ok) onShutter()
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
        if (deviceInfo?.usesStandardCapture == true) return // EOS-only; never eat a Nikon ObjectAdded
        runCatching {
            val (_, data) = t.transact(Ptp.OP_EOS_GET_EVENT)
            if (data != null && data.size > 8) {
                parseEventsForObjectHandle(data)?.let { pendingObjectHandle = it }
            }
        }
    }

    // ───────────────────── Standard PTP / Nikon capture ─────────────────────
    //
    // Nikon DSLRs (D3100 etc.) and other non-Canon bodies: fire with the
    // standard InitiateCapture (photo lands on the card), learn the new
    // object from Nikon's CheckEvent, the interrupt endpoint, or — always
    // available — a before/after diff of the card's object handles; pick the
    // JPEG (RAW+JPEG announces two objects) and download it with GetObject.
    // Nikon's InitiateCaptureRecInSdram is the fallback when the standard op
    // is missing or refused. Every step lands in the on-screen trace.

    /** One batch of pending events from whichever source this body offers. */
    private fun standardEvents(t: PtpTransport, waitMs: Int): List<PtpParsing.Event> {
        val info = deviceInfo
        val events = when {
            info?.supportsNikonCheckEvent == true -> {
                val (resp, data) = t.transact(Ptp.OP_NIKON_CHECK_EVENT)
                if (resp.ok) PtpParsing.parseNikonEvents(data) else emptyList()
            }
            t.hasEventEndpoint -> listOfNotNull(t.readEvent(waitMs))
            else -> emptyList()
        }
        events.forEach { diag("event 0x${it.code.toString(16)} param=0x${it.param.toString(16)}") }
        return events
    }

    /** Clear stale events so an old ObjectAdded can't be mistaken for this shot. */
    private fun drainStandardEvents(t: PtpTransport, label: String) {
        var total = 0
        repeat(8) {
            val n = standardEvents(t, waitMs = 30).size
            total += n
            if (n == 0) {
                if (total > 0) diag("[$label] drained $total stale event(s)")
                return
            }
        }
    }

    /** Nikon: wait while the body answers DeviceBusy (AF, card write). No-op elsewhere. */
    private fun waitDeviceReady(t: PtpTransport, timeoutMs: Long) {
        if (deviceInfo?.supportsNikonDeviceReady != true) return
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val code = t.transact(Ptp.OP_NIKON_DEVICE_READY).first.code
            if (code != Ptp.RESP_DEVICE_BUSY) return
            Thread.sleep(100)
        }
        diag("DeviceReady still busy after ${timeoutMs}ms")
    }

    private fun standardCaptureOnce(t: PtpTransport, timeoutMs: Long, onShutter: () -> Unit = {}): ByteArray {
        val info = deviceInfo
        _capture.value = Capture.Busy("Preparing camera…")
        drainStandardEvents(t, "pre-shutter")
        waitDeviceReady(t, 3_000)
        val handlesBefore = listObjectHandles(t)
        diag("pre-shutter handles: ${handlesBefore?.size ?: "GetObjectHandles unavailable"}")

        _capture.value = Capture.Busy("Firing shutter…")
        var resp: PtpTransport.PtpResponse? = null
        var sdram = false
        if (info?.supportsInitiateCapture == true) {
            // StorageID 0 / format 0 = "camera's choice": the card, as JPEG/RAW per its settings.
            resp = releaseOp(t, "InitiateCapture(0x100E)", Ptp.OP_INITIATE_CAPTURE, intArrayOf(0, 0))
        }
        val standardRefused = resp == null ||
            (!resp.ok && resp.code != Ptp.RESP_NIKON_OUT_OF_FOCUS)
        if (standardRefused && info?.supportsNikonSdramCapture == true) {
            // 0xFFFFFFFF = autofocus as the body is configured.
            resp = releaseOp(
                t, "Nikon InitiateCaptureRecInSdram(0x90C0)",
                Ptp.OP_NIKON_INITIATE_CAPTURE_REC_IN_SDRAM, intArrayOf(-1)
            )
            sdram = resp.ok
        }
        val fired = resp ?: throw NoPhotoException(
            "This camera doesn't offer a USB capture operation, so it can't take photos remotely."
        )
        when {
            fired.code == Ptp.RESP_NIKON_OUT_OF_FOCUS -> throw NoPhotoException(
                "The camera couldn't lock focus, so it didn't take the photo. Switch the lens to " +
                    "MF and pre-focus where guests stand."
            )
            !fired.ok -> throw NoPhotoException(
                "The camera refused the capture (0x${fired.code.toString(16)}). Check a memory card " +
                    "is in and not full, the camera isn't showing a menu or playback, and the mode " +
                    "dial is on P, S, A or M."
            )
        }
        onShutter() // InitiateCapture accepted = the exposure is happening now
        val shotAt = System.currentTimeMillis()

        _capture.value = Capture.Busy("Waiting for the photo…")
        val candidates = LinkedHashSet<Long>()
        var completed = false
        var lastAddAt = 0L
        var lastDiffAt = 0L
        val canPollEvents = info?.supportsNikonCheckEvent == true || t.hasEventEndpoint
        val deadline = shotAt + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val now = System.currentTimeMillis()
            if (canPollEvents) {
                val events = standardEvents(t, waitMs = 200)
                PtpParsing.addedHandles(events).forEach { if (candidates.add(it)) lastAddAt = now }
                if (PtpParsing.captureCompleted(events)) completed = true
                if (events.isEmpty() && info?.supportsNikonCheckEvent == true) Thread.sleep(150)
            }
            // Card diff as a backstop for bodies whose events don't arrive
            // (or don't exist): from 1 s after the shot, every ~600 ms. It
            // keeps running after the first find so RAW+JPEG's second file
            // (written a beat later) is caught before the quiet-period break.
            if (!sdram && handlesBefore != null &&
                now - shotAt > 1_000 && now - lastDiffAt >= 600
            ) {
                lastDiffAt = now
                val after = listObjectHandles(t)
                after?.filter { it !in handlesBefore }?.forEach {
                    if (candidates.add(it)) {
                        lastAddAt = now
                        diag("new object via diff: 0x${it.toString(16)}")
                    }
                }
            }
            // RAW+JPEG announces two objects — give the second a moment.
            if (candidates.isNotEmpty() && (completed || now - lastAddAt > 1_000)) break
            if (!canPollEvents) {
                // Nothing to listen to: SDRAM shots go straight to Nikon's
                // default handle; without a card listing there's nothing to diff.
                if (sdram || handlesBefore == null) break
                Thread.sleep(200)
            }
        }
        if (candidates.isEmpty() && sdram) {
            diag("no ObjectAdded for the SDRAM capture — trying Nikon's default handle")
            candidates.add(Ptp.NIKON_SDRAM_HANDLE)
        }
        if (candidates.isEmpty()) {
            throw NoPhotoException(
                "Shutter fired but no new image appeared. If autofocus can't lock the camera may " +
                    "refuse to fire — set the lens to MF; check a card is in and not full."
            )
        }

        waitDeviceReady(t, 5_000) // let the card write finish before reading it
        val handle = pickJpeg(t, candidates.toList()) ?: throw NoPhotoException(
            "The camera saved no JPEG — set Image quality to JPEG (Fine) or RAW+JPEG."
        )
        _capture.value = Capture.Busy("Downloading photo…")
        val (getResp, data) = transactRetryBusy(t, Ptp.OP_GET_OBJECT, intArrayOf(handle.toInt()))
        if (!getResp.ok || data == null || data.size < 2 ||
            data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()
        ) {
            throw NoPhotoException(
                "Couldn't download the photo (GetObject rc=0x${getResp.code.toString(16)}, " +
                    "${data?.size ?: 0} bytes)."
            )
        }
        diag("downloaded ${data.size} bytes from 0x${handle.toString(16)}")
        return data
    }

    /** First JPEG among [handles]; skips folders and RAW. Falls back to an unidentifiable one. */
    private fun pickJpeg(t: PtpTransport, handles: List<Long>): Long? {
        var unknown: Long? = null
        for (h in handles) {
            val (resp, data) = runCatching { transactRetryBusy(t, Ptp.OP_GET_OBJECT_INFO, intArrayOf(h.toInt())) }
                .getOrElse { if (it is java.io.IOException) throw it; null to null }
            val format = if (resp?.ok == true) PtpParsing.objectFormat(data) else -1
            diag("object 0x${h.toString(16)} format=0x${format.toString(16)}")
            when (format) {
                Ptp.FORMAT_EXIF_JPEG -> return h
                -1 -> if (unknown == null) unknown = h // SDRAM objects may not describe themselves
            }
        }
        return unknown
    }

    /** A transaction retried while the body answers DeviceBusy (card still writing, etc.). */
    private fun transactRetryBusy(t: PtpTransport, op: Int, params: IntArray): Pair<PtpTransport.PtpResponse, ByteArray?> {
        var result = t.transact(op, params)
        var retries = 0
        while (result.first.code == Ptp.RESP_DEVICE_BUSY && retries++ < 20) {
            Thread.sleep(150)
            result = t.transact(op, params)
        }
        return result
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
        // No catch: transact only throws on transport death (refusals come
        // back as response codes), and a dead pipe must abort the capture so
        // the caller's reconnect logic can run — not limp on to the shutter.
        logResp("[$label] SetRemoteMode(1)", t.transact(Ptp.OP_EOS_SET_REMOTE_MODE, intArrayOf(1)))
        logResp("[$label] SetEventMode(1)", t.transact(Ptp.OP_EOS_SET_EVENT_MODE, intArrayOf(1)))
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
        }.onFailure {
            diag("$label threw: ${it.message}")
            if (it is java.io.IOException) throw it // dead pipe — abort, don't limp on
        }
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
        }.onFailure {
            diag("[$label] drain threw: ${it.message}")
            if (it is java.io.IOException) throw it // dead pipe — abort, don't limp on
        }
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
    } catch (e: java.io.IOException) {
        throw e // dead pipe — abort the capture so reconnect logic can run
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

    /**
     * Operator-facing release: hand the camera back to its own controls
     * without unplugging. While the app holds a session in PC-remote mode the
     * body's rear screen is black and its buttons are dead (normal EOS tether
     * behavior, not a fault) — this is the way out.
     */
    fun releaseCamera() {
        scope.launch {
            captureMutex.withLock { disconnect() }
            val device = findPtpDevice()
            _state.value = if (device == null) State.NoCamera else State.Detected(device.productName)
        }
    }

    fun disconnect() {
        transport?.let { t ->
            runCatching { disarmLocked(t) } // never leave a half-press held
            // Undo PC-remote mode BEFORE closing: without SetRemoteMode(0) the
            // body can stay locked (black screen, dead buttons) until the cable
            // is pulled or it's power-cycled. Best-effort — the transport may
            // already be dead when we get here.
            if (deviceInfo?.let { it.isCanon && it.supportsEosRemote } == true) {
                runCatching { t.transact(Ptp.OP_EOS_SET_EVENT_MODE, intArrayOf(0)) }
                runCatching { t.transact(Ptp.OP_EOS_SET_REMOTE_MODE, intArrayOf(0)) }
            }
            runCatching { t.transact(Ptp.OP_CLOSE_SESSION) }
        }
        dropTransport()
    }

    /**
     * Drop the connection WITHOUT PTP goodbyes — for when the pipe is already
     * dead (unplug, camera power-off). Talking into a dead pipe just burns USB
     * timeouts; what matters is to stop claiming Connected so the next use
     * reconnects fresh.
     */
    private fun dropTransport() {
        transport?.close()
        transport = null
        deviceInfo = null
        armedShot = null
        _verified.value = false
        _capture.value = Capture.Idle
    }
}
