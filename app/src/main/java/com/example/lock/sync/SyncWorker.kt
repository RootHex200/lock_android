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
 * U12 / AND-005: authenticated command poll as a WorkManager BACKSTOP to the
 * always-on foreground service (SyncForegroundService). Two triggers:
 *  - a 15-min periodic (WorkManager floor, KTD-6), and
 *  - immediate pollNow() from the FCM wake / boot / network-reconnect / adb.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        if (!container.isEnrolled()) return Result.success()

        val outcome = runCatching { container.commandPoller.pollOnce() }
            .getOrDefault(CommandPoller.Outcome.ERROR)

        return if (outcome == CommandPoller.Outcome.ERROR) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC = "command-poll-periodic"

        /** 15-min periodic backstop. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }

        /** Poll ASAP — FCM wake, boot, network-reconnect, adb DEBUG_SYNC_NOW. */
        fun pollNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
