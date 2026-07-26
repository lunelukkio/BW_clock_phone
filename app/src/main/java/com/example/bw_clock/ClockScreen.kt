package com.example.bw_clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.example.bw_clock.ui.theme.LocalClockColors
import com.example.bw_clock.ui.theme.SecondHandRed
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Foreground Compose host for the clock face. Owns two tickers:
 *
 *  1. **Time ticker** — drives [drawClock] by updating `currentTimeMillis`.
 *     Cadence is 1 s when a second hand is shown, otherwise 60 s. The
 *     [LaunchedEffect] is keyed on `settings.showSecondHand` so toggling
 *     the second hand restarts the loop with the new cadence instead of
 *     waiting out the previous `delay`.
 *  2. **Burn-in offset ticker** — when `burnInPrevention` is on, shifts the
 *     clock center along a slow circular orbit (one full lap every 360 min)
 *     so a long-running always-on display doesn't burn a fixed clock face
 *     into the OLED. When the setting is off the offsets are forced to 0.
 *
 * `dimAlpha = 1 - brightness%`: drawn as a final black overlay inside
 * [drawClock]. This implements brightness as a darkening filter without
 * touching the actual screen backlight.
 */
@Composable
fun ClockScreen(settings: ClockSettings) {
    val clockColors = LocalClockColors.current
    val dimAlpha = 1.0f - settings.brightnessPercent / 100f
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var burnInOffsetX by remember { mutableFloatStateOf(0f) }
    var burnInOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(settings.showSecondHand) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(if (settings.showSecondHand) 1000L else 60_000L)
        }
    }

    LaunchedEffect(settings.burnInPrevention) {
        if (!settings.burnInPrevention) {
            burnInOffsetX = 0f
            burnInOffsetY = 0f
            return@LaunchedEffect
        }
        while (true) {
            val time = System.currentTimeMillis()
            val phase = (time / 60000L) % 360
            val rad = Math.toRadians(phase.toDouble())
            burnInOffsetX = (cos(rad) * 30).toFloat()
            burnInOffsetY = (sin(rad) * 20).toFloat()
            delay(60_000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(clockColors.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawClock(
                canvas = drawContext.canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                density = density,
                settings = settings,
                timeMillis = currentTimeMillis,
                foregroundColor = clockColors.foreground.toArgb(),
                secondHandColor = SecondHandRed.toArgb(),
                backgroundColor = null,
                dimAlpha = dimAlpha,
                burnInOffsetX = burnInOffsetX,
                burnInOffsetY = burnInOffsetY
            )
        }
    }
}
