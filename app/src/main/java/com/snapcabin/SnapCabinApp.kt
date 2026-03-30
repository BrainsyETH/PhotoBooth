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

    private fun setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                // Log the crash
                val entryPoint = EntryPointAccessors.fromApplication(
                    this, CrashReporterEntryPoint::class.java
                )
                entryPoint.crashReporter().logException(throwable, "Uncaught exception — scheduling restart")

                // Schedule a restart in 1 second
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
                alarmManager.setExact(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            } catch (e: Exception) {
                // If crash reporting itself fails, fall through to default handler
            }

            // Kill the process
            exitProcess(1)
        }
    }
}
