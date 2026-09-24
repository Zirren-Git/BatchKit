package com.batchkit.app.data.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed Android application and its state.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val isDeviceAdmin: Boolean = false,
    val isProtected: Boolean = false,
    val isFrozen: Boolean = false,
    val isRunning: Boolean = false,
    val isBackgroundRestricted: Boolean? = null,
    val isBatteryOptimizedExempt: Boolean? = null,
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val uid: Int = 0,
    val versionName: String = "",
    // Transient cached icon (not compared in equals/hashCode)
    val icon: Drawable? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppInfo) return false
        return packageName == other.packageName &&
                label == other.label &&
                isSystemApp == other.isSystemApp &&
                isDeviceAdmin == other.isDeviceAdmin &&
                isProtected == other.isProtected &&
                isFrozen == other.isFrozen &&
                isRunning == other.isRunning &&
                isBackgroundRestricted == other.isBackgroundRestricted &&
                isBatteryOptimizedExempt == other.isBatteryOptimizedExempt
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + isSystemApp.hashCode()
        result = 31 * result + isDeviceAdmin.hashCode()
        result = 31 * result + isProtected.hashCode()
        result = 31 * result + isFrozen.hashCode()
        result = 31 * result + isRunning.hashCode()
        return result
    }
}
