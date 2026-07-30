package com.example.lock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Package 1 watchdog: schedules a repeating exact alarm that fires EVEN IN DOZE
 * and restarts the foreground service if an OEM killed it. Each firing re-arms
 * the next one, so the service is resurrected within ~WATCHDOG_INTERVAL. This is
 * the piece that specifically fights "killed after N minutes" OEM behaviour.
 */
object Watchdog {

    /** How often to check-and-resurrect the service. */
    const val WATCHDOG_INTERVAL_MS = 90_000L

    fun arm(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(app)
        val triggerAt = SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS

        runCatching {
            if (canScheduleExact(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            } else {
                // Fallback: inexact but still fires in Doze.
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(app))
    }

    private fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WatchdogReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private const val REQUEST_CODE = 91
    const val ACTION = "com.example.lock.WATCHDOG_TICK"
}
