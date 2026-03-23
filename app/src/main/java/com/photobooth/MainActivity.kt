package com.photobooth

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.photobooth.kiosk.KioskManager
import com.photobooth.settings.SettingsManager
import com.photobooth.ui.components.SoundManager
import com.photobooth.ui.navigation.NavGraph
import com.photobooth.ui.theme.PhotoBoothTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var kioskManager: KioskManager
    @Inject lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
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
            PhotoBoothTheme {
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
