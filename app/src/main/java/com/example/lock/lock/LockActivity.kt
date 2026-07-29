package com.example.lock.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.lock.R
import com.example.lock.databinding.ActivityLockBinding
import com.example.lock.state.SettingsStore
import kotlinx.coroutines.launch

/**
 * U20 / U21 / U22: full-screen kiosk lock screen. Enters LockTask on resume,
 * blocks Back, binds shop identity/support/instruction from synced settings
 * (U3), offers a per-model emergency call action, and exits cleanly on a
 * verified Unlock (ACTION_UNLOCK broadcast).
 */
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Verified Unlock (U22): leave the kiosk and restore the launcher.
            stopLockTaskSafely()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindSettings()

        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            IntentFilter(LockController.ACTION_UNLOCK),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        // Enter kiosk mode. On a Device Owner this locks without user consent.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { startLockTask() }
        }
    }

    private fun bindSettings() {
        lifecycleScope.launch {
            val settings = SettingsStore(this@LockActivity).get()
            binding.shopName.text = settings.shopName ?: getString(R.string.app_name)
            binding.lockMessage.text = settings.lockMessage ?: getString(R.string.lock_default_message)
            binding.supportPhone.text = settings.supportPhone ?: ""
            binding.reconnectInstructions.text = settings.reconnectInstructions ?: ""

            binding.emergencyCall.setOnClickListener {
                val phone = settings.supportPhone
                if (!phone.isNullOrBlank()) {
                    // Per-model emergency/support dial (validated per model — U21 HITL).
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
            }
        }
    }

    private fun stopLockTaskSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { stopLockTask() }
        }
    }

    // Block Back from escaping the kiosk (U20).
    @Deprecated("Kiosk: back is intentionally suppressed")
    override fun onBackPressed() {
        // no-op
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }
}
