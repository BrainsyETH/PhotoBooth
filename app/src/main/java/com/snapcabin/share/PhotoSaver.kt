package com.snapcabin.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoSaver @Inject constructor() {

    companion object {
        private const val TAG = "PhotoSaver"
    }

    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "SnapCabin_${System.currentTimeMillis()}",
        quality: Int = 95
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, fileName, quality)
        } else {
            saveToExternalStorage(context, bitmap, fileName, quality)
        }
    }

    private fun saveWithMediaStore(context: Context, bitmap: Bitmap, fileName: String, quality: Int): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SnapCabin")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
            }
            uri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save via MediaStore", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToExternalStorage(context: Context, bitmap: Bitmap, fileName: String, quality: Int): String? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SnapCabin"
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$fileName.jpg")
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to external storage", e)
            null
        }
    }

    fun saveToCacheForSharing(context: Context, bitmap: Bitmap, quality: Int = 95): File {
        val file = File(context.cacheDir, "share_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        }
        return file
    }
}
