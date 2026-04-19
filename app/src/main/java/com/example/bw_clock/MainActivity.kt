package com.example.bw_clock

import android.os.Bundle
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.bw_clock.ui.theme.BW_clockTheme
import com.example.bw_clock.ui.theme.ClockBlack
import com.example.bw_clock.ui.theme.ClockWhite

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        // Fullscreen: hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val settings by settingsRepository.settingsFlow
                .collectAsState(initial = ClockSettings())
            var showSettings by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            val bgColor = if (settings.isDarkBackground) ClockBlack else ClockWhite

            BW_clockTheme(isDarkBackground = settings.isDarkBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    // Clock (rotated)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = settings.rotation.toFloat()
                            }
                            .clickable {
                                showSettings = !showSettings
                            }
                    ) {
                        ClockScreen(settings = settings)
                    }

                    if (showSettings) {
                        SettingsScreen(
                            settings = settings,
                            repository = settingsRepository,
                            coroutineScope = coroutineScope,
                            onDismiss = {
                                showSettings = false
                            },
                            rotation = settings.rotation
                        )
                    }
                }
            }
        }
    }
}
