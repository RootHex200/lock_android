package com.example.lock.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.example.lock.DeviceAdminReceiver

/**
 * U25 / AND-015/016/017: Device Owner policy. Applies the non-destructive
 * protections that gate the U20 lock guarantee — block uninstall/disable and
 * the boot-level escapes (safe boot, factory reset, secondary user). NEVER
 * wipes or bricks; declares no personal-content permission.
 */
class DpcPolicy(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin: ComponentName = DeviceAdminReceiver.componentName(context)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * Set once at enrollment. Idempotent — safe to re-run on boot.
     */
    fun applyBaselineProtections() {
        if (!isDeviceOwner()) return

        // Boot-level escape closures (gate U20).
        addRestriction(UserManager.DISALLOW_SAFE_BOOT)
        addRestriction(UserManager.DISALLOW_FACTORY_RESET)
        addRestriction(UserManager.DISALLOW_ADD_USER)

        // Keep the DPC installed/enabled.
        runCatching {
            dpm.setUninstallBlocked(admin, context.packageName, true)
        }

        // LockTask packages: this app is the only permitted kiosk package.
        runCatching {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        }
    }

    /** Restrictions that MUST NOT appear (privacy boundary, AND-017). */
    fun removeSurveillanceCapabilities() {
        // No-op by construction: we never request contacts/SMS/mic/camera/etc.
        // Enforced by the manifest permission audit test (U25).
    }

    private fun addRestriction(key: String) {
        runCatching { dpm.addUserRestriction(admin, key) }
    }
}
