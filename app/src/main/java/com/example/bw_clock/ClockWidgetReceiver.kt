package com.example.bw_clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Schedules and dispatches minute-aligned redraws for [ClockWidget].
 *
 * Why a custom alarm instead of the manifest-declared `ACTION_TIME_TICK`:
 * since Android 8.0 (Oreo), implicit broadcasts such as `ACTION_TIME_TICK`
 * are no longer delivered to manifest-registered receivers, so the widget
 * would only refresh when the user interacted with it. Instead this class
 * arms an exact one-shot alarm with a custom [ACTION_MINUTE_TICK] action
 * targeted explicitly at this receiver and re-arms it on every fire —
 * a self-chaining alarm, re-aligned to the next minute boundary each time
 * so drift cannot accumulate.
 *
 * Exactness: `setExactAndAllowWhileIdle(RTC_WAKEUP)` keeps the widget on the
 * wall-clock minute even under Doze / App Standby, where the previous
 * `setRepeating(RTC)` implementation (inexact, non-waking) lagged by minutes
 * to tens of minutes. In deep Doze the OS still throttles whileIdle alarms
 * to roughly one per 15 minutes, but pending alarms are delivered on Doze
 * exit — i.e. when the screen turns on — so the visible clock re-syncs
 * almost immediately. The per-minute wakeup battery cost is accepted:
 * the app targets always-on desk-clock use.
 *
 * Recovery: alarms survive neither a reboot nor a force-stop, and the
 * widget's `updatePeriodMillis` is 0, so the chain is restarted from
 * `ACTION_BOOT_COMPLETED` / `ACTION_MY_PACKAGE_REPLACED` (both exempt from
 * the implicit-broadcast restriction) and from [MainActivity]'s `onResume`.
 * The activity path matters after a force-stop: a stopped app receives no
 * broadcasts at all, so "open the app" is the only intuitive repair.
 */
class ClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ClockWidget()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MINUTE_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Re-arm before redrawing: the redraw is async and the process
                // may be killed before it completes, but the chain must survive.
                // (Time / timezone changes also move the minute boundary, so
                // the pending alarm needs re-aligning, not just the display.)
                scheduleNextMinuteTick(context)
                updateAll(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleNextMinuteTick(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextMinuteTick(context)
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

    companion object {
        const val ACTION_MINUTE_TICK = "com.example.bw_clock.action.MINUTE_TICK"

        // Arms a one-shot exact alarm at the next wall-clock minute boundary.
        // onReceive calls this again on every fire, which keeps the chain
        // alive; MainActivity.onResume also calls it as the recovery path
        // after a force-stop wiped the alarm (see class KDoc). Safe to call
        // redundantly: the same PendingIntent is replaced in place.
        // One-shot exact replaces setRepeating(RTC), which is inexact on
        // API 19+ and was delayed unboundedly by Doze / App Standby bucket
        // demotion (the observed widget-clock lag).
        fun scheduleNextMinuteTick(context: Context) {
            if (!hasActiveWidgets(context)) {
                // No widget placed (last one removed, or boot without any):
                // stop the chain instead of waking the device every minute
                // for nothing.
                cancelMinuteTicks(context)
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = minuteTickPendingIntent(context)
            val now = System.currentTimeMillis()
            val nextMinute = now - (now % 60_000L) + 60_000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, nextMinute, pending
                    )
                } else {
                    // API 31-32 with SCHEDULE_EXACT_ALARM revoked by the user
                    // (API 33+ holds the non-revocable USE_EXACT_ALARM instead):
                    // degrade to inexact rather than crash with SecurityException.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, nextMinute, pending
                    )
                }
            } else {
                // API 21-22: Doze does not exist yet; plain setExact needs no
                // permission and fires reliably.
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMinute, pending)
            }
        }

        private fun cancelMinuteTicks(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(minuteTickPendingIntent(context))
        }

        // AppWidgetManager rather than GlanceAppWidgetManager because this must
        // be answerable synchronously inside onReceive; the provider's
        // ComponentName is the receiver class.
        private fun hasActiveWidgets(context: Context): Boolean {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ClockWidgetReceiver::class.java))
            return ids.isNotEmpty()
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
    }
}
