package com.snapcabin.share

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the captured photo to a guest via the Resend HTTP API
 * (https://resend.com/docs/api-reference/emails/send-email). The photo rides as
 * a base64 JPEG attachment so the email is self-contained — no Cloudinary
 * dependency for delivery.
 *
 * Security:
 *  - The API key is passed in per-call. The class never holds it.
 *  - HTTPS-only (api.resend.com is hardcoded).
 *  - Recipient address is validated before transmit.
 *  - Bearer token is never logged.
 *
 * Rate limiting is the caller's responsibility (per-session + per-address
 * counters in the ShareViewModel).
 */
@Singleton
class ResendEmailSender @Inject constructor() {

    companion object {
        private const val TAG = "ResendEmailSender"
        private const val API_URL = "https://api.resend.com/emails"
        private const val JPEG_QUALITY = 90
        // RFC-5322-lite. Good enough to reject obvious typos before paying for
        // the HTTPS round trip; the API itself is the real validator.
        private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    sealed class Result {
        data class Ok(val emailId: String) : Result()
        data class Err(val message: String, val isQuotaError: Boolean = false) : Result()
    }

    /**
     * Send an email with the photo attached as a JPEG.
     *
     * @param apiKey         Resend API key (re_...)
     * @param fromAddress    e.g. "SnapCabin <booth@yourdomain.com>"
     * @param replyToAddress optional Reply-To so guest replies land in the host's inbox
     * @param toAddress      recipient
     * @param subject        subject line
     * @param htmlBody       HTML body of the email
     * @param photo          the bitmap to compress and attach
     */
    suspend fun send(
        apiKey: String,
        fromAddress: String,
        replyToAddress: String,
        toAddress: String,
        subject: String,
        htmlBody: String,
        photo: Bitmap
    ): Result {
        val jpegBytes = ByteArrayOutputStream().use { out ->
            photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        return send(
            apiKey = apiKey,
            fromAddress = fromAddress,
            replyToAddress = replyToAddress,
            toAddress = toAddress,
            subject = subject,
            htmlBody = htmlBody,
            attachmentBytes = jpegBytes,
            attachmentFilename = "photo.jpg",
            attachmentContentType = "image/jpeg"
        )
    }

    /**
     * Sends an email with an arbitrary already-encoded image attachment (JPEG
     * still or animated GIF). The Bitmap overload above funnels into this.
     */
    suspend fun send(
        apiKey: String,
        fromAddress: String,
        replyToAddress: String,
        toAddress: String,
        subject: String,
        htmlBody: String,
        attachmentBytes: ByteArray,
        attachmentFilename: String,
        attachmentContentType: String
    ): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || fromAddress.isBlank()) {
            return@withContext Result.Err("Resend isn't configured.")
        }
        if (!isValidEmail(toAddress)) {
            return@withContext Result.Err("That email address doesn't look right.")
        }

        val base64 = Base64.getEncoder().encodeToString(attachmentBytes)

        val payload = JSONObject().apply {
            put("from", fromAddress)
            put("to", JSONArray().apply { put(toAddress) })
            put("subject", subject)
            put("html", htmlBody)
            if (replyToAddress.isNotBlank() && isValidEmail(replyToAddress)) {
                put("reply_to", replyToAddress)
            }
            put("attachments", JSONArray().apply {
                put(JSONObject().apply {
                    put("filename", attachmentFilename)
                    put("content", base64)
                    put("content_type", attachmentContentType)
                })
            })
        }.toString().toByteArray(Charsets.UTF_8)

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10_000
                readTimeout = 30_000
                doOutput = true
            }
            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            val responseStream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                val id = extractField(response, "id")
                Result.Ok(id)
            } else {
                Log.w(TAG, "Resend API responded $code: ${response.take(500)}")
                val (human, isQuota) = humanizeError(code, response)
                Result.Err(human, isQuotaError = isQuota)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Resend send failed", e)
            Result.Err("Couldn't reach Resend — check the kiosk's internet connection.")
        } finally {
            conn?.disconnect()
        }
    }

    fun isValidEmail(address: String): Boolean = EMAIL.matches(address.trim())

    private fun extractField(json: String, field: String): String {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    /** Returns (human message, true if quota/rate-limit-related). */
    private fun humanizeError(code: Int, response: String): Pair<String, Boolean> = when (code) {
        401, 403 -> "Resend rejected the API key." to false
        422 -> {
            // Resend's most common 422 is a from-address that isn't on a
            // verified domain. Surface the API's own message so the operator
            // can act on it.
            val msg = extractField(response, "message").ifBlank { "Resend rejected the request." }
            msg to false
        }
        429 -> "Email quota hit. The Resend free tier caps at 100/day." to true
        in 500..599 -> "Resend is having trouble. Try again in a moment." to false
        else -> "Resend responded $code. Check the From address and API key." to false
    }
}
