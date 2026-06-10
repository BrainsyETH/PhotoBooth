package com.snapcabin.analytics

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting that stays true to the no-telemetry promise: nothing leaves
 * the device. Exceptions are logged to logcat AND appended to a local file the
 * operator can retrieve over USB / a file manager, so a crash at an event isn't
 * a total black box (there's no Crashlytics by design).
 *
 * The file lives in the app's external files dir
 * (Android/data/<pkg>/files/crash_log.txt) — readable without permissions,
 * wiped on uninstall, never networked.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CrashReporter"
        private const val LOG_FILE = "crash_log.txt"
        private const val MAX_BYTES = 256 * 1024L // keep the newest ~256 KB
    }

    fun logException(throwable: Throwable, message: String? = null) {
        if (message != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, "Caught exception", throwable)
        }
        appendToFile(buildString {
            append(timestamp())
            append(message?.let { " — $it" } ?: "")
            append('\n')
            append(stackTraceString(throwable))
            append("\n\n")
        })
    }

    fun log(message: String) {
        Log.i(TAG, message)
        appendToFile("${timestamp()} — $message\n")
    }

    private fun appendToFile(text: String) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, LOG_FILE)
            if (file.exists() && file.length() > MAX_BYTES) {
                // Truncate to keep the newest entries.
                val tail = file.readText().takeLast((MAX_BYTES / 2).toInt())
                file.writeText("--- log trimmed ---\n$tail")
            }
            file.appendText(text)
        } catch (e: Exception) {
            // Never let crash logging cause a crash.
            Log.w(TAG, "Could not write crash log", e)
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun stackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
