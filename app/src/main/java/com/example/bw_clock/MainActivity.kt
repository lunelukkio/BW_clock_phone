package com.example.bw_clock

import android.os.Bundle
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.bw_clock.ui.theme.BW_clockTheme
import com.example.bw_clock.ui.theme.ClockBlack
import com.example.bw_clock.ui.theme.ClockWhite
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        // Reflect widget-scope setting changes to the home-screen widget immediately.
        val widget = ClockWidget()
        lifecycleScope.launch {
            settingsRepository.widgetSettingsFlow
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    val manager = GlanceAppWidgetManager(applicationContext)
                    manager.getGlanceIds(ClockWidget::class.java)
                        .forEach { id -> widget.update(applicationContext, id) }
                }
        }

        // Fullscreen: hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val appSettings by settingsRepository.appSettingsFlow
                .collectAsState(initial = ClockSettings())
            val widgetSettings by settingsRepository.widgetSettingsFlow
                .collectAsState(initial = ClockSettings())
            var showSettings by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            val bgColor = if (appSettings.isDarkBackground) ClockBlack else ClockWhite

            BW_clockTheme(isDarkBackground = appSettings.isDarkBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = appSettings.rotation.toFloat()
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { showSettings = !showSettings }
                                )
                            }
                    ) {
                        ClockScreen(settings = appSettings)
                    }

                    if (showSettings) {
                        SettingsScreen(
                            appSettings = appSettings,
                            widgetSettings = widgetSettings,
                            repository = settingsRepository,
                            coroutineScope = coroutineScope,
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}
