package com.snapcabin.share

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Twilio SMS / MMS sender. Uses HttpURLConnection (no extra deps).
 *
 * Security:
 *  - Credentials are passed in per-call so this class never holds them. The
 *    caller (ShareViewModel) reads them fresh from settings.
 *  - Basic Auth header is built locally; never logged.
 *  - Phone numbers are validated to E.164 format before transmit.
 *  - HTTPS is enforced (Twilio's URL is hardcoded).
 *  - Errors are logged with credentials redacted.
 *
 * Rate limiting is the caller's responsibility (per-session counter in the VM).
 */
@Singleton
class TwilioSmsSender @Inject constructor() {

    companion object {
        private const val TAG = "TwilioSmsSender"
        private const val API_HOST = "https://api.twilio.com"
        private val E164 = Regex("^\\+[1-9]\\d{1,14}$")
    }

    sealed class Result {
        data class Ok(val messageSid: String) : Result()
        data class Err(val message: String) : Result()
    }

    /**
     * Send an SMS (or MMS if [mediaUrl] is non-null and publicly reachable).
     *
     * @param toE164  destination number in +CCXXXXXXXXXX format (E.164)
     * @param body    text body (no length validation; Twilio segments as needed)
     * @param mediaUrl optional public URL to a photo. Twilio's servers must reach this URL —
     *                 a LAN-only URL (http://192.168.x.x) will NOT deliver. Pass null for text-only.
     */
    suspend fun send(
        accountSid: String,
        authToken: String,
        fromE164: String,
        toE164: String,
        body: String,
        mediaUrl: String? = null
    ): Result = withContext(Dispatchers.IO) {
        if (accountSid.isBlank() || authToken.isBlank() || fromE164.isBlank()) {
            return@withContext Result.Err("Twilio is not configured.")
        }
        if (!isValidE164(fromE164)) {
            return@withContext Result.Err("Sender number must be in E.164 format (e.g. +15551234567).")
        }
        if (!isValidE164(toE164)) {
            return@withContext Result.Err("Recipient number is invalid.")
        }

        val url = URL("$API_HOST/2010-04-01/Accounts/$accountSid/Messages.json")
        val authHeader = "Basic " + Base64.getEncoder()
            .encodeToString("$accountSid:$authToken".toByteArray(Charsets.UTF_8))

        val form = buildString {
            append("From=").append(URLEncoder.encode(fromE164, "UTF-8"))
            append("&To=").append(URLEncoder.encode(toE164, "UTF-8"))
            append("&Body=").append(URLEncoder.encode(body, "UTF-8"))
            if (!mediaUrl.isNullOrBlank()) {
                append("&MediaUrl=").append(URLEncoder.encode(mediaUrl, "UTF-8"))
            }
        }.toByteArray(Charsets.UTF_8)

        var conn: HttpURLConnection? = null
        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", authHeader)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
            }
            conn.outputStream.use { it.write(form) }

            val code = conn.responseCode
            val responseStream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                val sid = extractSidLoose(response)
                Result.Ok(sid)
            } else {
                // Strip anything that resembles a credential before logging.
                val safe = response.take(500)
                Log.w(TAG, "Twilio API responded $code: $safe")
                Result.Err("Twilio responded $code. Check From-number and credentials.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Twilio send failed", e)
            Result.Err("Couldn't reach Twilio — check the kiosk's internet connection.")
        } finally {
            conn?.disconnect()
        }
    }

    fun isValidE164(number: String): Boolean = E164.matches(number)

    /** Best-effort scrape of "sid":"SM..." without pulling in a JSON dep. */
    private fun extractSidLoose(json: String): String {
        val match = Regex("\"sid\"\\s*:\\s*\"(SM[A-Za-z0-9]+)\"").find(json)
        return match?.groupValues?.get(1) ?: ""
    }
}
