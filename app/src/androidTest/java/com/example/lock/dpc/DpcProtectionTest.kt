package com.example.lock.dpc

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lock.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U25 / AND-015/016/017 — DPC protection & privacy boundary. The manifest
 * permission audit runs without Device Owner; uninstall/restriction checks are
 * skipped off-hardware and validated on the U1 matrix.
 */
@RunWith(AndroidJUnit4::class)
class DpcProtectionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val forbiddenPermissions = listOf(
        "android.permission.READ_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
    )

    @Test
    fun manifestDeclaresNoPersonalContentPermission() {
        val info = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions?.toList().orEmpty()
        forbiddenPermissions.forEach { forbidden ->
            assertFalse("Manifest must not request $forbidden", requested.contains(forbidden))
        }
    }

    @Test
    fun releaseBuildStripsDevEscapeHatch() {
        // In a release build DEV_ESCAPE_ENABLED is false (BuildConfig, U25).
        if (!BuildConfig.DEBUG) {
            assertFalse(BuildConfig.DEV_ESCAPE_ENABLED)
        }
    }
}
