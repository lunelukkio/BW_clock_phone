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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.bw_clock.ui.theme.LocalClockColors
import com.example.bw_clock.ui.theme.SecondHandRed
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ClockScreen(settings: ClockSettings) {
    val clockColors = LocalClockColors.current
    val dimAlpha = 1.0f - settings.brightnessPercent / 100f
    val clockSizeRatio = settings.clockSizePercent / 100f
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var burnInOffsetX by remember { mutableFloatStateOf(0f) }
    var burnInOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(settings.showSecondHand) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(if (settings.showSecondHand) 1000L else 60_000L)
        }
    }

    // Burn-in prevention: shift position every 60 seconds
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

    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(clockColors.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shortSide = min(size.width, size.height)
            val radius = (shortSide / 2f) * clockSizeRatio * 0.9f
            val clockPixelOffsetX = size.width * settings.clockOffsetX / 100f
            val clockPixelOffsetY = size.height * settings.clockOffsetY / 100f
            val centerX = size.width / 2f + burnInOffsetX + clockPixelOffsetX
            val centerY = size.height / 2f + burnInOffsetY + clockPixelOffsetY
            val fg = clockColors.foreground

            // Frame (outer circle)
            if (settings.showFrame) {
                drawCircle(
                    color = fg,
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Tick marks (60 dots)
            if (settings.showTickMarks) {
                drawTickMarks(centerX, centerY, radius, fg)
            }

            // Numbers (1-12)
            if (settings.showNumbers) {
                drawNumbers(centerX, centerY, radius, fg, settings.clockFont)
            }

            // Hour hand
            val hourAngle = Math.toRadians(
                ((hours % 12) + minutes / 60.0) * 30.0 - 90.0
            ).toFloat()
            val hourLength = radius * 0.55f
            drawLine(
                color = fg,
                start = Offset(centerX, centerY),
                end = Offset(
                    centerX + cos(hourAngle) * hourLength,
                    centerY + sin(hourAngle) * hourLength
                ),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute hand
            val minuteAngle = Math.toRadians(
                (minutes + seconds / 60.0) * 6.0 - 90.0
            ).toFloat()
            val minuteLength = radius * 0.78f
            drawLine(
                color = fg,
                start = Offset(centerX, centerY),
                end = Offset(
                    centerX + cos(minuteAngle) * minuteLength,
                    centerY + sin(minuteAngle) * minuteLength
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Second hand
            if (settings.showSecondHand) {
                val secondAngle = Math.toRadians(seconds * 6.0 - 90.0).toFloat()
                val secondLength = radius * 0.85f
                drawLine(
                    color = SecondHandRed,
                    start = Offset(centerX, centerY),
                    end = Offset(
                        centerX + cos(secondAngle) * secondLength,
                        centerY + sin(secondAngle) * secondLength
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Center dot
            drawCircle(
                color = fg,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            if (settings.showSecondHand) {
                drawCircle(
                    color = SecondHandRed,
                    radius = 3.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }

            // Date display
            if (settings.showDate) {
                drawDate(calendar, settings, fg, burnInOffsetX, burnInOffsetY)
            }

            // Brightness dimming overlay
            if (dimAlpha > 0f) {
                drawRect(
                    color = Color.Black.copy(alpha = dimAlpha)
                )
            }
        }
    }
}

private fun DrawScope.drawDate(
    calendar: Calendar,
    settings: ClockSettings,
    color: Color,
    burnInOffsetX: Float,
    burnInOffsetY: Float
) {
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> ""
    }

    val dateLine = "$month/$day"
    val dateScale = settings.dateSizePercent / 100f
    val shortSide = min(size.width, size.height)
    val baseFontSize = shortSide * 0.06f * dateScale

    val textPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = baseFontSize
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val lineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
    val textCenterOffset = -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f

    val datePixelOffsetX = size.width * settings.dateOffsetX / 100f
    val datePixelOffsetY = size.height * settings.dateOffsetY / 100f
    var baseX: Float
    var baseY: Float
    val isPortrait = size.height > size.width && (settings.rotation == 0 || settings.rotation == 180)

    if (isPortrait) {
        val marginHeight = (size.height - shortSide) / 2f
        baseX = size.width / 2f + burnInOffsetX
        if (settings.datePosition == DatePosition.LEFT) {
            baseY = marginHeight / 2f + burnInOffsetY
        } else {
            baseY = size.height - marginHeight / 2f + burnInOffsetY
        }
    } else {
        val marginWidth = (size.width - shortSide) / 2f
        baseY = size.height / 2f + burnInOffsetY
        if (settings.datePosition == DatePosition.LEFT) {
            baseX = if (marginWidth > baseFontSize) marginWidth / 2f + burnInOffsetX
                else baseFontSize * 0.8f + burnInOffsetX
        } else {
            baseX = if (marginWidth > baseFontSize) size.width - marginWidth / 2f + burnInOffsetX
                else size.width - baseFontSize * 0.8f + burnInOffsetX
        }
    }

    val x = baseX + datePixelOffsetX
    val y = baseY + datePixelOffsetY

    drawContext.canvas.nativeCanvas.drawText(
        dateLine, x, y - lineHeight * 0.5f + textCenterOffset, textPaint
    )
    drawContext.canvas.nativeCanvas.drawText(
        dayOfWeek, x, y + lineHeight * 0.5f + textCenterOffset, textPaint
    )
}

private fun DrawScope.drawTickMarks(
    centerX: Float, centerY: Float, radius: Float, color: Color
) {
    for (i in 0 until 60) {
        val angle = Math.toRadians(i * 6.0 - 90.0).toFloat()
        val isMajor = i % 5 == 0
        val dotRadius = if (isMajor) 4.dp.toPx() else 1.5.dp.toPx()
        val distance = radius * 0.90f
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(
                centerX + cos(angle) * distance,
                centerY + sin(angle) * distance
            )
        )
    }
}

private fun DrawScope.drawNumbers(
    centerX: Float, centerY: Float, radius: Float, color: Color, clockFont: ClockFont
) {
    val typeface = when (clockFont) {
        ClockFont.DEFAULT -> android.graphics.Typeface.DEFAULT_BOLD
        ClockFont.SERIF -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        ClockFont.MONOSPACE -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        ClockFont.SANS_SERIF_LIGHT -> android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
        ClockFont.SANS_SERIF_BOLD -> android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    val textPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = radius * 0.18f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        this.typeface = typeface
    }

    val fontMetrics = textPaint.fontMetrics
    val textCenterOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f

    for (i in 1..12) {
        val angle = Math.toRadians(i * 30.0 - 90.0).toFloat()
        val distance = radius * 0.74f
        val x = centerX + cos(angle) * distance
        val y = centerY + sin(angle) * distance
        drawContext.canvas.nativeCanvas.drawText(
            i.toString(), x, y + textCenterOffset, textPaint
        )
    }
}
