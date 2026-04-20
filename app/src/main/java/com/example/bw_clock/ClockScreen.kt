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
