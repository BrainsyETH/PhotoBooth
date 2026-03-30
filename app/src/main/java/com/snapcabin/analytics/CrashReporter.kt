package com.snapcabin.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting abstraction.
 *
 * To enable Firebase Crashlytics:
 * 1. Add Firebase BOM + Crashlytics dependencies
 * 2. Replace implementation with FirebaseCrashlytics calls
 */
@Singleton
class CrashReporter @Inject constructor() {

    companion object {
        private const val TAG = "CrashReporter"
    }

    fun logException(throwable: Throwable, message: String? = null) {
        if (message != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, "Caught exception", throwable)
        }

        // Uncomment when Firebase Crashlytics is integrated:
        // FirebaseCrashlytics.getInstance().apply {
        //     message?.let { log(it) }
        //     recordException(throwable)
        // }
    }

    fun log(message: String) {
        Log.i(TAG, message)

        // Uncomment when Firebase Crashlytics is integrated:
        // FirebaseCrashlytics.getInstance().log(message)
    }

    fun setUserId(userId: String) {
        // Uncomment when Firebase Crashlytics is integrated:
        // FirebaseCrashlytics.getInstance().setUserId(userId)
    }
}
