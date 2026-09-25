package com.batchkit.app.core.model

import android.content.pm.PackageManager

/**
 * One installed application, as shown in the app list.
 *
 * Icons are intentionally not part of this model: they are loaded lazily by
 * [com.batchkit.app.data.AppIconLoader] so that list scrolling stays cheap.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val uid: Int,
    val userId: Int,
    val isSystem: Boolean,
    val enabledSetting: Int,
    val isDeviceAdmin: Boolean,
    val isProtected: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val versionName: String?,
    val lastUsedTime: Long? = null,
    val isRunning: Boolean = false,
) {
    val isFrozen: Boolean
        get() = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
            enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    val isDisabled: Boolean
        get() = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
}
