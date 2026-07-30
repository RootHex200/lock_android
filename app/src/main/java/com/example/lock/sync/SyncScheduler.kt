package com.example.lock.sync

import android.content.Context
import com.example.lock.service.SyncForegroundService
import com.example.lock.service.Watchdog

/**
 * Single entry point for the background command channel. Called after a
 * successful enrollment, on app start (if enrolled), and on boot.
 *
 * Layers (defense in depth):
 *  1. SyncForegroundService — persistent, always-on ~45s poll loop (primary).
 *  2. Watchdog exact alarm — resurrects the service if an OEM kills it.
 *  3. WorkManager 15-min periodic + network-reconnect + boot — cheap backstops.
 *  4. FCM (when configured) — faster expedited wake via pollNow().
 * The durable server-side queue means a command is never lost, only delayed.
 */
object SyncScheduler {

    fun start(context: Context) {
        val app = context.applicationContext
        SyncForegroundService.start(app)      // always-on primary
        Watchdog.arm(app)                     // resurrect if killed
        SyncWorker.schedulePeriodic(app)      // 15-min backstop
        HeartbeatWorker.schedulePeriodic(app)
        runCatching { NetworkCallback(app).register() }
    }

    fun stop(context: Context) {
        Watchdog.cancel(context.applicationContext)
    }
}
