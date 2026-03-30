package com.snapcabin.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor() {

    fun enterKioskMode(activity: Activity) {
        keepScreenOn(activity, true)
        configureDeviceOwner(activity)
        lockTask(activity, true)
    }

    fun exitKioskMode(activity: Activity) {
        lockTask(activity, false)
        keepScreenOn(activity, false)
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

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Configures Device Owner policies when the app is set as Device Owner.
     * This enables full kiosk lockdown: no status bar, no nav bar, no home button,
     * no recent apps, disabled keyguard, and restricted user actions.
     */
    private fun configureDeviceOwner(activity: Activity) {
        if (!isDeviceOwner(activity)) return

        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(activity, DeviceAdminReceiver::class.java)

        try {
            // Whitelist this package for Lock Task Mode
            dpm.setLockTaskPackages(adminComponent, arrayOf(activity.packageName))

            // Disable all system UI in Lock Task Mode
            dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)

            // Disable the keyguard (lock screen)
            dpm.setKeyguardDisabled(adminComponent, true)

            // Keep screen on while plugged in (AC, USB, wireless)
            Settings.Global.putInt(
                activity.contentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                android.os.BatteryManager.BATTERY_PLUGGED_AC or
                    android.os.BatteryManager.BATTERY_PLUGGED_USB or
                    android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS
            )

            // Apply user restrictions
            val restrictions = mutableListOf(
                UserManager.DISALLOW_SAFE_BOOT,
                UserManager.DISALLOW_FACTORY_RESET,
                UserManager.DISALLOW_ADD_USER,
                UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                restrictions.add(UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
            }

            for (restriction in restrictions) {
                dpm.addUserRestriction(adminComponent, restriction)
            }
        } catch (e: Exception) {
            // Device Owner configuration failed — continue with basic kiosk mode
        }
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
