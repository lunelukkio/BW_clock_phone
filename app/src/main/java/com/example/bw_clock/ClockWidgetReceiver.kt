package com.example.bw_clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ClockWidget()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MINUTE_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> updateAll(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleMinuteTicks(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMinuteTicks(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteTicks(context)
    }

    private fun updateAll(context: Context): Job = coroutineScope.launch {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(ClockWidget::class.java)
        ids.forEach { id -> glanceAppWidget.update(context, id) }
    }

    private fun scheduleMinuteTicks(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = minuteTickPendingIntent(context)
        val now = System.currentTimeMillis()
        val nextMinute = now - (now % 60_000L) + 60_000L
        alarmManager.setRepeating(AlarmManager.RTC, nextMinute, 60_000L, pending)
    }

    private fun cancelMinuteTicks(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(minuteTickPendingIntent(context))
    }

    private fun minuteTickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClockWidgetReceiver::class.java).apply {
            action = ACTION_MINUTE_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MINUTE_TICK = "com.example.bw_clock.action.MINUTE_TICK"
    }
}
