package com.example.bw_clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure drawing function shared by the foreground app ([ClockScreen]) and the
 * home-screen widget ([ClockWidget]). Does not allocate beyond [Paint]/[Path]
 * locals and reads no global state — all inputs come through parameters so the
 * same renderer can target either a Compose [androidx.compose.foundation.Canvas]
 * (via `nativeCanvas`) or an off-screen [Bitmap]-backed [Canvas].
 *
 * Coordinates: time is rendered around `(width/2, height/2)` plus
 * [burnInOffsetX]/[burnInOffsetY] (anti burn-in shift) plus the user-configured
 * `clockOffset*` from [settings]. `0°` points up (12 o'clock), angles grow
 * clockwise (the `- 90°` term in each `Math.toRadians(...)` rotates the math
 * convention into the clock convention).
 *
 * @param width Drawable width in pixels.
 * @param height Drawable height in pixels.
 * @param density Display density (`displayMetrics.density`) used to scale
 *   stroke widths and dot radii so dp-relative sizes stay visually consistent
 *   across devices.
 * @param timeMillis Wall-clock time to render. Passed in (not read from
 *   `System.currentTimeMillis()`) so the caller controls the tick cadence.
 * @param backgroundColor If non-null the canvas is cleared to this color first.
 *   Pass `null` when drawing onto a surface that already has a background
 *   (Compose Box background, or an `ARGB_8888` bitmap that the widget composites
 *   over the launcher).
 * @param dimAlpha 0..1 black overlay applied last (post-everything). Used to
 *   implement the `brightnessPercent` setting in the foreground app; the widget
 *   passes 0 because Glance can't usefully dim a static bitmap.
 * @param burnInOffsetX X pixel shift applied to the clock center to prevent
 *   OLED burn-in when the app is used as an always-on display.
 * @param burnInOffsetY Y pixel shift, same purpose as [burnInOffsetX].
 * @param radiusPadding Fraction of the short side reserved as outer margin
 *   before computing `radius`. 0.1 for the app (breathing room from the
 *   screen edge), 0.0 for the widget so the clock fills its allocated cell.
 */
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

    val thickness = settings.handThicknessScale / 100f
    if (thickness > 0f) {
        val hourAngle = Math.toRadians(
            ((hours % 12) + minutes / 60.0) * 30.0 - 90.0
        ).toFloat()
        drawHand(
            canvas, centerX, centerY, hourAngle, radius * 0.55f,
            6f * density * thickness, foregroundColor, settings.handTipStyle, strokePaint, fillPaint
        )

        val minuteAngle = Math.toRadians(
            (minutes + seconds / 60.0) * 6.0 - 90.0
        ).toFloat()
        drawHand(
            canvas, centerX, centerY, minuteAngle, radius * 0.78f,
            4f * density * thickness, foregroundColor, settings.handTipStyle, strokePaint, fillPaint
        )

        if (settings.showSecondHand) {
            val secondAngle = Math.toRadians(seconds * 6.0 - 90.0).toFloat()
            drawHand(
                canvas, centerX, centerY, secondAngle, radius * 0.85f,
                2f * density * thickness, secondHandColor, settings.handTipStyle, strokePaint, fillPaint
            )
        }
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

/**
 * Draws a single hand from the center to `(centerX + cos(angle)*length,
 * centerY + sin(angle)*length)`.
 *
 * ROUNDED and SQUARED are cheap stroked lines (differ only in [Paint.Cap]).
 * TAPERED is drawn as a filled triangle [Path] — base of width [strokeWidth]
 * at the center, apex at the tip — which is why it cannot be expressed as a
 * stroke cap and needs its own branch.
 *
 * Mutates the shared [strokePaint] and [fillPaint] in place (color / strokeWidth
 * / strokeCap). The trailing `strokePaint.color = color` is a deliberate reset
 * so a later caller in [drawClock] that reuses the same paint inherits a known
 * color rather than whatever the previous hand left behind.
 */
private fun drawHand(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    angle: Float,
    length: Float,
    strokeWidth: Float,
    color: Int,
    style: HandTipStyle,
    strokePaint: Paint,
    fillPaint: Paint
) {
    val endX = centerX + cos(angle) * length
    val endY = centerY + sin(angle) * length
    when (style) {
        HandTipStyle.ROUNDED -> {
            strokePaint.color = color
            strokePaint.strokeWidth = strokeWidth
            strokePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(centerX, centerY, endX, endY, strokePaint)
        }
        HandTipStyle.SQUARED -> {
            strokePaint.color = color
            strokePaint.strokeWidth = strokeWidth
            strokePaint.strokeCap = Paint.Cap.BUTT
            canvas.drawLine(centerX, centerY, endX, endY, strokePaint)
        }
        HandTipStyle.TAPERED -> {
            val dx = endX - centerX
            val dy = endY - centerY
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
            val normX = -dy / len
            val normY = dx / len
            val halfW = strokeWidth / 2f
            val path = Path().apply {
                moveTo(centerX + normX * halfW, centerY + normY * halfW)
                lineTo(centerX - normX * halfW, centerY - normY * halfW)
                lineTo(endX, endY)
                close()
            }
            fillPaint.color = color
            canvas.drawPath(path, fillPaint)
            fillPaint.color = color
        }
    }
    // Restore the stroke paint color to the foreground so the caller's later draws
    // don't accidentally inherit the hand color.
    strokePaint.color = color
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

/**
 * Draws the date (`M/D` on the first line, English day-of-week abbreviation
 * on the second). The date is positioned in the margin *outside* the clock
 * face: in portrait it goes above (LEFT) or below (RIGHT) the square clock
 * area; in landscape it goes to the left or right of it. When the margin is
 * narrower than the font size (landscape on a near-square cell) it falls back
 * to a small inset from the edge instead of centering in the margin.
 *
 * Day-of-week strings are hard-coded English on purpose — they are part of the
 * clock face design, not localized UI text, so they stay identical between the
 * ja and en app locales.
 */
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
