package com.example.bw_clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget version of the clock. Reads the *widget-scope* settings
 * (`widget_*` keys via [SettingsRepository.widgetSettingsFlow]) — independent
 * from the foreground app's settings.
 *
 * Several settings are force-overridden in [WidgetContent] before rendering,
 * because the widget environment cannot support them:
 *  - `showSecondHand = false`: AppWidget cannot legally refresh more than once
 *    per minute (see [ClockWidgetReceiver]), so a second hand would freeze.
 *  - `burnInPrevention = false`: redundant — Glance recomposes the bitmap each
 *    minute tick at a fixed center, and the launcher controls placement.
 *  - `brightnessPercent = 100`: a dimming overlay on a static widget bitmap
 *    just darkens the icon against the wallpaper; not useful.
 *  - `rotation = 0`: the launcher owns widget orientation.
 */
class ClockWidget : GlanceAppWidget() {

    // SizeMode.Exact -> provideGlance re-runs whenever the launcher resizes
    // the widget, so the bitmap matches the new cell dimensions exactly.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = SettingsRepository(context)
        val settings = repository.widgetSettingsFlow.first()

        provideContent {
            WidgetContent(settings)
        }
    }

    @Composable
    private fun WidgetContent(settings: ClockSettings) {
        val context = LocalContext.current
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density

        // Use a SQUARE bitmap sized to the shorter widget side, so the clock is never
        // clipped when the allocated area is non-square (or slightly non-square due to
        // launcher padding). ContentScale.Fit on the Image then letterboxes the square
        // into the actual cell. Capped at MAX_BITMAP_SIDE_PX to keep bitmap memory bounded
        // on very large widgets (Glance bitmaps cross a Binder boundary).
        val shortSideDp = minOf(size.width.value, size.height.value)
        val sidePx = (shortSideDp * density).toInt()
            .coerceAtLeast(1)
            .coerceAtMost(MAX_BITMAP_SIDE_PX)

        val renderSettings = settings.copy(
            showSecondHand = false,
            burnInPrevention = false,
            brightnessPercent = 100,
            rotation = 0
        )

        val foregroundColor = if (settings.isDarkBackground) AndroidColor.WHITE else AndroidColor.BLACK

        val bitmap = Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawClock(
            canvas = canvas,
            width = sidePx.toFloat(),
            height = sidePx.toFloat(),
            density = density,
            settings = renderSettings,
            timeMillis = System.currentTimeMillis(),
            foregroundColor = foregroundColor,
            secondHandColor = AndroidColor.RED,
            backgroundColor = null,
            radiusPadding = 0f
        )

        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "BW clock",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>())
        )
    }

    companion object {
        private const val MAX_BITMAP_SIDE_PX = 1024
    }
}
