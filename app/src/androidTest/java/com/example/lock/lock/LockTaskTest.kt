package com.example.lock.lock

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U20 / AND-007 — instrumented on the U1 matrix. Requires the app to be Device
 * Owner (set via QR provisioning / test setup); otherwise the test is skipped.
 *
 * Manual companion checks (recorded in docs/decisions/device-matrix.md): while
 * LOCKED, Home/Recent/Back/shade do not exit, and safe-boot + recovery
 * factory-reset do NOT yield an unlocked usable device.
 */
@RunWith(AndroidJUnit4::class)
class LockTaskTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    @Test
    fun lockTaskPackagesWhitelistThisApp() {
        assumeTrue("Requires Device Owner (matrix hardware)", dpm.isDeviceOwnerApp(context.packageName))

        LockController(context).enterLock()
        val whitelisted = dpm.getLockTaskPackages(
            com.example.lock.DeviceAdminReceiver.componentName(context),
        )
        assert(whitelisted.contains(context.packageName))
    }

    @Test
    fun lockAppliedOnlyAfterDeviceOwner() {
        // Lock behaviour must never engage when we are not Device Owner.
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            assert(!dpm.isLockTaskPermitted(context.packageName))
        }
    }
}
