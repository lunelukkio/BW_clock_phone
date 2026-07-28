package com.example.bw_clock

import android.os.Bundle
import android.view.WindowManager

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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

/**
 * Single activity host. Notes for readers/maintainers:
 *
 *  - Extends [AppCompatActivity] (not `ComponentActivity`) specifically so the
 *    runtime language switch in [SettingsScreen] can call
 *    `AppCompatDelegate.setApplicationLocales(...)` and have it actually
 *    re-create the activity with the new locale. Don't downgrade the base class.
 *  - The orientation is locked to portrait in the manifest. Rotation in the UI
 *    is applied via a Compose `graphicsLayer { rotationZ = ... }` on the clock
 *    Box, *not* by changing the activity orientation, so the settings panel and
 *    system bars don't flip with the clock.
 *  - Settings are opened by **double-tap** on the clock (single tap is reserved
 *    so the user can dismiss accidentally-shown system bars by tapping anywhere).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onResume() {
        super.onResume()
        // Re-arm the widget's minute alarm. A force-stop (user-initiated or by
        // dev tooling) deletes the app's alarms, and a stopped app receives no
        // broadcasts that could restore them — so opening the app is the
        // recovery path. Redundant calls are harmless (same PendingIntent).
        ClockWidgetReceiver.scheduleNextMinuteTick(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        // Reflect widget-scope setting changes to the home-screen widget immediately.
        // Without this push, the widget would only pick up the new settings on the
        // next minute tick from ClockWidgetReceiver — visibly laggy when the user is
        // adjusting widget settings in the foreground app. `drop(1)` skips the initial
        // emission so we don't redraw the widget once on every app launch.
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
