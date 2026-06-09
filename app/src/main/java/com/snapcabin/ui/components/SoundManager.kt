package com.snapcabin.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sound effects for the photo booth using [ToneGenerator] (no asset
 * files needed).
 *
 * Tones play on STREAM_MUSIC — the media volume guests and operators actually
 * turn up — NOT STREAM_NOTIFICATION, which tablets silence under Do Not Disturb
 * or silent mode and which made the booth seem mute even with sound "on". If you
 * still hear nothing, raise the tablet's MEDIA volume.
 */
@Singleton
class SoundManager @Inject constructor() {

    companion object {
        private const val TAG = "SoundManager"
        private const val VOLUME = 100 // 0..100
    }

    var soundEnabled: Boolean = true
    var shutterEnabled: Boolean = true
    var countdownBeepEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = null

    /**
     * Lazily (re)creates the ToneGenerator. A one-time failure in init used to
     * leave it null forever; building on demand recovers if the audio service
     * was briefly unavailable at startup.
     */
    private fun tones(): ToneGenerator? {
        toneGenerator?.let { return it }
        return try {
            ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME).also { toneGenerator = it }
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator unavailable", e)
            null
        }
    }

    private fun play(tone: Int, durationMs: Int) {
        try {
            tones()?.startTone(tone, durationMs)
        } catch (e: Exception) {
            // A dead generator throws; drop it so the next call rebuilds.
            Log.w(TAG, "Tone playback failed; resetting generator", e)
            try { toneGenerator?.release() } catch (_: Exception) {}
            toneGenerator = null
        }
    }

    fun playCountdownBeep() {
        if (!soundEnabled || !countdownBeepEnabled) return
        play(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playCountdownFinalBeep() {
        if (!soundEnabled || !countdownBeepEnabled) return
        play(ToneGenerator.TONE_PROP_BEEP2, 300)
    }

    fun playShutter() {
        if (!soundEnabled || !shutterEnabled) return
        play(ToneGenerator.TONE_PROP_ACK, 200)
    }

    fun playSuccess() {
        if (!soundEnabled) return
        play(ToneGenerator.TONE_PROP_PROMPT, 300)
    }

    // Sample playback for the admin SOUND section — bypasses the enable flags
    // because the operator explicitly tapped Play to audition the tone.

    fun playShutterSample() = play(ToneGenerator.TONE_PROP_ACK, 200)

    fun playBeepSample() = play(ToneGenerator.TONE_PROP_BEEP, 150)

    fun release() {
        try { toneGenerator?.release() } catch (_: Exception) {}
        toneGenerator = null
    }
}
