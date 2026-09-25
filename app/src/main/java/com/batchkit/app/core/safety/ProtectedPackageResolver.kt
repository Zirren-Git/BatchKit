package com.batchkit.app.core.safety

import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.batchkit.app.core.model.ShizukuStatus

/**
 * Builds the device specific protected package list.
 *
 * The list is intentionally resolved from the running system instead of being
 * hardcoded: launcher packages, the active input methods and the vendor specific
 * system apps differ per device.
 */
class ProtectedPackageResolver(private val context: Context) {

    fun resolve(): Map<String, ProtectedReason> {
        val result = LinkedHashMap<String, ProtectedReason>()

        result[context.packageName] = ProtectedReason.SELF
        result[ShizukuStatus.SHIZUKU_PACKAGE] = ProtectedReason.SHIZUKU

        SYSTEM_CRITICAL.forEach { packageName ->
            if (!result.containsKey(packageName)) result[packageName] = ProtectedReason.SYSTEM_CRITICAL
        }
        launcherPackages().forEach { packageName ->
            if (!result.containsKey(packageName)) result[packageName] = ProtectedReason.LAUNCHER
        }
        inputMethodPackages().forEach { packageName ->
            if (!result.containsKey(packageName)) result[packageName] = ProtectedReason.INPUT_METHOD
        }
        return result
    }

    private fun launcherPackages(): List<String> = try {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
    } catch (t: Throwable) {
        emptyList()
    }

    private fun inputMethodPackages(): List<String> = try {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.inputMethodList?.mapNotNull { it.packageName }.orEmpty()
    } catch (t: Throwable) {
        emptyList()
    }

    private companion object {
        val SYSTEM_CRITICAL = listOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.shell",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.providers.settings",
            "com.android.providers.telephony",
            "com.android.providers.contacts",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.providers.calendar",
            "com.android.keychain",
            "com.android.certinstaller",
            "com.android.location.fused",
        )
    }
}
