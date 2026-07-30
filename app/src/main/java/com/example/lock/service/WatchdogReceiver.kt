package com.example.lock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lock.AppContainer

/**
 * Package 1: fired by the Watchdog exact alarm. If enrolled, (re)start the
 * foreground service — a no-op if it's already running, a resurrection if an
 * OEM killed it — then re-arm the next alarm.
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AppContainer(context).isEnrolled()) {
            SyncForegroundService.start(context)
        }
        Watchdog.arm(context)
    }
}
