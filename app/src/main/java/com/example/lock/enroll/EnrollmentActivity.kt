package com.example.lock.enroll

import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lock.BuildConfig
import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.CredentialStore
import com.example.lock.data.net.ApiClient
import com.example.lock.state.SettingsStore
import com.example.lock.state.StateManager
import com.example.lock.state.StateStore
import kotlinx.coroutines.launch

/**
 * U10: entry point after Device Owner provisioning. Reads the enrollment token
 * from the admin-extras bundle passed via the QR provisioning payload and runs
 * the enrollment. UI stays XML/Views (KTD-7).
 */
class EnrollmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { setPadding(48, 96, 48, 48) }
        setContentView(status)

        val extras = readAdminExtras()
        val token = extras?.getString(EXTRA_TOKEN) ?: intent.getStringExtra(EXTRA_TOKEN)

        if (token.isNullOrBlank()) {
            status.text = getString(com.example.lock.R.string.enroll_missing_token)
            return
        }

        status.text = getString(com.example.lock.R.string.enroll_in_progress)

        val manager = EnrollmentManager(
            api = ApiClient.create(this),
            keyStore = DeviceKeyStore(),
            credentials = CredentialStore(this),
            stateManager = StateManager(StateStore(this)),
            appVersion = BuildConfig.VERSION_NAME,
            settingsStore = SettingsStore(this),
        )

        lifecycleScope.launch {
            val result = manager.enroll(token)
            status.text = when (result) {
                is EnrollmentManager.Result.Success ->
                    getString(com.example.lock.R.string.enroll_success)
                is EnrollmentManager.Result.Failure ->
                    getString(com.example.lock.R.string.enroll_failed, result.reason)
            }
        }
    }

    /** The provisioning payload's PROVISIONING_ADMIN_EXTRAS_BUNDLE (U6). */
    private fun readAdminExtras(): PersistableBundle? =
        intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)

    companion object {
        const val EXTRA_TOKEN = "enrollment_token"
    }
}
