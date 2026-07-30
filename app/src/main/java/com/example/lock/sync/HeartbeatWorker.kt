package com.example.lock.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * U13 / AND-012: periodic health/state report (backstop to the foreground
 * service's own heartbeats). Advances last_seen_at server-side (U9).
 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        if (Heartbeater.beat(applicationContext)) Result.success() else Result.retry()

    companion object {
        private const val PERIODIC = "heartbeat-periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }
    }
}
