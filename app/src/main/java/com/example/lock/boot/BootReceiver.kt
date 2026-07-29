package com.example.lock.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.lock.AppContainer
import com.example.lock.sync.HeartbeatWorker
import com.example.lock.sync.SyncWorker

/**
 * U12 + U23 / AND-005/010: on BOOT_COMPLETED, reschedule sync/heartbeat, poll
 * immediately, and — if the last confirmed state is LOCKED — start a foreground
 * service that re-enters LockTask. We do NOT launch the lock Activity directly
 * from the receiver (Android 10+ background-activity-launch limits would leave a
 * window where a rebooted device is briefly unlocked).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        SyncWorker.schedulePeriodic(context)
        HeartbeatWorker.schedulePeriodic(context)
        SyncWorker.pollNow(context)

        if (!AppContainer(context).isEnrolled()) return

        // Start the foreground relock service; it decides whether to re-lock.
        val serviceIntent = Intent(context, RelockForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
