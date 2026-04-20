package com.example.bw_clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun drawClock(
    canvas: Canvas,
    width: Float,
    height: Float,
    density: Float,
    settings: ClockSettings,
    timeMillis: Long,
    foregroundColor: Int,
    secondHandColor: Int,
    backgroundColor: Int?,
    dimAlpha: Float = 0f,
    burnInOffsetX: Float = 0f,
    burnInOffsetY: Float = 0f,
    radiusPadding: Float = 0.1f
) {
    if (backgroundColor != null) {
        canvas.drawColor(backgroundColor)
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    val shortSide = min(width, height)
    val clockSizeRatio = settings.clockSizePercent / 100f
    val radius = (shortSide / 2f) * clockSizeRatio * (1f - radiusPadding)
    val clockPixelOffsetX = width * settings.clockOffsetX / 100f
    val clockPixelOffsetY = height * settings.clockOffsetY / 100f
    val centerX = width / 2f + burnInOffsetX + clockPixelOffsetX
    val centerY = height / 2f + burnInOffsetY + clockPixelOffsetY

    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = foregroundColor
    }
    val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = foregroundColor
    }

    if (settings.showFrame) {
        strokePaint.strokeWidth = 3f * density
        canvas.drawCircle(centerX, centerY, radius, strokePaint)
    }

    val majorScale = settings.majorTickScale / 100f
    val minorScale = settings.minorTickScale / 100f
    for (i in 0 until 60) {
        val isMajor = i % 5 == 0
        val scale = if (isMajor) majorScale else minorScale
        if (scale <= 0f) continue
        val baseDot = if (isMajor) 4f else 1.5f
        val dotRadius = baseDot * scale * density
        val angle = Math.toRadians(i * 6.0 - 90.0).toFloat()
        val distance = radius * 0.90f
        canvas.drawCircle(
            centerX + cos(angle) * distance,
            centerY + sin(angle) * distance,
            dotRadius,
            fillPaint
        )
    }

    if (settings.numberScale > 0) {
        drawNumbers(canvas, centerX, centerY, radius, foregroundColor, settings.clockFont, settings.numberScale / 100f)
    }

    val hourAngle = Math.toRadians(
        ((hours % 12) + minutes / 60.0) * 30.0 - 90.0
    ).toFloat()
    val hourLength = radius * 0.55f
    strokePaint.strokeWidth = 6f * density
    strokePaint.strokeCap = Paint.Cap.ROUND
    canvas.drawLine(
        centerX, centerY,
        centerX + cos(hourAngle) * hourLength,
        centerY + sin(hourAngle) * hourLength,
        strokePaint
    )

    val minuteAngle = Math.toRadians(
        (minutes + seconds / 60.0) * 6.0 - 90.0
    ).toFloat()
    val minuteLength = radius * 0.78f
    strokePaint.strokeWidth = 4f * density
    canvas.drawLine(
        centerX, centerY,
        centerX + cos(minuteAngle) * minuteLength,
        centerY + sin(minuteAngle) * minuteLength,
        strokePaint
    )

    if (settings.showSecondHand) {
        val secondAngle = Math.toRadians(seconds * 6.0 - 90.0).toFloat()
        val secondLength = radius * 0.85f
        strokePaint.color = secondHandColor
        strokePaint.strokeWidth = 2f * density
        canvas.drawLine(
            centerX, centerY,
            centerX + cos(secondAngle) * secondLength,
            centerY + sin(secondAngle) * secondLength,
            strokePaint
        )
        strokePaint.color = foregroundColor
    }

    canvas.drawCircle(centerX, centerY, 5f * density, fillPaint)
    if (settings.showSecondHand) {
        fillPaint.color = secondHandColor
        canvas.drawCircle(centerX, centerY, 3f * density, fillPaint)
        fillPaint.color = foregroundColor
    }

    if (settings.showDate) {
        drawDate(
            canvas, calendar, settings, foregroundColor,
            width, height, shortSide, burnInOffsetX, burnInOffsetY
        )
    }

    if (dimAlpha > 0f) {
        val overlay = Paint().apply {
            color = Color.argb((dimAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, width, height, overlay)
    }
}

private fun drawNumbers(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Int,
    clockFont: ClockFont,
    scale: Float
) {
    val typeface = when (clockFont) {
        ClockFont.DEFAULT -> Typeface.DEFAULT_BOLD
        ClockFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        ClockFont.MONOSPACE -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        ClockFont.SANS_SERIF_LIGHT -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        ClockFont.SANS_SERIF_BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val textPaint = Paint().apply {
        this.color = color
        textSize = radius * 0.18f * scale
        textAlign = Paint.Align.CENTER
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
        canvas.drawText(i.toString(), x, y + textCenterOffset, textPaint)
    }
}

private fun drawDate(
    canvas: Canvas,
    calendar: Calendar,
    settings: ClockSettings,
    color: Int,
    width: Float,
    height: Float,
    shortSide: Float,
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
    val baseFontSize = shortSide * 0.06f * dateScale

    val textPaint = Paint().apply {
        this.color = color
        textSize = baseFontSize
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    val lineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
    val textCenterOffset = -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f

    val datePixelOffsetX = width * settings.dateOffsetX / 100f
    val datePixelOffsetY = height * settings.dateOffsetY / 100f
    val baseX: Float
    val baseY: Float
    val isPortrait = height > width && (settings.rotation == 0 || settings.rotation == 180)

    if (isPortrait) {
        val marginHeight = (height - shortSide) / 2f
        baseX = width / 2f + burnInOffsetX
        baseY = if (settings.datePosition == DatePosition.LEFT) {
            marginHeight / 2f + burnInOffsetY
        } else {
            height - marginHeight / 2f + burnInOffsetY
        }
    } else {
        val marginWidth = (width - shortSide) / 2f
        baseY = height / 2f + burnInOffsetY
        baseX = if (settings.datePosition == DatePosition.LEFT) {
            if (marginWidth > baseFontSize) marginWidth / 2f + burnInOffsetX
            else baseFontSize * 0.8f + burnInOffsetX
        } else {
            if (marginWidth > baseFontSize) width - marginWidth / 2f + burnInOffsetX
            else width - baseFontSize * 0.8f + burnInOffsetX
        }
    }

    val x = baseX + datePixelOffsetX
    val y = baseY + datePixelOffsetY

    canvas.drawText(dateLine, x, y - lineHeight * 0.5f + textCenterOffset, textPaint)
    canvas.drawText(dayOfWeek, x, y + lineHeight * 0.5f + textCenterOffset, textPaint)
}
