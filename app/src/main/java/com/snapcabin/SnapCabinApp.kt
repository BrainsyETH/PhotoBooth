package com.snapcabin

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.snapcabin.analytics.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.system.exitProcess

@HiltAndroidApp
class SnapCabinApp : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CrashReporterEntryPoint {
        fun crashReporter(): CrashReporter
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    /**
     * Kiosk crash strategy: when something throws an unhandled exception,
     * we want the booth back up as fast as possible for the next guest.
     *
     * Two important guardrails on top of the original "log, schedule
     * restart, exitProcess" path:
     *
     * 1. Death-loop protection. If we've restarted twice in the last 60
     *    seconds, stop trying — something is reproducibly broken and
     *    looping silently makes it harder to diagnose. We let the default
     *    handler take over so the operator sees the failure.
     *
     * 2. Inexact alarm. AlarmManager.setExact() needs SCHEDULE_EXACT_ALARM
     *    on Android 12+ and we don't request it. set() doesn't need any
     *    permission and is plenty fast for a 1-second restart.
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this, CrashReporterEntryPoint::class.java
                )
                entryPoint.crashReporter().logException(
                    throwable,
                    "Uncaught exception — considering restart"
                )

                if (isInRestartLoop()) {
                    // Hand off to the default handler so the failure is
                    // visible (ANR dialog / Play Store crash report path).
                    defaultHandler?.uncaughtException(thread, throwable)
                    return@setDefaultUncaughtExceptionHandler
                }

                recordRestartAttempt()
                scheduleRestart()
            } catch (e: Exception) {
                // If crash reporting itself fails, fall through to default
            }

            exitProcess(1)
        }
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact set() is fine for a ~1s booth restart and avoids the
        // SCHEDULE_EXACT_ALARM permission required by setExact() on
        // Android 12+.
        alarmManager.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }

    private fun isInRestartLoop(): Boolean {
        val prefs = getSharedPreferences(RESTART_PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_RESTART, 0L)
        val count = prefs.getInt(KEY_RESTART_COUNT, 0)
        val now = System.currentTimeMillis()
        return now - last < RESTART_WINDOW_MS && count >= MAX_RESTARTS_IN_WINDOW
    }

    private fun recordRestartAttempt() {
        val prefs = getSharedPreferences(RESTART_PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_RESTART, 0L)
        val now = System.currentTimeMillis()
        val countSoFar = if (now - last < RESTART_WINDOW_MS) {
            prefs.getInt(KEY_RESTART_COUNT, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putLong(KEY_LAST_RESTART, now)
            .putInt(KEY_RESTART_COUNT, countSoFar)
            .apply()
    }

    companion object {
        private const val RESTART_PREFS = "snapcabin_restart_guard"
        private const val KEY_LAST_RESTART = "last_restart_ms"
        private const val KEY_RESTART_COUNT = "restart_count_in_window"
        private const val RESTART_WINDOW_MS = 60_000L
        private const val MAX_RESTARTS_IN_WINDOW = 2
    }
}
