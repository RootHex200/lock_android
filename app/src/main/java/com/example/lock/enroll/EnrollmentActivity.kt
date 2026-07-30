package com.example.lock.enroll

import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lock.BuildConfig
import com.example.lock.R
import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.CredentialStore
import com.example.lock.data.net.ApiClient
import com.example.lock.databinding.ActivityEnrollmentBinding
import com.example.lock.dpc.BatteryOptimization
import com.example.lock.dpc.DpcPolicy
import com.example.lock.state.SettingsStore
import com.example.lock.state.StateManager
import com.example.lock.state.StateStore
import com.example.lock.sync.SyncScheduler
import kotlinx.coroutines.launch

/**
 * U10 / U2: entry point after Device Owner provisioning. Reads the enrollment
 * token from the admin-extras bundle, runs enrollment, and shows a clear,
 * non-technical result (success / reset-and-retry / token / network) via the
 * pure EnrollmentOutcome mapper. UI stays XML/Views (KTD-7).
 */
class EnrollmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnrollmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = readAdminExtras()
        val token = extras?.getString(EXTRA_TOKEN) ?: intent.getStringExtra(EXTRA_TOKEN)

        if (token.isNullOrBlank()) {
            binding.resultTitle.setText(R.string.enroll_result_failed_title)
            binding.resultMessage.setText(R.string.enroll_missing_token)
            return
        }

        binding.resultTitle.setText(R.string.enroll_in_progress)

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
            val isDeviceOwner = DpcPolicy(this@EnrollmentActivity).isDeviceOwner()
            val outcome = EnrollmentOutcome.from(result, isDeviceOwner)
            render(outcome)

            if (outcome.isSuccess) {
                // Start background sync immediately (fixes "only polls when open").
                SyncScheduler.start(applicationContext)
            }
            if (outcome.promptBattery) {
                BatteryOptimization.promptIfNeeded(this@EnrollmentActivity)
            }
        }
    }

    private fun render(outcome: EnrollmentOutcome) {
        val (title, message) = when (outcome) {
            EnrollmentOutcome.SUCCESS -> R.string.enroll_result_success_title to R.string.enroll_result_success_msg
            EnrollmentOutcome.ALREADY_ENROLLED -> R.string.enroll_result_already_title to R.string.enroll_result_already_msg
            EnrollmentOutcome.NOT_DEVICE_OWNER -> R.string.enroll_result_not_owner_title to R.string.enroll_result_not_owner_msg
            EnrollmentOutcome.TOKEN_REJECTED -> R.string.enroll_result_token_title to R.string.enroll_result_token_msg
            EnrollmentOutcome.NETWORK_ERROR -> R.string.enroll_result_network_title to R.string.enroll_result_network_msg
            EnrollmentOutcome.FAILED -> R.string.enroll_result_failed_title to R.string.enroll_result_failed_msg
        }
        binding.resultTitle.setText(title)
        binding.resultMessage.setText(message)
    }

    /** The provisioning payload's PROVISIONING_ADMIN_EXTRAS_BUNDLE (U6). */
    private fun readAdminExtras(): PersistableBundle? =
        intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)

    companion object {
        const val EXTRA_TOKEN = "enrollment_token"
    }
}
