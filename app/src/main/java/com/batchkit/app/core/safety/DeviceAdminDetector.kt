package com.batchkit.app.core.safety

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.batchkit.app.privileged.ShizukuShell

/**
 * Finds device administrator apps.
 *
 * Two sources are combined: the receivers an app declares in its manifest (always
 * available) and the admins the system actually activated, which is read through
 * Shizuku when the connection is up. Active admins cannot be force-stopped or
 * disabled, so BatchKit refuses those actions instead of reporting a mysterious
 * failure.
 */
class DeviceAdminDetector(private val context: Context) {

    fun declaredAdmins(): Set<String> = try {
        val intent = Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)
        val flags = PackageManager.GET_RECEIVERS or PackageManager.MATCH_DISABLED_COMPONENTS
        @Suppress("DEPRECATION")
        context.packageManager.queryBroadcastReceivers(intent, flags)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    } catch (t: Throwable) {
        emptySet()
    }

    /** Active admins, requires a working Shizuku connection to be precise. */
    fun activeAdmins(): Set<String> {
        val result = ShizukuShell.run("dumpsys device_policy", timeoutSeconds = 15L)
        if (!result.isSuccess) return emptySet()
        return COMPONENT_PATTERN.findAll(result.stdout)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private companion object {
        val COMPONENT_PATTERN = Regex("ComponentInfo\\{([^/}]+)")
    }
}
