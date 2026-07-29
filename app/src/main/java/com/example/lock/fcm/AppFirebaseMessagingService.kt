package com.example.lock.fcm

import com.example.lock.sync.SyncWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * U12 / AND-005, KTD-6: FCM is a WAKE HINT ONLY. A high-priority data message
 * tells the app to poll commands/next sooner — it NEVER carries the
 * authoritative command payload.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "poll_now") {
            SyncWorker.pollNow(applicationContext)
        }
    }

    override fun onNewToken(token: String) {
        // The refreshed token is picked up on the next heartbeat/enrollment.
        // (Persisted server-side via the heartbeat/activate payload.)
    }
}
