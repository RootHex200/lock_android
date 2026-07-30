package com.example.lock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.lock.AppContainer
import com.example.lock.R
import com.example.lock.sync.Heartbeater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Package 1: the ALWAYS-ON command channel. A persistent foreground service
 * that polls commands/next on a short interval and heartbeats periodically —
 * independent of whether the app is open and not deferred by Doze. As Device
 * Owner the user can't force-stop or dismiss it; the AlarmManager watchdog
 * (Watchdog) resurrects it if an aggressive OEM kills it. START_STICKY + a
 * restart on task removal keep it alive across process death.
 */
class SyncForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        if (loopJob?.isActive != true) {
            loopJob = scope.launch { runLoop() }
        }
        // Keep the watchdog armed every time we (re)start.
        Watchdog.arm(applicationContext)

        return START_STICKY
    }

    private suspend fun runLoop() {
        val container = AppContainer(applicationContext)
        var tick = 0L
        while (scope.isActive) {
            if (container.isEnrolled()) {
                runCatching { container.commandPoller.pollOnce() }
                if (tick % HEARTBEAT_EVERY_TICKS == 0L) {
                    runCatching { Heartbeater.beat(applicationContext) }
                }
            }
            tick++
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiped from Recents → immediately restart so the channel stays up.
        runCatching {
            ContextCompat.startForegroundService(
                applicationContext, Intent(applicationContext, SyncForegroundService::class.java),
            )
        }
        Watchdog.arm(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        loopJob?.cancel()
        // If we're being killed, make sure something will bring us back.
        Watchdog.arm(applicationContext)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** API 34+ requires the FGS type at startForeground for a typed service. */
    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Device management", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) },
            )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.sync_service_running))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "device_management"
        private const val NOTIF_ID = 7

        /** Poll cadence of the always-on loop (no FCM needed). */
        const val POLL_INTERVAL_MS = 45_000L

        /** Heartbeat every N ticks (~ every 4–5 min at 45s). */
        const val HEARTBEAT_EVERY_TICKS = 6L

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, SyncForegroundService::class.java),
                )
            }
        }
    }
}
