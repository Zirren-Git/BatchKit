package com.batchkit.app.data.model

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_DENIED,
    READY
}

data class ShizukuInfo(
    val status: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val version: Int = 0,
    val uid: Int = -1,
    val isRoot: Boolean = false
) {
    val isReady: Boolean get() = status == ShizukuStatus.READY
}
