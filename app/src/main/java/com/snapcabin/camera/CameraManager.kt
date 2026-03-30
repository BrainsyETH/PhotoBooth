package com.snapcabin.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

                // Set target rotation to match the display so output is landscape-correct
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rotation = windowManager.defaultDisplay.rotation
                capture.targetRotation = rotation

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
     * Build a CameraSelector — tries specific camera ID first, then falls back to front/back
     */
    private fun buildCameraSelector(
        provider: ProcessCameraProvider,
        cameraId: String,
        useFront: Boolean
    ): CameraSelector {
        // If a specific camera ID is provided, use it
        if (cameraId.isNotEmpty()) {
            try {
                val selector = CameraSelector.Builder()
                    .addCameraFilter { cameraInfos ->
                        cameraInfos.filter { info ->
                            // Match by camera ID through CameraX info
                            val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
                            systemCameraManager.cameraIdList.any { id -> id == cameraId }
                        }
                    }
                    .build()

                // Verify this selector has available cameras
                if (provider.hasCamera(selector)) {
                    return selector
                }
            } catch (e: Exception) {
                Log.w(TAG, "Specific camera ID $cameraId not available, falling back", e)
            }
        }

        // Standard front/back selector
        return if (useFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    /**
     * If primary camera fails, try any available camera
     */
    private fun tryFallbackCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        try {
            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Try back camera if front failed, or vice versa
            val fallbackSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                fallbackSelector,
                preview,
                imageCapture
            )
            // Update state so mirroring is correct
            useFrontCamera = !useFrontCamera
            Log.i(TAG, "Fallback camera bound. Front=$useFrontCamera")
        } catch (e: Exception) {
            Log.e(TAG, "No cameras available", e)
        }
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
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap == null) {
                        photoFile.delete()
                        continuation.resumeWithException(IllegalStateException("Failed to decode photo"))
                        return
                    }
                    val finalBitmap = if (useFrontCamera && mirrorImage) {
                        val matrix = Matrix().apply { preScale(-1f, 1f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
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
