package com.example.lock

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.lock.dpc.DpcPolicy

/**
 * U10 / U25: the Device Owner admin component. On first provisioning it applies
 * the non-destructive protection policy (block uninstall + the boot-level
 * escapes: safe boot, factory reset, add-user — AND-015/016).
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        DpcPolicy(context).applyBaselineProtections()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        DpcPolicy(context).applyBaselineProtections()
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context, DeviceAdminReceiver::class.java)
    }
}
