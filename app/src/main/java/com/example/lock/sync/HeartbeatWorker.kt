package com.example.lock.sync

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lock.AppContainer
import com.example.lock.BuildConfig
import com.example.lock.data.net.dto.HeartbeatRequest
import java.util.concurrent.TimeUnit

/**
 * U13 / AND-012: periodic health/state report. Posts app/android version, the
 * confirmed state, last applied sequence and a small health summary to the
 * server heartbeat endpoint (advances last_seen_at, U9).
 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        val deviceId = container.credentials.deviceId() ?: return Result.success()

        val state = container.stateManager.currentState()
        val sequence = container.stateManager.lastAppliedSequence()

        val ok = runCatching {
            container.api.heartbeat(
                deviceId,
                HeartbeatRequest(
                    reportedState = state.name,
                    appVersion = BuildConfig.VERSION_NAME,
                    androidVersion = Build.VERSION.RELEASE,
                    lastSequence = sequence,
                    health = mapOf(
                        "enrollment_ok" to container.isEnrolled(),
                        "device_owner" to true,
                    ),
                ),
            ).isSuccessful
        }.getOrDefault(false)

        return if (ok) Result.success() else Result.retry()
    }

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
