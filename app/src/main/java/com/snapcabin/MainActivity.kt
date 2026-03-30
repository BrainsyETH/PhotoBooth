package com.snapcabin

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.snapcabin.kiosk.KioskManager
import com.snapcabin.settings.SettingsManager
import com.snapcabin.ui.components.SoundManager
import com.snapcabin.ui.navigation.NavGraph
import com.snapcabin.ui.theme.SnapCabinTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var kioskManager: KioskManager
    @Inject lateinit var soundManager: SoundManager

    private var kioskEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()
        hideSystemUI()

        // Apply persisted settings
        lifecycleScope.launch {
            settingsManager.settings.collect { settings ->
                kioskManager.setScreenBrightness(this@MainActivity, settings.screenBrightness)
                kioskManager.keepScreenOn(this@MainActivity, true)
                kioskEnabled = settings.kioskModeEnabled

                if (settings.kioskModeEnabled) {
                    kioskManager.enterKioskMode(this@MainActivity)
                }
            }
        }

        setContent {
            SnapCabinTheme {
                // Swallow hardware back button in kiosk mode
                BackHandler(enabled = kioskEnabled) {
                    // Do nothing — prevent exiting the app
                }
                NavGraph(settingsManager = settingsManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        // Re-enter lock task mode on resume if kiosk is enabled
        if (kioskEnabled) {
            kioskManager.enterKioskMode(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    private fun hideSystemUI() {
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
