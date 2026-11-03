package com.snapcabin.camera

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
        val matchedRequest: Boolean
    ) : CameraBindState()

    data class Failed(val message: String) : CameraBindState()
}

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var useFrontCamera: Boolean = true
    @Volatile private var mirrorImage: Boolean = true // read on captureExecutor
    private var selectedCameraId: String = ""

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
    private val recoveryRunnable = Runnable { rebindLast("scheduled recovery") }

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

        /**
         * Sentinel camera id meaning "bind to whatever external (USB) camera is
         * present, by lens-facing." More robust than a specific Camera2 id:
         * external cameras enumerate unreliably in cameraIdList, but CameraX can
         * still select them by LENS_FACING_EXTERNAL.
         */
        const val EXTERNAL_CAMERA_ID = "external"
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
                val isVideo = device != null && (0 until device.interfaceCount).any {
                    device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_VIDEO
                }
                if (!isVideo) return // DSLR (PTP), printers, hubs: not our camera
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
                        val bound = _bindState.value as? CameraBindState.Bound
                        val wantsExternal = lastRequest?.cameraId?.isNotEmpty() == true
                        if (wantsExternal && bound?.matchedRequest != true) {
                            // The camera HAL needs a moment to enumerate the
                            // new device; the backoff covers a slow one.
                            Log.i(TAG, "Video device attached — trying to get the selected camera back")
                            recoveryAttempts = 0
                            scheduleRecovery()
                        }
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
        val externalSelected = cameraId == EXTERNAL_CAMERA_ID ||
            (cameraId.isNotEmpty() && detectCameras().any { it.id == cameraId && it.isExternal })
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
        useFrontCamera = useFront
        mirrorImage = mirror
        selectedCameraId = cameraId
        _bindState.value = CameraBindState.Idle
        mainHandler.removeCallbacks(recoveryRunnable)
        recoveryAttempts = 0 // rebindLast() restores its own count after this
        lastRequest = BindRequest(
            WeakReference(lifecycleOwner), WeakReference(previewView),
            useFront, cameraId, mirror, maxResolution
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val captureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

                // Apply resolution constraint if not max
                if (maxResolution < Int.MAX_VALUE) {
                    val resSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(maxResolution, maxResolution * 3 / 4),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                    captureBuilder.setResolutionSelector(resSelector)
                }

                val capture = captureBuilder.build()
                capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0

                imageCapture = capture

                val cameraSelector = buildCameraSelector(provider, cameraId, useFront)

                provider.unbindAll()
                val bound = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                camera = bound
                watchCameraState(bound)
                _bindState.value = describeBound(bound, cameraId)

                Log.i(TAG, "Camera bound: ${_bindState.value} (requested front=$useFront id=$cameraId)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera, trying fallback", e)
                tryFallbackCamera(cameraProviderFuture.get(), lifecycleOwner, previewView)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Build a CameraSelector. If [cameraId] is set, bind to that specific Camera2 ID
     * (handles external/USB cameras). Otherwise default to front/back per [useFront].
     */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun buildCameraSelector(
        provider: ProcessCameraProvider,
        cameraId: String,
        useFront: Boolean
    ): CameraSelector {
        // "Use external camera" — bind to any external camera by lens-facing.
        if (cameraId == EXTERNAL_CAMERA_ID) {
            val external = externalCameraSelector()
            try {
                if (provider.hasCamera(external)) return external
            } catch (e: Exception) {
                Log.w(TAG, "External camera not bindable yet, falling back", e)
            }
            // Fall through to a built-in default if no external is ready.
            return if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        }

        if (cameraId.isNotEmpty()) {
            try {
                val selector = CameraSelector.Builder()
                    .addCameraFilter { cameraInfos ->
                        cameraInfos.filter { info ->
                            Camera2CameraInfo.from(info).cameraId == cameraId
                        }
                    }
                    .build()
                if (provider.hasCamera(selector)) {
                    return selector
                }
            } catch (e: Exception) {
                Log.w(TAG, "Specific camera ID $cameraId not available, falling back", e)
            }
        }

        return if (useFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    /** A selector matching any LENS_FACING_EXTERNAL camera. */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun externalCameraSelector(): CameraSelector =
        CameraSelector.Builder()
            .addCameraFilter { infos ->
                infos.filter { info ->
                    try {
                        Camera2CameraInfo.from(info)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
                            CameraCharacteristics.LENS_FACING_EXTERNAL
                    } catch (e: Exception) { false }
                }
            }
            .build()

    /**
     * If the primary bind failed, walk the fallback chain:
     * 1) the opposite built-in (back if we tried front, etc.)
     * 2) any external camera that's available
     */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun tryFallbackCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val candidates = buildList {
            add(if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA)
            // Any LENS_FACING_EXTERNAL camera.
            add(externalCameraSelector())
        }

        for (selector in candidates) {
            try {
                if (!provider.hasCamera(selector)) continue
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                camera = bound
                watchCameraState(bound)
                _bindState.value = describeBound(bound, selectedCameraId)
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
        if (error.type == CameraState.ErrorType.CRITICAL) {
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
     * external, otherwise a specific Camera2 id).
     */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun describeBound(bound: Camera, requestedId: String): CameraBindState.Bound {
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
            requestedId == EXTERNAL_CAMERA_ID -> isExternal
            requestedId.isNotEmpty() -> info.cameraId == requestedId
            else -> true
        }
        return CameraBindState.Bound(
            cameraId = info.cameraId,
            facingLabel = facingLabel,
            isExternal = isExternal,
            matchedRequest = matched
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
        val photo = withTimeoutOrNull(timeoutMs) { captureStill(maxDim) }
        if (photo != null) {
            recoveryAttempts = 0 // healthy again — future failures get a full backoff
            return photo
        }
        Log.e(TAG, "Capture timed out after ${timeoutMs}ms — re-opening the camera")
        mainHandler.post {
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
        var sample = 1
        var longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        while (longEdge > maxDim) {
            sample *= 2
            longEdge /= 2
        }

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
        lastRequest = null
        observedCameraState?.removeObserver(cameraStateObserver)
        observedCameraState = null
        cameraProvider?.unbindAll()
        camera = null
        _bindState.value = CameraBindState.Idle
    }
}
