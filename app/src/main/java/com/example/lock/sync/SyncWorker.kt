package com.example.lock.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lock.AppContainer
import java.util.concurrent.TimeUnit

/**
 * U12 / AND-005: authenticated command poll. Enqueued expedited on an FCM wake,
 * and also scheduled as periodic fallback (WorkManager minimum 15 min — KTD-6).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        if (!container.isEnrolled()) return Result.success()

        return when (container.commandPoller.pollOnce()) {
            CommandPoller.Outcome.ERROR -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val PERIODIC = "command-poll-periodic"
        private const val EXPEDITED = "command-poll-now"

        /** Fallback periodic poll (15-min floor). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }

        /** Poll ASAP — used by the FCM wake, boot, and network-reconnect. */
        fun pollNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
