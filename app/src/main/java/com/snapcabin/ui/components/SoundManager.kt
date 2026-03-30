package com.snapcabin.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sound effects for the photo booth.
 * Uses ToneGenerator for beeps (no asset files needed) and system
 * camera shutter sound.
 */
@Singleton
class SoundManager @Inject constructor() {

    private var toneGenerator: ToneGenerator? = null
    var soundEnabled: Boolean = true
    var shutterEnabled: Boolean = true
    var countdownBeepEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            // Audio not available
        }
    }

    fun playCountdownBeep() {
        if (!soundEnabled || !countdownBeepEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            // Ignore audio errors
        }
    }

    fun playCountdownFinalBeep() {
        if (!soundEnabled || !countdownBeepEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (e: Exception) {
            // Ignore audio errors
        }
    }

    fun playShutter() {
        if (!soundEnabled || !shutterEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (e: Exception) {
            // Ignore audio errors
        }
    }

    fun playSuccess() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        } catch (e: Exception) {
            // Ignore audio errors
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
