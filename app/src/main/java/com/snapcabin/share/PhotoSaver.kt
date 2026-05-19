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
        // On Q+ we use IS_PENDING so the entry isn't visible to gallery apps
        // while we're still writing bytes. Some OEM gallery apps refuse to
        // show partially-written entries; clearing IS_PENDING after the write
        // also kicks off MediaStore indexing.
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SnapCabin")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Log.e(TAG, "MediaStore.insert returned null. RELATIVE_PATH=${contentValues.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)}")
            return null
        }

        return try {
            resolver.openOutputStream(uri).use { stream ->
                if (stream == null) {
                    Log.e(TAG, "openOutputStream returned null for $uri")
                    resolver.delete(uri, null, null)
                    return null
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
                stream.flush()
            }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            Log.i(TAG, "Saved photo to $uri")
            uri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save via MediaStore", e)
            try { resolver.delete(uri, null, null) } catch (_: Exception) { }
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
