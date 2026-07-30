package com.example.lock

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lock.diag.DiagnosticsActivity
import com.example.lock.state.DeviceState
import kotlinx.coroutines.launch

/**
 * U11 / AND-004: while UNLOCKED the device is a fully normal phone — this
 * screen is a thin status/diagnostics entry only, with NO launcher replacement,
 * app suspension, or blocking UI. If the confirmed state is LOCKED, the kiosk
 * LockActivity is what the user actually sees (via the relock service).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.statusText)
        findViewById<Button>(R.id.diagnosticsButton).setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        val container = AppContainer(applicationContext)
        lifecycleScope.launch {
            val state = container.stateManager.currentState()
            status.text = when {
                !container.isEnrolled() -> getString(R.string.status_not_enrolled)
                state == DeviceState.LOCKED -> getString(R.string.status_locked)
                else -> getString(R.string.status_normal)
            }
            if (container.isEnrolled()) maybePromptBatteryExemption()
        }
    }

    /**
     * Package 1 (belt-and-braces): the persistent service + watchdog keep the
     * command channel alive without this, but on aggressive OEMs adding the app
     * to the battery-optimization allowlist makes it bulletproof. There is no
     * public *silent* API, so offer the one-tap system prompt if not already exempt.
     */
    @SuppressLint("BatteryLife")
    private fun maybePromptBatteryExemption() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName")),
            )
        }
    }
}
