package com.example.lock.lock

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import com.example.lock.DeviceAdminReceiver

/**
 * U20 / AND-007 + U22 / AND-009: enter/exit the Device Owner LockTask kiosk.
 *
 * The boot-level escape closures (safe boot / factory reset / add user) are set
 * once by DpcPolicy (U25); this controller assumes they are in force and only
 * drives the LockTask + the lock screen Activity.
 */
/** Lock/unlock side effects, abstracted so CommandApplier is unit-testable. */
interface LockActions {
    fun enterLock()
    fun exitLock()
}

class LockController(private val context: Context) : LockActions {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin = DeviceAdminReceiver.componentName(context)

    /** Enter the kiosk: whitelist this package + launch the lock screen. */
    override fun enterLock() {
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            runCatching { dpm.setLockTaskPackages(admin, arrayOf(context.packageName)) }
        }
        val intent = Intent(context, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Exit the kiosk and restore the launcher. Data is never wiped (AND-009).
     * LockActivity calls stopLockTask() itself in onResume; this broadcast asks
     * a live LockActivity to finish.
     */
    override fun exitLock() {
        context.sendBroadcast(Intent(ACTION_UNLOCK).setPackage(context.packageName))
    }

    companion object {
        const val ACTION_UNLOCK = "com.example.lock.ACTION_UNLOCK"
    }
}
