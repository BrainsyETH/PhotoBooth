package com.snapcabin.share

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads a photo to Cloudinary via the unsigned-upload endpoint and returns
 * the resulting public URL. Used so the share screen can render a QR code
 * pointing at the hosted photo (and optionally include the link in the
 * Resend email body).
 *
 * Security:
 *  - "Unsigned" means no API secret is shipped in the app. The cloud_name +
 *    upload_preset are the only credentials; mitigations belong on Cloudinary's
 *    side (preset configured as Unsigned, restricted to image formats, max file size,
 *    folder, allowed formats, etc.).
 *  - HTTPS-only (api.cloudinary.com is hardcoded).
 *  - No credential bytes are logged.
 */
@Singleton
class CloudinaryUploader @Inject constructor() {

    companion object {
        private const val TAG = "CloudinaryUploader"
        private const val API_HOST = "https://api.cloudinary.com"
        private const val JPEG_QUALITY = 90
    }

    sealed class Result {
        data class Ok(val secureUrl: String) : Result()
        data class Err(val message: String) : Result()
    }

    suspend fun upload(
        cloudName: String,
        uploadPreset: String,
        bitmap: Bitmap,
        folder: String? = null
    ): Result {
        val jpegBytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        return uploadBytes(
            cloudName = cloudName,
            uploadPreset = uploadPreset,
            fileBytes = jpegBytes,
            filename = "photo.jpg",
            contentType = "image/jpeg",
            folder = folder
        )
    }

    /**
     * Uploads already-encoded image bytes (JPEG or animated GIF) to the same
     * unsigned /image/upload endpoint. Cloudinary serves GIFs back as animated
     * GIFs, so the QR/email link the booth hands out keeps moving.
     */
    suspend fun uploadBytes(
        cloudName: String,
        uploadPreset: String,
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
        folder: String? = null
    ): Result = withContext(Dispatchers.IO) {
        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            return@withContext Result.Err("Cloudinary isn't configured.")
        }

        val boundary = "----snapcabin${System.currentTimeMillis()}"
        val url = URL("$API_HOST/v1_1/$cloudName/image/upload")

        var conn: HttpURLConnection? = null
        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10_000
                readTimeout = 30_000
                useCaches = false
            }

            DataOutputStream(conn.outputStream).use { out ->
                writeFormField(out, boundary, "upload_preset", uploadPreset)
                if (!folder.isNullOrBlank()) {
                    writeFormField(out, boundary, "folder", folder)
                }
                writeFileField(out, boundary, "file", filename, contentType, fileBytes)
                out.writeBytes("--$boundary--\r\n")
                out.flush()
            }

            val code = conn.responseCode
            val responseStream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                val secureUrl = extractField(response, "secure_url")
                if (secureUrl.isNotEmpty()) {
                    Result.Ok(secureUrl)
                } else {
                    Log.w(TAG, "Cloudinary 200 OK but no secure_url in payload")
                    Result.Err("Upload succeeded but no URL returned.")
                }
            } else {
                Log.w(TAG, "Cloudinary responded $code: ${response.take(500)}")
                Result.Err("Cloudinary responded $code. Check cloud name and preset.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload failed", e)
            Result.Err("Couldn't reach Cloudinary — check the kiosk's internet connection.")
        } finally {
            conn?.disconnect()
        }
    }

    private fun writeFormField(
        out: DataOutputStream,
        boundary: String,
        name: String,
        value: String
    ) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.write(value.toByteArray(Charsets.UTF_8))
        out.writeBytes("\r\n")
    }

    private fun writeFileField(
        out: DataOutputStream,
        boundary: String,
        name: String,
        filename: String,
        contentType: String,
        bytes: ByteArray
    ) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n")
        out.writeBytes("Content-Type: $contentType\r\n\r\n")
        out.write(bytes)
        out.writeBytes("\r\n")
    }

    /** Loose JSON field extractor for "field":"value" without pulling in a JSON dep. */
    private fun extractField(json: String, field: String): String {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1)?.replace("\\/", "/") ?: ""
    }
}
