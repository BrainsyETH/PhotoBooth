package com.snapcabin

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

    private fun setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                // Log the crash
                val entryPoint = EntryPointAccessors.fromApplication(
                    this, CrashReporterEntryPoint::class.java
                )
                entryPoint.crashReporter().logException(throwable, "Uncaught exception — scheduling restart")

                if (isCrashLooping()) {
                    // Crashing over and over (e.g. right at startup): stop
                    // relaunching so the operator can get into the tablet.
                    entryPoint.crashReporter().log("Crash loop detected — NOT scheduling a restart")
                } else {
                    scheduleRestart()
                }
            } catch (e: Exception) {
                // If crash reporting itself fails, fall through to default handler
            }

            // Kill the process
            exitProcess(1)
        }
    }

    /**
     * Relaunch the booth ~1 s after a crash. setExact() needs the "Alarms &
     * reminders" special access on Android 12+ (targetSdk 31+), which this app
     * doesn't hold — it threw SecurityException, the catch swallowed it, and
     * NO restart was ever scheduled. Fall back to an inexact alarm, which the
     * system still delivers promptly while the screen is on.
     *
     * Android 10+ may still block an activity launch from the background for a
     * normal app; a Device Owner (full kiosk setup) is exempt. That is one more
     * reason Device Owner is the recommended setup for unattended events.
     */
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
        val at = System.currentTimeMillis() + 1000
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExact(AlarmManager.RTC, at, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC, at, pendingIntent)
        }
    }

    /** True if this is the 3rd crash within 2 minutes. Records this crash either way. */
    private fun isCrashLooping(): Boolean {
        val prefs = getSharedPreferences("crash_restart", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val recent = (prefs.getString("times", "") ?: "")
            .split(',')
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < CRASH_LOOP_WINDOW_MS }
        val updated = recent + now
        // commit(), not apply(): the process is about to be killed.
        prefs.edit().putString("times", updated.takeLast(5).joinToString(",")).commit()
        return updated.size >= CRASH_LOOP_LIMIT
    }

    private companion object {
        const val CRASH_LOOP_WINDOW_MS = 2 * 60_000L
        const val CRASH_LOOP_LIMIT = 3
    }
}
