package com.example.lock.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lock.AppContainer
import java.util.concurrent.TimeUnit

/**
 * U12 / AND-005: authenticated command poll. Runs in the background regardless
 * of whether the app is open. Three triggers:
 *  - a ~60s self-rescheduling LOOP (works without FCM; the loop re-enqueues the
 *    next iteration at the end of every run, so it survives process death),
 *  - a 15-min periodic backstop (WorkManager floor, KTD-6),
 *  - immediate pollNow() from the FCM wake / boot / network-reconnect.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        if (!container.isEnrolled()) return Result.success()

        val outcome = runCatching { container.commandPoller.pollOnce() }
            .getOrDefault(CommandPoller.Outcome.ERROR)

        // Keep the loop alive: schedule the next poll ~POLL_INTERVAL later.
        // (Only the loop tag re-schedules; one-off pollNow / periodic do not.)
        if (tags.contains(LOOP)) {
            scheduleNextLoop(applicationContext)
        }

        return if (outcome == CommandPoller.Outcome.ERROR) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC = "command-poll-periodic"
        const val LOOP = "command-poll-loop"

        /** How often the background loop polls when FCM is unavailable. */
        var pollIntervalSeconds: Long = 60

        /** 15-min periodic backstop. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }

        /** Bootstrap the ~60s poll loop (runs immediately, then re-schedules itself). */
        fun startLoop(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().addTag(LOOP).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                LOOP, ExistingWorkPolicy.KEEP, request,
            )
        }

        private fun scheduleNextLoop(context: Context) {
            val next = OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag(LOOP)
                .setInitialDelay(pollIntervalSeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                LOOP, ExistingWorkPolicy.REPLACE, next,
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
