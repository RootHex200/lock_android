package com.example.lock.sync

import android.content.Context
import android.os.Build
import com.example.lock.AppContainer
import com.example.lock.BuildConfig
import com.example.lock.data.net.dto.HeartbeatRequest

/**
 * U13: shared heartbeat send, used by both HeartbeatWorker and the persistent
 * foreground service so the health/state report goes out on one code path.
 */
object Heartbeater {

    suspend fun beat(context: Context): Boolean {
        val container = AppContainer(context.applicationContext)
        val deviceId = container.credentials.deviceId() ?: return false

        return runCatching {
            container.api.heartbeat(
                deviceId,
                HeartbeatRequest(
                    reportedState = container.stateManager.currentState().name,
                    appVersion = BuildConfig.VERSION_NAME,
                    androidVersion = Build.VERSION.RELEASE,
                    lastSequence = container.stateManager.lastAppliedSequence(),
                    health = mapOf(
                        "enrollment_ok" to container.isEnrolled(),
                        "device_owner" to true,
                        "foreground_service" to true,
                    ),
                ),
            ).isSuccessful
        }.getOrDefault(false)
    }
}
