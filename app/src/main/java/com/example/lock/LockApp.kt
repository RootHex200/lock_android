package com.example.lock

import android.app.Application
import com.example.lock.sync.SyncScheduler

/**
 * Starts the durable polling + heartbeat once the device is enrolled. Enrollment
 * itself also calls SyncScheduler.start (see EnrollmentActivity) so background
 * sync begins immediately after activation — not only when the app is reopened.
 */
class LockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (AppContainer(this).isEnrolled()) {
            SyncScheduler.start(this)
        }
    }
}
