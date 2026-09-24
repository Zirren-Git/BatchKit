package com.batchkit.app.domain

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.batchkit.app.data.model.BatchActionType

data class SafetyCheckResult(
    val isAllowed: Boolean,
    val reason: String? = null
)

class SafetyManager(private val context: Context) {

    private val staticProtectedPackages = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.google.android.gsf",
        "moe.shizuku.privileged.api",
        "rikka.shizuku",
        context.packageName
    )

    /**
     * Resolves the current default launcher package name.
     */
    fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Resolves the default dialer package name.
     */
    fun getDefaultDialerPackage(): String? {
        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves the active keyboard / input method package name.
     */
    fun getDefaultImePackage(): String? {
        return try {
            val imeString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            imeString?.split("/")?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets all active Device Administrator package names.
     */
    fun getActiveDeviceAdminPackages(): Set<String> {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Checks if a package is classified as protected from tampering.
     */
    fun isPackageProtected(packageName: String): Boolean {
        if (staticProtectedPackages.contains(packageName)) return true
        if (packageName == context.packageName) return true
        val launcher = getDefaultLauncherPackage()
        if (launcher != null && launcher == packageName) return true
        val dialer = getDefaultDialerPackage()
        if (dialer != null && dialer == packageName) return true
        val ime = getDefaultImePackage()
        if (ime != null && ime == packageName) return true
        return false
    }

    /**
     * Checks if a package is an active device admin.
     */
    fun isDeviceAdmin(packageName: String): Boolean {
        return getActiveDeviceAdminPackages().contains(packageName)
    }

    /**
     * Verifies if a given action can be executed on a target package,
     * taking into account protected status, device administration, and safe mode.
     */
    fun checkSafety(
        packageName: String,
        action: BatchActionType,
        isSafeModeEnabled: Boolean
    ): SafetyCheckResult {
        // Never allow freezing or clearing BatchKit itself or Shizuku
        if (packageName == context.packageName || packageName == "moe.shizuku.privileged.api") {
            return SafetyCheckResult(
                isAllowed = false,
                reason = "Protected core app (BatchKit / Shizuku) cannot be modified"
            )
        }

        // Never allow freezing default launcher or system UI
        val isLauncher = (packageName == getDefaultLauncherPackage())
        val isCoreSystem = (packageName == "android" || packageName == "com.android.systemui")

        if (action == BatchActionType.FREEZE || action == BatchActionType.CLEAR_CACHE) {
            if (isLauncher) {
                return SafetyCheckResult(
                    isAllowed = false,
                    reason = "Cannot freeze or clear current default launcher"
                )
            }
            if (isCoreSystem) {
                return SafetyCheckResult(
                    isAllowed = false,
                    reason = "Cannot freeze or clear core Android system packages"
                )
            }
        }

        // Safe mode restriction
        if (isSafeModeEnabled && action.requiresSafeModeDisabled) {
            return SafetyCheckResult(
                isAllowed = false,
                reason = "Action requires Safe Mode to be disabled in Settings"
            )
        }

        // Device admin restriction
        if (isDeviceAdmin(packageName)) {
            if (action == BatchActionType.FREEZE || action == BatchActionType.FORCE_STOP) {
                return SafetyCheckResult(
                    isAllowed = false,
                    reason = "App is an active Device Administrator and cannot be frozen or stopped"
                )
            }
        }

        // Other protected packages
        if (isPackageProtected(packageName) && action == BatchActionType.FREEZE) {
            return SafetyCheckResult(
                isAllowed = false,
                reason = "App is critical to device operation and cannot be frozen"
            )
        }

        return SafetyCheckResult(isAllowed = true)
    }
}
