package com.snapcabin.camera

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

data class DetectedCamera(
    val id: String,
    val facing: Int, // CameraCharacteristics.LENS_FACING_*
    val facingLabel: String,
    val isExternal: Boolean
)

/**
 * Observable truth about what the camera pipeline is ACTUALLY doing. Binding
 * used to fail silently into a front-camera fallback, which made external
 * camera setups undiagnosable — the preview showed *a* camera and nothing
 * recorded whether it was the one the operator selected.
 */
sealed class CameraBindState {
    /** Nothing bound (startup, released, or binding in flight). */
    data object Idle : CameraBindState()

    data class Bound(
        val cameraId: String,
        val facingLabel: String,
        val isExternal: Boolean,
        /** False when this is a fallback, not the camera the settings asked for. */
        val matchedRequest: Boolean,
        /** Operator-facing reason for a fallback, when one is known. */
        val note: String? = null
    ) : CameraBindState()

    data class Failed(val message: String) : CameraBindState()
}

/**
 * Power-of-two BitmapFactory sample size that brings the long edge to at most
 * [maxDim] (BitmapFactory only samples in powers of two, so the result lands
 * between maxDim/2 and maxDim).
 */
internal fun decodeSampleSize(width: Int, height: Int, maxDim: Int): Int {
    require(maxDim > 0) { "maxDim must be positive" }
    var sample = 1
    var longEdge = maxOf(width, height)
    while (longEdge > maxDim) {
        sample *= 2
        longEdge /= 2
    }
    return sample
}

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    @Volatile private var mirrorImage: Boolean = true // read on captureExecutor

    private val _bindState = MutableStateFlow<CameraBindState>(CameraBindState.Idle)
    /** What's live right now — admin surfaces this under TEST CAMERA. */
    val bindState: StateFlow<CameraBindState> = _bindState.asStateFlow()

    /**
     * Capture callbacks + the full-resolution decode run here, NOT on the main
     * thread: decoding and mirroring a 12 MP JPEG takes hundreds of ms and
     * froze the countdown/flash UI on every shot.
     */
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * The last bind request, so the camera can be re-opened WITHOUT the screen
     * doing anything — after a camera-service error, a capture that hangs, or
     * a USB camera being unplugged/replugged mid-event. Weak refs: this
     * singleton must never keep a dead screen alive.
     */
    private class BindRequest(
        val owner: WeakReference<LifecycleOwner>,
        val view: WeakReference<PreviewView>,
        val useFront: Boolean,
        val cameraId: String,
        val mirror: Boolean,
        val maxResolution: Int
    )
    private var lastRequest: BindRequest? = null
    private var recoveryAttempts = 0
    private val recoveryRunnable = Runnable {
        // Waiting for a USB camera to show up must not restart the live
        // preview every few seconds: rebind only when it would change cameras.
        if (recoveryWouldChangeNothing()) scheduleRecovery() else rebindLast("scheduled recovery")
    }

    /** Cameras a CameraX restart was already tried for (see [CameraPlan.refreshFor]). */
    private val refreshTried = mutableSetOf<String>()

    /**
     * A USB camera that keeps failing (won't open, shots time out) is skipped
     * until this time so guests get the tablet camera instead of a dark booth.
     */
    private var externalPausedUntil = 0L
    private var externalStrikes = 0
    private var openWatchdog: Runnable? = null

    private val cameraStateObserver = Observer<CameraState> { onCameraState(it) }
    private var observedCameraState: LiveData<CameraState>? = null

    companion object {
        private const val TAG = "CameraManager"

        /** A capture that hasn't called back by now is treated as a hung camera. */
        const val CAPTURE_TIMEOUT_MS = 10_000L

        /**
         * Hard ceiling on a decoded photo's long edge, even at "Full"
         * resolution. 4096 px is far beyond any print or email need, and caps
         * one ARGB frame at ~50 MB — a 50 MP sensor decoded at full size would
         * be ~200 MB and take the app down mid-event.
         */
        const val MAX_DECODE_DIMENSION = 4096

        /** Recovery backoff: 1 s, 2 s, 4 s, 8 s, 15 s, then wait for the next trigger. */
        private val RECOVERY_DELAYS_MS = longArrayOf(1_000, 2_000, 4_000, 8_000, 15_000)

        /** Any external (USB) camera; see [com.snapcabin.camera.EXTERNAL_CAMERA_ID]. */
        const val EXTERNAL_CAMERA_ID = com.snapcabin.camera.EXTERNAL_CAMERA_ID

        /** A bound USB camera that hasn't opened by now is treated as dead. */
        private const val EXTERNAL_OPEN_TIMEOUT_MS = 8_000L

        /** Failures in a row before the USB camera is skipped for a while. */
        private const val EXTERNAL_MAX_STRIKES = 2

        /** How long a failing USB camera is skipped (same as the DSLR pause). */
        private const val EXTERNAL_PAUSE_MS = 3 * 60_000L
    }

    init {
        // A USB webcam / HDMI capture stick can drop off the bus mid-event
        // (loose cable, hub brown-out) and come back. Without this the preview
        // froze on the last frame and every shot failed until someone walked
        // the guest flow back through a screen that rebinds.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val device: UsbDevice? =
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (device == null || !device.isVideoDevice()) return // DSLR (PTP), printers, hubs
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val bound = _bindState.value as? CameraBindState.Bound
                        if (bound?.isExternal == true) {
                            Log.w(TAG, "External camera detached — rebinding (fallback camera if needed)")
                            recoveryAttempts = 0
                            scheduleRecovery()
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        // A (re)plugged camera gets a clean slate.
                        clearExternalPause()
                        onWantedCameraMayBeBack("USB video device attached")
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        // Protected system broadcasts — exempt from the API 34 export-flag rule.
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter)

        // The USB broadcast arrives before the camera service has finished
        // enumerating the device, and never arrives for a camera that was
        // already plugged in at boot. This fires when the camera is really
        // usable, so the booth switches to it without guessing a delay.
        try {
            val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
            systemCameraManager.registerAvailabilityCallback(
                object : SystemCameraManager.AvailabilityCallback() {
                    override fun onCameraAvailable(cameraId: String) {
                        val bound = _bindState.value as? CameraBindState.Bound
                        if (cameraId == bound?.cameraId) return
                        if (systemCameras()?.any { it.id == cameraId && it.isExternal } != true) return
                        onWantedCameraMayBeBack("External camera $cameraId available")
                    }
                },
                mainHandler
            )
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't watch camera availability", e)
        }
    }

    private fun UsbDevice.isVideoDevice(): Boolean = (0 until interfaceCount).any {
        getInterface(it).interfaceClass == UsbConstants.USB_CLASS_VIDEO
    }

    /** The selected camera isn't the live one and may be usable now: go look. */
    private fun onWantedCameraMayBeBack(reason: String) {
        val wantsSpecific = lastRequest?.cameraId?.isNotEmpty() == true
        val bound = _bindState.value as? CameraBindState.Bound
        if (!wantsSpecific || bound?.matchedRequest == true) return
        Log.i(TAG, "$reason — trying to get the selected camera back")
        recoveryAttempts = 0
        scheduleRecovery()
    }

    /**
     * Try the selected camera again right now — after the operator grants a
     * permission or asks for a retry. Clears any failure pause.
     */
    fun retrySelectedCamera() {
        clearExternalPause()
        val bound = _bindState.value as? CameraBindState.Bound
        if (lastRequest?.cameraId?.isNotEmpty() == true && bound?.matchedRequest != true) {
            recoveryAttempts = 0
            rebindLast("retry requested")
        }
    }

    /** What the system camera service lists right now, or null if it can't be read. */
    private fun systemCameras(): List<KnownCamera>? = try {
        val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
        systemCameraManager.cameraIdList.mapNotNull { id ->
            try {
                val facing = systemCameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING)
                KnownCamera(id, facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
            } catch (e: Exception) {
                null // unplugged between the list and the lookup
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to list system cameras", e)
        null
    }

    /** True when a USB video-class (UVC) device is physically plugged in. */
    fun hasUsbVideoDevice(): Boolean = try {
        (context.getSystemService(Context.USB_SERVICE) as? UsbManager)
            ?.deviceList?.values?.any { it.isVideoDevice() } == true
    } catch (e: Exception) {
        false
    }

    /**
     * Detect all available cameras, including USB cameras the device exposes
     * via the camera HAL. Note: only UVC (USB Video Class) devices — webcams,
     * HDMI-to-USB capture sticks — can appear here. DSLRs plugged straight in
     * (Canon/Nikon) present as PTP photo-transfer devices, which Android's
     * camera service can NOT use; they need an HDMI→USB capture stick.
     */
    fun detectCameras(): List<DetectedCamera> {
        val cameras = mutableListOf<DetectedCamera>()
        try {
            val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
            for (id in systemCameraManager.cameraIdList) {
                val characteristics = systemCameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    ?: CameraCharacteristics.LENS_FACING_BACK

                val facingLabel = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Camera $id"
                }

                cameras.add(
                    DetectedCamera(
                        id = id,
                        facing = facing,
                        facingLabel = facingLabel,
                        isExternal = facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enumerate cameras", e)
        }
        return cameras
    }

    /**
     * Check if USB devices are connected (potential external cameras)
     */
    fun hasUsbDevices(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        return usbManager?.deviceList?.isNotEmpty() == true
    }

    /**
     * True when Android is exposing at least one external (USB/UVC) camera.
     */
    fun hasExternalCamera(): Boolean = detectCameras().any { it.isExternal }

    /**
     * True when the mic permission should be requested before opening a camera:
     * any USB device present OR an external camera exposed. UVC cameras are
     * composite audio+video devices the camera service won't open without
     * RECORD_AUDIO — and we must ask even before enumeration catches up, or the
     * operator gets the "record permission" error with no prompt ever shown.
     */
    fun needsAudioPermissionForExternal(): Boolean = hasUsbDevices() || hasExternalCamera()

    /**
     * What a GUEST-facing screen may ask for: only permissions that are
     * actually missing, and the microphone only when the operator selected an
     * external (UVC) camera. A tethered DSLR, printer or USB hub is not a
     * reason to show a guest an OS "record audio?" dialog — it used to pop on
     * every session, with the countdown already running behind it. Admin's
     * TEST CAMERA still uses [needsAudioPermissionForExternal] so setup asks
     * up front.
     */
    fun missingGuestPermissions(cameraId: String): Array<String> = buildList {
        if (!isGranted(android.Manifest.permission.CAMERA)) add(android.Manifest.permission.CAMERA)
        val externalSelected = cameraId == EXTERNAL_CAMERA_ID || (
            cameraId.isNotEmpty() && detectCameras().let { cams ->
                // Listed as external, or a USB camera that isn't listed yet.
                cams.any { it.id == cameraId && it.isExternal } ||
                    (cams.none { it.id == cameraId } && hasUsbVideoDevice())
            }
        )
        if (externalSelected && !isGranted(android.Manifest.permission.RECORD_AUDIO)) {
            add(android.Manifest.permission.RECORD_AUDIO)
        }
    }.toTypedArray()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFront: Boolean = true,
        cameraId: String = "",
        mirror: Boolean = true,
        maxResolution: Int = Int.MAX_VALUE
    ) {
        mirrorImage = mirror
        _bindState.value = CameraBindState.Idle
        mainHandler.removeCallbacks(recoveryRunnable)
        cancelOpenWatchdog()
        recoveryAttempts = 0 // rebindLast() restores its own count after this
        val request = BindRequest(
            WeakReference(lifecycleOwner), WeakReference(previewView),
            useFront, cameraId, mirror, maxResolution
        )
        lastRequest = request
        startBind(request)
    }

    private fun startBind(request: BindRequest) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            // A newer bind (settings change, recovery) replaced this one.
            if (lastRequest !== request) return@addListener
            val owner = request.owner.get()
            val view = request.view.get()
            if (owner == null || view == null ||
                owner.lifecycle.currentState == Lifecycle.State.DESTROYED
            ) {
                return@addListener
            }
            val provider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e(TAG, "CameraX failed to start", e)
                _bindState.value = CameraBindState.Failed("The camera couldn't start. Retrying…")
                scheduleRecovery()
                return@addListener
            }
            cameraProvider = provider

            val plan = planFor(request.cameraId, provider)
            val unseenCamera = plan.refreshFor
            if (unseenCamera != null) {
                // Plugged in after CameraX started: CameraX reads the camera
                // list once, so it has to be restarted to see this camera.
                refreshTried += unseenCamera
                refreshProvider(provider) {
                    if (lastRequest === request) startBind(request)
                }
                return@addListener
            }

            val plannedId = plan.cameraId
            val info = plannedId?.let { infoFor(provider, it) }
            try {
                val selector = when {
                    plannedId != null && info != null -> selectorForId(plannedId)
                    request.useFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                }
                bindTo(provider, owner, view, request, plan, selector, info, plainUseCases = false)
                Log.i(
                    TAG,
                    "Camera bound: ${_bindState.value} " +
                        "(requested front=${request.useFront} id=${request.cameraId})"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera, trying fallback", e)
                if (info != null && isExternalInfo(info)) noteExternalStrike("bind failed")
                tryFallbackCamera(provider, owner, view, request, plan)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Decide which camera this request should get right now. Main thread only. */
    private fun planFor(requestedId: String, provider: ProcessCameraProvider): CameraPlan {
        val known = providerCameras(provider)
        val system = systemCameras() ?: known
        refreshTried.retainAll(system.map { it.id }.toSet())
        return planCamera(
            requestedId = requestedId,
            system = system,
            provider = known,
            usbVideoPresent = hasUsbVideoDevice(),
            micGranted = isGranted(android.Manifest.permission.RECORD_AUDIO),
            externalPaused = SystemClock.elapsedRealtime() < externalPausedUntil,
            refreshTried = refreshTried
        )
    }

    /**
     * Shut CameraX down so the next [ProcessCameraProvider.getInstance] starts
     * a fresh one that re-reads the camera list. CameraX only supports cameras
     * added at runtime from 1.6; until the app moves to it, this is how a USB
     * camera plugged in after startup becomes usable without restarting the app.
     */
    @SuppressLint("VisibleForTests", "RestrictedApi")
    private fun refreshProvider(provider: ProcessCameraProvider, then: () -> Unit) {
        Log.i(TAG, "Restarting CameraX to pick up a newly connected camera")
        observedCameraState?.removeObserver(cameraStateObserver)
        observedCameraState = null
        camera = null
        imageCapture = null
        cameraProvider = null
        try {
            provider.shutdownAsync().addListener({ then() }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.w(TAG, "CameraX restart failed", e)
            then()
        }
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun cameraIdOf(info: CameraInfo): String? = try {
        Camera2CameraInfo.from(info).cameraId
    } catch (e: Exception) {
        null
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun isExternalInfo(info: CameraInfo): Boolean = try {
        Camera2CameraInfo.from(info)
            .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_EXTERNAL
    } catch (e: Exception) {
        false
    }

    /** The cameras CameraX knows — fixed when it started, so it can be stale. */
    private fun providerCameras(provider: ProcessCameraProvider): List<KnownCamera> =
        provider.availableCameraInfos.mapNotNull { info ->
            cameraIdOf(info)?.let { KnownCamera(it, isExternalInfo(info)) }
        }

    private fun infoFor(provider: ProcessCameraProvider, cameraId: String): CameraInfo? =
        provider.availableCameraInfos.firstOrNull { cameraIdOf(it) == cameraId }

    private fun selectorForId(cameraId: String): CameraSelector =
        CameraSelector.Builder()
            .addCameraFilter { infos -> infos.filter { cameraIdOf(it) == cameraId } }
            .build()

    /**
     * Bind preview + still capture to one camera and publish the result.
     * [info] is set when binding a specific camera by id (null = a built-in
     * default selector). Throws if the camera can't be bound.
     */
    private fun bindTo(
        provider: ProcessCameraProvider,
        owner: LifecycleOwner,
        view: PreviewView,
        request: BindRequest,
        plan: CameraPlan,
        selector: CameraSelector,
        info: CameraInfo?,
        plainUseCases: Boolean
    ) {
        val external = info != null && isExternalInfo(info)
        // A USB camera sits on its own stand: its picture must not turn when
        // the TABLET is rotated. CameraX assumes every camera is fixed to the
        // device and would turn the preview and photo sideways on tablets
        // whose landscape is a rotated display, so pin the rotation instead.
        val steady = external && info?.getSensorRotationDegrees(Surface.ROTATION_0) == 0

        val previewBuilder = Preview.Builder()
        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        when {
            external -> {
                // Webcams and HDMI capture sticks are 16:9. CameraX prefers
                // 4:3 by default, which picks a webcam's small 4:3 modes
                // (960x720 on a 1080p camera) and makes a capture stick
                // squeeze the HDMI picture.
                previewBuilder.setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build()
                )
                captureBuilder.setResolutionSelector(externalCaptureResolution(request.maxResolution))
            }
            !plainUseCases && request.maxResolution < Int.MAX_VALUE -> {
                captureBuilder.setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(request.maxResolution, request.maxResolution * 3 / 4),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                )
            }
        }
        if (steady) previewBuilder.setTargetRotation(Surface.ROTATION_0)

        val preview = previewBuilder.build().also { it.surfaceProvider = view.surfaceProvider }
        val capture = captureBuilder.build()
        capture.targetRotation =
            if (steady) Surface.ROTATION_0 else view.display?.rotation ?: Surface.ROTATION_0

        provider.unbindAll()
        val bound = provider.bindToLifecycle(owner, selector, preview, capture)
        imageCapture = capture
        camera = bound
        // The built-in front preview is mirrored by CameraX; a USB camera's
        // isn't. Flip it here so "Mirror Image" means the same thing for both:
        // the photo matches what guests saw on screen.
        view.scaleX = if (external && request.mirror) -1f else 1f

        val state = describeBound(bound, request.cameraId, plan)
        _bindState.value = state
        watchCameraState(bound)
        when {
            state.isExternal -> armOpenWatchdog(request)
            // Plugged in but not offered as a camera yet (still enumerating,
            // e.g. right after boot): keep checking, without touching the preview.
            plan.fallback == FallbackReason.NOT_OFFERED_BY_TABLET -> scheduleRecovery()
        }
    }

    private fun externalCaptureResolution(maxResolution: Int): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                if (maxResolution < Int.MAX_VALUE) {
                    ResolutionStrategy(
                        android.util.Size(maxResolution, maxResolution * 9 / 16),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                } else {
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                }
            )
            .build()

    /**
     * If the planned bind failed, walk the fallback chain: the built-in camera
     * the settings prefer, the other built-in, then a usable USB camera.
     */
    private fun tryFallbackCamera(
        provider: ProcessCameraProvider,
        owner: LifecycleOwner,
        view: PreviewView,
        request: BindRequest,
        plan: CameraPlan
    ) {
        val preferred = if (request.useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val other = if (request.useFront) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        val candidates = buildList<Pair<CameraSelector, CameraInfo?>> {
            // The planned camera was a specific one, so the preferred built-in is untried.
            if (plan.cameraId != null) add(preferred to null)
            add(other to null)
            val usb = planFor(EXTERNAL_CAMERA_ID, provider).cameraId
            if (usb != null && usb != plan.cameraId) {
                infoFor(provider, usb)?.let { add(selectorForId(usb) to it) }
            }
        }
        // The planned camera failed, so whatever binds now is a fallback.
        val fallbackPlan =
            if (plan.cameraId != null) plan.copy(fallback = FallbackReason.NOT_USABLE) else plan

        for ((selector, info) in candidates) {
            try {
                if (!provider.hasCamera(selector)) continue
                bindTo(provider, owner, view, request, fallbackPlan, selector, info, plainUseCases = true)
                Log.i(TAG, "Fallback camera bound: ${_bindState.value}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Fallback candidate failed", e)
            }
        }
        Log.e(TAG, "No usable cameras available")
        _bindState.value = CameraBindState.Failed(
            "No camera could be opened. Check the USB connection and that camera + microphone permissions are allowed."
        )
        // Keep trying in the background (camera service busy, USB camera
        // still enumerating) instead of leaving the booth dark all night.
        scheduleRecovery()
    }

    private fun fallbackNote(reason: FallbackReason?): String? = when (reason) {
        FallbackReason.NOT_CONNECTED -> "No USB camera is connected."
        FallbackReason.NOT_OFFERED_BY_TABLET ->
            "A USB camera is plugged in, but the tablet isn't offering it as a camera yet."
        FallbackReason.MIC_NOT_ALLOWED ->
            "Microphone access isn't allowed, and Android won't turn a USB camera on without it."
        FallbackReason.PAUSED_AFTER_FAILURES ->
            "The USB camera kept failing, so the booth is using the tablet camera for a few minutes."
        FallbackReason.NOT_USABLE -> "The selected camera couldn't be started on this tablet."
        null -> null
    }

    private fun noteExternalStrike(reason: String) {
        externalStrikes++
        Log.w(TAG, "USB camera failure $externalStrikes/$EXTERNAL_MAX_STRIKES: $reason")
        if (externalStrikes >= EXTERNAL_MAX_STRIKES) pauseExternal()
    }

    private fun pauseExternal() {
        externalStrikes = 0
        externalPausedUntil = SystemClock.elapsedRealtime() + EXTERNAL_PAUSE_MS
        Log.w(TAG, "USB camera paused for ${EXTERNAL_PAUSE_MS / 60_000} min; using a built-in camera")
    }

    private fun clearExternalPause() {
        externalStrikes = 0
        externalPausedUntil = 0L
    }

    /**
     * A USB camera can bind fine and then never open — unplugged a moment ago,
     * or refused by the camera service — and CameraX just keeps retrying behind
     * a black preview. If it isn't open shortly after binding, give guests the
     * tablet camera instead.
     */
    private fun armOpenWatchdog(request: BindRequest) {
        cancelOpenWatchdog()
        val watchdog = Runnable {
            openWatchdog = null
            val owner = request.owner.get()
            if (lastRequest !== request || owner == null) return@Runnable
            // Off screen (tablet asleep, app in the background): CameraX keeps
            // the camera closed on purpose.
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@Runnable
            if (observedCameraState?.value?.type == CameraState.Type.OPEN) return@Runnable
            Log.e(TAG, "USB camera didn't open within ${EXTERNAL_OPEN_TIMEOUT_MS}ms")
            pauseExternal()
            rebindLast("USB camera never opened")
        }
        openWatchdog = watchdog
        mainHandler.postDelayed(watchdog, EXTERNAL_OPEN_TIMEOUT_MS)
    }

    private fun cancelOpenWatchdog() {
        openWatchdog?.let { mainHandler.removeCallbacks(it) }
        openWatchdog = null
    }

    /**
     * True when a recovery rebind would land on the camera that's already
     * live — e.g. still waiting for a USB camera to appear.
     */
    private fun recoveryWouldChangeNothing(): Boolean {
        val request = lastRequest ?: return false
        val bound = _bindState.value as? CameraBindState.Bound ?: return false
        val provider = cameraProvider ?: return false
        val plan = planFor(request.cameraId, provider)
        if (plan.refreshFor != null) return false
        return if (plan.cameraId != null) {
            plan.cameraId == bound.cameraId
        } else {
            !bound.isExternal && !bound.matchedRequest
        }
    }

    /** Follow the bound camera's health; only one camera is observed at a time. */
    private fun watchCameraState(bound: Camera) {
        observedCameraState?.removeObserver(cameraStateObserver)
        observedCameraState = bound.cameraInfo.cameraState.also {
            it.observeForever(cameraStateObserver)
        }
    }

    /**
     * CameraX retries RECOVERABLE errors (camera briefly in use, too many open)
     * on its own. CRITICAL ones — camera disabled by policy, fatal HAL error,
     * stream configuration failure — leave the preview frozen for good unless
     * we re-open, so rebind with backoff. Rebinding walks the normal selection
     * + fallback chain, so a dead USB camera lands on the built-in one and
     * guests keep getting photos.
     */
    private fun onCameraState(state: CameraState) {
        // Backoff is NOT reset on OPEN: a camera that opens and then fails
        // again would otherwise flap forever. It resets on a fresh screen bind
        // or a successful capture.
        val error = state.error ?: return
        Log.w(TAG, "Camera state ${state.type} error code=${error.code} type=${error.type}")
        val bound = _bindState.value as? CameraBindState.Bound
        if (error.type != CameraState.ErrorType.CRITICAL) {
            // CameraX retries these forever — pointless once the camera has
            // been unplugged, so move on as soon as the system stops listing it.
            if (bound?.isExternal == true &&
                systemCameras()?.none { it.id == bound.cameraId } == true && !hasUsbVideoDevice()
            ) {
                scheduleRecovery()
            }
            return
        }
        if (bound?.isExternal == true) noteExternalStrike("camera error ${error.code}")
        _bindState.value = CameraBindState.Failed(
            when (error.code) {
                CameraState.ERROR_CAMERA_DISABLED -> "The camera is disabled on this tablet (device policy)."
                CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> "Do Not Disturb is blocking the camera — turn it off."
                CameraState.ERROR_STREAM_CONFIG -> "The camera rejected this resolution — try a lower one."
                else -> "The camera stopped responding. Reconnecting…"
            }
        )
        scheduleRecovery()
    }

    private fun scheduleRecovery() {
        val attempt = recoveryAttempts
        if (attempt >= RECOVERY_DELAYS_MS.size) {
            Log.e(TAG, "Camera recovery gave up after $attempt attempts; waiting for the next screen or USB event")
            return
        }
        recoveryAttempts = attempt + 1
        mainHandler.removeCallbacks(recoveryRunnable)
        mainHandler.postDelayed(recoveryRunnable, RECOVERY_DELAYS_MS[attempt])
    }

    /** Re-run the last bind if its screen is still alive. Main thread only. */
    private fun rebindLast(reason: String) {
        val req = lastRequest ?: return
        val owner = req.owner.get()
        val view = req.view.get()
        if (owner == null || view == null ||
            owner.lifecycle.currentState == Lifecycle.State.DESTROYED
        ) {
            // The screen that wanted the camera is gone; the next one binds fresh.
            return
        }
        Log.i(TAG, "Rebinding camera ($reason), attempt $recoveryAttempts")
        val attempts = recoveryAttempts // bindCamera doesn't reset the backoff
        bindCamera(owner, view, req.useFront, req.cameraId, req.mirror, req.maxResolution)
        recoveryAttempts = attempts
    }

    /**
     * Resolve which physical camera actually got bound, and whether it's the
     * one the settings asked for ([requestedId] "" = auto, sentinel = any
     * external, otherwise a specific Camera2 id; [plan] is how it was chosen).
     */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun describeBound(bound: Camera, requestedId: String, plan: CameraPlan): CameraBindState.Bound {
        val info = Camera2CameraInfo.from(bound.cameraInfo)
        val facing = try {
            info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
        } catch (e: Exception) {
            null
        }
        val isExternal = facing == CameraCharacteristics.LENS_FACING_EXTERNAL
        val facingLabel = when (facing) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Camera"
        }
        val matched = when {
            requestedId.isEmpty() -> true
            // Any USB camera satisfies the request (including one that came
            // back under a new id after a replug).
            plan.acceptAnyExternal -> isExternal
            else -> info.cameraId == requestedId
        }
        return CameraBindState.Bound(
            cameraId = info.cameraId,
            facingLabel = facingLabel,
            isExternal = isExternal,
            matchedRequest = matched,
            note = if (matched) null else fallbackNote(plan.fallback)
        )
    }

    /**
     * Capture one still. Never hangs and never takes the app down:
     *  - a camera that doesn't call back within [timeoutMs] is treated as hung
     *    (UVC sticks and some HALs do wedge) → throws, and the camera is
     *    re-opened in the background so the NEXT shot works;
     *  - the decode runs off the main thread, sampled down so the long edge is
     *    at most [maxDecodeDimension] (GIF frames and collage tiles don't need
     *    a 12 MP bitmap each — that's how a 6-frame GIF exhausted the heap);
     *  - running out of memory is reported as a failed shot, not a crash.
     */
    suspend fun takePhoto(
        maxDecodeDimension: Int = MAX_DECODE_DIMENSION,
        timeoutMs: Long = CAPTURE_TIMEOUT_MS
    ): Bitmap {
        val maxDim = maxDecodeDimension.coerceIn(320, MAX_DECODE_DIMENSION)
        val photo = try {
            withTimeoutOrNull(timeoutMs) { captureStill(maxDim) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // A USB camera that streams but can't deliver stills: after a
            // couple of misses, hand guests the tablet camera.
            mainHandler.post {
                if ((_bindState.value as? CameraBindState.Bound)?.isExternal == true) {
                    noteExternalStrike("capture failed")
                    if (SystemClock.elapsedRealtime() < externalPausedUntil) rebindLast("USB camera paused")
                }
            }
            throw e
        }
        if (photo != null) {
            mainHandler.post {
                recoveryAttempts = 0 // healthy again — future failures get a full backoff
                externalStrikes = 0
            }
            return photo
        }
        Log.e(TAG, "Capture timed out after ${timeoutMs}ms — re-opening the camera")
        mainHandler.post {
            if ((_bindState.value as? CameraBindState.Bound)?.isExternal == true) {
                noteExternalStrike("capture timed out")
            }
            recoveryAttempts = 0
            rebindLast("capture timeout")
        }
        throw IllegalStateException("The camera didn't respond.")
    }

    private suspend fun captureStill(maxDim: Int): Bitmap = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("Camera not initialized"))
            return@suspendCancellableCoroutine
        }

        val photoFile = File(
            context.cacheDir,
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            captureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val result = try {
                        decodeCapture(photoFile, maxDim)
                    } catch (oom: OutOfMemoryError) {
                        Log.e(TAG, "Out of memory decoding capture", oom)
                        null
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to decode capture", e)
                        null
                    } finally {
                        photoFile.delete()
                    }
                    when {
                        result == null -> if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException("Failed to decode photo"))
                        }
                        // Timed out / cancelled while we were decoding: free
                        // the pixels now instead of waiting for GC.
                        !continuation.isActive -> result.recycle()
                        else -> continuation.resume(result)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    photoFile.delete()
                    Log.e(TAG, "Photo capture failed", exception)
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
        )
    }

    /** Decode (sampled to [maxDim]), apply EXIF rotation + mirror onto a MUTABLE bitmap. */
    private fun decodeCapture(photoFile: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = decodeSampleSize(bounds.outWidth, bounds.outHeight, maxDim)

        // Decode mutable so downstream branding/watermark stamps can
        // write into the same buffer instead of allocating a second
        // full-resolution copy. Without this, BitmapFactory returns
        // an immutable bitmap and the share pipeline silently falls
        // back to the unbranded photo on memory pressure.
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOpts) ?: return null

        // Apply EXIF rotation — BitmapFactory ignores EXIF orientation
        val exifRotation = try {
            val exif = ExifInterface(photoFile.absolutePath)
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }

        val matrix = Matrix()
        if (exifRotation != 0f) {
            matrix.postRotate(exifRotation)
        }
        if (mirrorImage) {
            matrix.preScale(-1f, 1f)
        }
        if (matrix.isIdentity) return rawBitmap

        // Transform onto a mutable target ourselves. Despite docs
        // suggesting otherwise, Bitmap.createBitmap(src, ..., matrix)
        // returns an IMMUTABLE bitmap — which silently defeats the
        // in-place branding pipeline downstream (mirroring is on by
        // default, so every capture hit this) and forces a second
        // full-resolution copy in CustomBrandingRenderer.
        val src = RectF(0f, 0f, rawBitmap.width.toFloat(), rawBitmap.height.toFloat())
        val dst = RectF()
        matrix.mapRect(dst, src)
        val out = try {
            Bitmap.createBitmap(
                dst.width().roundToInt().coerceAtLeast(1),
                dst.height().roundToInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
        } catch (oom: OutOfMemoryError) {
            rawBitmap.recycle()
            throw oom
        }
        val canvas = Canvas(out)
        canvas.translate(-dst.left, -dst.top)
        canvas.drawBitmap(
            rawBitmap,
            matrix,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        rawBitmap.recycle()
        return out
    }

    fun release() {
        mainHandler.removeCallbacks(recoveryRunnable)
        cancelOpenWatchdog()
        lastRequest = null
        observedCameraState?.removeObserver(cameraStateObserver)
        observedCameraState = null
        cameraProvider?.unbindAll()
        camera = null
        _bindState.value = CameraBindState.Idle
    }
}
