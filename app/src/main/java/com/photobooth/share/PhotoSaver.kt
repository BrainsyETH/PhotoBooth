package com.photobooth.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoSaver @Inject constructor() {

    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String = "PhotoBooth_${System.currentTimeMillis()}"): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, fileName)
        } else {
            saveToExternalStorage(context, bitmap, fileName)
        }
    }

    private fun saveWithMediaStore(context: Context, bitmap: Bitmap, fileName: String): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoBooth")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            uri.toString()
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToExternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "PhotoBooth"
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$fileName.jpg")
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun saveToCacheForSharing(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "share_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
        return file
    }
}
