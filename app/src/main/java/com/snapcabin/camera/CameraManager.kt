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

    companion object {
        private const val TAG = "CameraManager"
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
     * Used to decide whether to request RECORD_AUDIO before opening it — UVC
     * cameras are composite audio+video devices the camera service won't open
     * without that permission.
     */
    fun hasExternalCamera(): Boolean = detectCameras().any { it.isExternal }

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
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                Log.i(TAG, "Camera bound successfully. Front=$useFront, CameraId=$cameraId")
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
            // External camera selector — matches any LENS_FACING_EXTERNAL info
            add(
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
            )
        }

        for (selector in candidates) {
            try {
                if (!provider.hasCamera(selector)) continue
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                Log.i(TAG, "Fallback camera bound via $selector")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Fallback candidate failed", e)
            }
        }
        Log.e(TAG, "No usable cameras available")
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
    }
}
