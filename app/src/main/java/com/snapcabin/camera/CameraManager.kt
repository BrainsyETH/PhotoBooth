package com.snapcabin.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
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
    private var mirrorImage: Boolean = true
    private var selectedCameraId: String = ""

    private val _bindState = MutableStateFlow<CameraBindState>(CameraBindState.Idle)
    /** What's live right now — admin surfaces this under TEST CAMERA. */
    val bindState: StateFlow<CameraBindState> = _bindState.asStateFlow()

    companion object {
        private const val TAG = "CameraManager"

        /**
         * Sentinel camera id meaning "bind to whatever external (USB) camera is
         * present, by lens-facing." More robust than a specific Camera2 id:
         * external cameras enumerate unreliably in cameraIdList, but CameraX can
         * still select them by LENS_FACING_EXTERNAL.
         */
        const val EXTERNAL_CAMERA_ID = "external"
    }

    /**
     * Detect all available cameras including USB-connected external cameras
     * (Nikon, Canon, etc. that present as UVC devices)
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

    suspend fun takePhoto(): Bitmap = suspendCancellableCoroutine { continuation ->
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
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Decode mutable so downstream branding/watermark stamps can
                    // write into the same buffer instead of allocating a second
                    // full-resolution copy. Without this, BitmapFactory returns
                    // an immutable bitmap and the share pipeline silently falls
                    // back to the unbranded photo on memory pressure.
                    val decodeOpts = BitmapFactory.Options().apply {
                        inMutable = true
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOpts)
                    if (rawBitmap == null) {
                        photoFile.delete()
                        continuation.resumeWithException(IllegalStateException("Failed to decode photo"))
                        return
                    }

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

                    // Transform onto a mutable target ourselves. Despite docs
                    // suggesting otherwise, Bitmap.createBitmap(src, ..., matrix)
                    // returns an IMMUTABLE bitmap — which silently defeats the
                    // in-place branding pipeline downstream (mirroring is on by
                    // default, so every capture hit this) and forces a second
                    // full-resolution copy in CustomBrandingRenderer.
                    val finalBitmap = if (!matrix.isIdentity) {
                        val src = RectF(0f, 0f, rawBitmap.width.toFloat(), rawBitmap.height.toFloat())
                        val dst = RectF()
                        matrix.mapRect(dst, src)
                        val out = Bitmap.createBitmap(
                            dst.width().roundToInt().coerceAtLeast(1),
                            dst.height().roundToInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(out)
                        canvas.translate(-dst.left, -dst.top)
                        canvas.drawBitmap(
                            rawBitmap,
                            matrix,
                            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        )
                        rawBitmap.recycle()
                        out
                    } else {
                        rawBitmap
                    }

                    photoFile.delete()
                    continuation.resume(finalBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    photoFile.delete()
                    Log.e(TAG, "Photo capture failed", exception)
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun release() {
        cameraProvider?.unbindAll()
        camera = null
        _bindState.value = CameraBindState.Idle
    }
}
