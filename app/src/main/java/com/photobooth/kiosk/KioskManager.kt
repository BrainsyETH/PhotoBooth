package com.photobooth.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor() {

    fun enterKioskMode(activity: Activity) {
        keepScreenOn(activity, true)
        lockTask(activity, true)
    }

    fun exitKioskMode(activity: Activity) {
        keepScreenOn(activity, false)
        lockTask(activity, false)
    }

    fun keepScreenOn(activity: Activity, enable: Boolean) {
        if (enable) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun setScreenBrightness(activity: Activity, brightness: Float) {
        val params = activity.window.attributes
        params.screenBrightness = brightness.coerceIn(0.01f, 1f)
        activity.window.attributes = params
    }

    private fun lockTask(activity: Activity, lock: Boolean) {
        try {
            if (lock) {
                if (!isInLockTaskMode(activity)) {
                    activity.startLockTask()
                }
            } else {
                if (isInLockTaskMode(activity)) {
                    activity.stopLockTask()
                }
            }
        } catch (e: Exception) {
            // Lock task requires device owner or allowlisted app
            // Silently fail on non-managed devices
        }
    }

    private fun isInLockTaskMode(activity: Activity): Boolean {
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}
