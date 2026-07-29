package com.example.lock.diag

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lock.AppContainer
import com.example.lock.BuildConfig
import kotlinx.coroutines.launch

/**
 * U13 / AND-018: protected diagnostics — enrollment id, connectivity, last
 * command/sequence, app version. Exposes NO secrets (no keys, no credential
 * secret material — only the opaque device id).
 */
class DiagnosticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply { setPadding(48, 96, 48, 48) }
        setContentView(view)

        val container = AppContainer(applicationContext)
        lifecycleScope.launch {
            val state = container.stateManager.currentState()
            val seq = container.stateManager.lastAppliedSequence()
            view.text = buildString {
                appendLine("Enrollment id: ${container.credentials.deviceId() ?: "(not enrolled)"}")
                appendLine("Enrolled: ${container.isEnrolled()}")
                appendLine("Confirmed state: $state")
                appendLine("Last applied sequence: $seq")
                appendLine("App version: ${BuildConfig.VERSION_NAME}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Server: ${BuildConfig.SERVER_URL}")
                // NOTE: no keys / credential secrets are ever shown here (AND-018).
            }
        }
    }
}
