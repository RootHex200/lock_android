package com.example.lock.sync

import android.content.Context

/**
 * Single entry point for starting the background sync graph. Called after a
 * successful enrollment, on app start (if enrolled), and on boot — so polling
 * begins immediately instead of only when the app is reopened.
 *
 * Delivery without FCM: a ~60s self-rescheduling poll loop (SyncWorker) plus a
 * 15-min periodic backstop. FCM (when configured) just makes it faster (≤60s
 * expedited wake). Durable polling remains the source of truth (KTD-6).
 */
object SyncScheduler {

    fun start(context: Context) {
        val app = context.applicationContext
        SyncWorker.schedulePeriodic(app)      // 15-min backstop
        SyncWorker.startLoop(app)             // ~60s poll loop
        HeartbeatWorker.schedulePeriodic(app)
        SyncWorker.pollNow(app)               // immediate first poll
        runCatching { NetworkCallback(app).register() }
    }
}
