package com.batchkit.app.core.model

/**
 * Everything the privileged layer needs about one app.
 *
 * [uid] and [userId] come from the PackageManager snapshot; the privileged
 * executor never has to query them again.
 */
data class PrivilegedTarget(
    val packageName: String,
    val label: String,
    val uid: Int,
    val userId: Int,
    val isSystem: Boolean = false,
    val isDeviceAdmin: Boolean = false,
) {
    companion object {
        fun of(entry: AppEntry): PrivilegedTarget = PrivilegedTarget(
            packageName = entry.packageName,
            label = entry.label,
            uid = entry.uid,
            userId = entry.userId,
            isSystem = entry.isSystem,
            isDeviceAdmin = entry.isDeviceAdmin,
        )
    }
}
