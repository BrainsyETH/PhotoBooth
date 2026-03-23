package com.photobooth.analytics

import android.os.Bundle
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics abstraction layer. Currently logs locally.
 *
 * To enable Firebase Analytics:
 * 1. Add google-services.json to app/
 * 2. Add Firebase BOM + Analytics dependencies to build.gradle.kts
 * 3. Add 'id("com.google.gms.google-services")' plugin
 * 4. Replace LogAnalyticsBackend with FirebaseAnalyticsBackend
 *
 * To enable Firebase Crashlytics:
 * 1. Add Crashlytics dependency
 * 2. Add 'id("com.google.firebase.crashlytics")' plugin
 * 3. Uncomment crash reporting in CrashReporter
 */
@Singleton
class AnalyticsTracker @Inject constructor() {

    companion object {
        private const val TAG = "Analytics"
    }

    private val backend: AnalyticsBackend = LogAnalyticsBackend()

    // --- Photo Events ---

    fun trackPhotoTaken(mode: String) {
        backend.logEvent("photo_taken", bundleOf("mode" to mode))
    }

    fun trackPhotoSaved() {
        backend.logEvent("photo_saved", null)
    }

    fun trackPhotoShared(method: String) {
        backend.logEvent("photo_shared", bundleOf("method" to method))
    }

    fun trackPhotoPrinted() {
        backend.logEvent("photo_printed", null)
    }

    // --- Mode Events ---

    fun trackModeSelected(mode: String) {
        backend.logEvent("mode_selected", bundleOf("mode" to mode))
    }

    fun trackFilterApplied(filterName: String) {
        backend.logEvent("filter_applied", bundleOf("filter" to filterName))
    }

    fun trackOverlayApplied(overlayName: String) {
        backend.logEvent("overlay_applied", bundleOf("overlay" to overlayName))
    }

    fun trackBrandingApplied(template: String) {
        backend.logEvent("branding_applied", bundleOf("template" to template))
    }

    // --- Collage Events ---

    fun trackCollageCreated(layout: String, photoCount: Int) {
        backend.logEvent("collage_created", bundleOf(
            "layout" to layout,
            "photo_count" to photoCount.toString()
        ))
    }

    // --- GIF Events ---

    fun trackGifCreated(frameCount: Int) {
        backend.logEvent("gif_created", bundleOf("frame_count" to frameCount.toString()))
    }

    // --- Session Events ---

    fun trackSessionStart() {
        backend.logEvent("session_start", null)
    }

    fun trackInactivityTimeout() {
        backend.logEvent("inactivity_timeout", null)
    }

    // --- Helper ---

    private fun bundleOf(vararg pairs: Pair<String, String>): Bundle {
        return Bundle().apply {
            pairs.forEach { (key, value) -> putString(key, value) }
        }
    }
}

interface AnalyticsBackend {
    fun logEvent(name: String, params: Bundle?)
}

/**
 * Default backend that logs to Logcat. Replace with Firebase in production.
 */
class LogAnalyticsBackend : AnalyticsBackend {
    override fun logEvent(name: String, params: Bundle?) {
        val paramsStr = params?.keySet()?.joinToString(", ") { "$it=${params.getString(it)}" } ?: ""
        Log.i("Analytics", "Event: $name [$paramsStr]")
    }
}

// Uncomment when Firebase is integrated:
// class FirebaseAnalyticsBackend(private val analytics: FirebaseAnalytics) : AnalyticsBackend {
//     override fun logEvent(name: String, params: Bundle?) {
//         analytics.logEvent(name, params)
//     }
// }
