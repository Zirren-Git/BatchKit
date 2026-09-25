package com.batchkit.app.core.model

enum class ShizukuPhase {
    CHECKING,
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY,
}

data class ShizukuStatus(
    val phase: ShizukuPhase = ShizukuPhase.CHECKING,
    val installed: Boolean = false,
    val running: Boolean = false,
    val permissionGranted: Boolean = false,
    val version: Int = -1,
    val uid: Int = -1,
) {
    val ready: Boolean get() = phase == ShizukuPhase.READY

    val statusLabelRes: Int
        get() = when (phase) {
            ShizukuPhase.CHECKING -> com.batchkit.app.R.string.chip_shizuku_checking
            ShizukuPhase.NOT_INSTALLED -> com.batchkit.app.R.string.chip_shizuku_not_installed
            ShizukuPhase.NOT_RUNNING -> com.batchkit.app.R.string.chip_shizuku_not_running
            ShizukuPhase.PERMISSION_REQUIRED -> com.batchkit.app.R.string.chip_shizuku_permission_required
            ShizukuPhase.READY -> com.batchkit.app.R.string.chip_shizuku_ready
        }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
