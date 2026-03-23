package com.photobooth.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var useFrontCamera: Boolean = true

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFront: Boolean = true
    ) {
        useFrontCamera = useFront
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))
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
                    val finalBitmap = if (useFrontCamera) {
                        // Mirror the front camera image horizontally
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
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun release() {
        cameraProvider?.unbindAll()
    }
}
