package com.example.lock.dpc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Package 1 (shared): battery-optimization exemption prompt. There is no public
 * *silent* API even for Device Owner, so offer the one-tap system prompt when
 * the app isn't already exempt. Used post-enrollment (U2) and from MainActivity.
 */
object BatteryOptimization {

    @SuppressLint("BatteryLife")
    fun promptIfNeeded(activity: Activity) {
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(activity.packageName)) return
        runCatching {
            activity.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${activity.packageName}")),
            )
        }
    }
}
