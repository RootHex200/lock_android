package com.example.lock.dpc

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
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

        // Package 1: auto-grant our own notification permission so the ongoing
        // service notice shows. Battery-opt whitelist has no public silent API,
        // but it isn't required — the persistent FGS is Doze-exempt while
        // running, and the exact-alarm watchdog gets a temporary allowlist to
        // restart it when it fires (see Watchdog). MainActivity additionally
        // offers a one-tap "ignore battery optimizations" prompt as belt-and-braces.
        grantOwnNotificationPermission()
    }

    /** Auto-grant POST_NOTIFICATIONS (33+) so the ongoing service notice shows. */
    private fun grantOwnNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            dpm.setPermissionGrantState(
                admin, context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
            )
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
