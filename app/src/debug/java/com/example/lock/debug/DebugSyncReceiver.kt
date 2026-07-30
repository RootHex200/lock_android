package com.example.lock.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lock.sync.SyncWorker

/**
 * DEBUG ONLY: force an immediate command poll from adb, even while the device is
 * in the kiosk lock screen. Never present in release builds.
 *
 *   adb shell am broadcast -a com.example.lock.DEBUG_SYNC_NOW -p com.example.lock
 */
class DebugSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SyncWorker.pollNow(context.applicationContext)
    }
}
