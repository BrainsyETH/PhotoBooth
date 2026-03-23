package com.snapcabin

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUI()

        // Apply persisted settings
        lifecycleScope.launch {
            settingsManager.settings.collect { settings ->
                kioskManager.setScreenBrightness(this@MainActivity, settings.screenBrightness)
                kioskManager.keepScreenOn(this@MainActivity, true)

                if (settings.kioskModeEnabled) {
                    kioskManager.enterKioskMode(this@MainActivity)
                }
            }
        }

        setContent {
            SnapCabinTheme {
                NavGraph(settingsManager = settingsManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
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
