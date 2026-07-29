package com.example.lock

import android.app.Application
import com.example.lock.sync.HeartbeatWorker
import com.example.lock.sync.NetworkCallback
import com.example.lock.sync.SyncWorker

/**
 * Schedules the durable polling + heartbeat and registers the network-reconnect
 * poll trigger once the device is enrolled (KTD-6 / U12/U13).
 */
class LockApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (AppContainer(this).isEnrolled()) {
            SyncWorker.schedulePeriodic(this)
            HeartbeatWorker.schedulePeriodic(this)
            runCatching { NetworkCallback(this).register() }
        }
    }
}
