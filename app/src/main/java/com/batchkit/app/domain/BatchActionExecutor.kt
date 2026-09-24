package com.batchkit.app.domain

import android.content.Context
import android.util.Log
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.SingleActionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

data class ActionProgress(
    val currentApp: AppInfo,
    val currentIndex: Int,
    val totalCount: Int,
    val actionType: BatchActionType
)

class BatchActionExecutor(
    private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val safetyManager: SafetyManager
) {
    private val tag = "BatchActionExecutor"

    private val _progressFlow = MutableSharedFlow<ActionProgress>(extraBufferCapacity = 16)
    val progressFlow: SharedFlow<ActionProgress> = _progressFlow.asSharedFlow()

    /**
     * Executes the given action sequentially on all target applications.
     */
    suspend fun executeBatch(
        apps: List<AppInfo>,
        action: BatchActionType,
        isSafeModeEnabled: Boolean
    ): BatchExecutionReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<SingleActionResult>()

        for ((index, app) in apps.withIndex()) {
            _progressFlow.tryEmit(
                ActionProgress(
                    currentApp = app,
                    currentIndex = index + 1,
                    totalCount = apps.size,
                    actionType = action
                )
            )

            // 1. Safety check
            val safetyResult = safetyManager.checkSafety(app.packageName, action, isSafeModeEnabled)
            if (!safetyResult.isAllowed) {
                results.add(
                    SingleActionResult(
                        packageName = app.packageName,
                        appLabel = app.label,
                        actionType = action,
                        isSuccess = false,
                        isSkipped = true,
                        message = safetyResult.reason ?: "Safety rail prevented execution"
                    )
                )
                continue
            }

            // 2. Execute privileged action
            val result = executeSingleAppAction(app, action)
            results.add(result)
        }

        BatchExecutionReport(
            actionType = action,
            results = results,
            startTime = startTime,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Executes an action on a single app.
     */
    suspend fun executeSingleAppAction(
        app: AppInfo,
        action: BatchActionType
    ): SingleActionResult = withContext(Dispatchers.IO) {
        val pkg = app.packageName
        val label = app.label

        try {
            when (action) {
                BatchActionType.FORCE_STOP -> {
                    val cmd = listOf("am", "force-stop", pkg)
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Processes terminated" else res.error.ifEmpty { "Error code ${res.exitCode}" }
                    )
                }

                BatchActionType.BLOCK_BACKGROUND -> {
                    val cmd = listOf("cmd", "appops", "set", pkg, "RUN_IN_BACKGROUND", "ignore")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "RUN_IN_BACKGROUND set to ignore" else res.error
                    )
                }

                BatchActionType.BLOCK_ANY_BACKGROUND -> {
                    val cmd = listOf("cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", "ignore")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "RUN_ANY_IN_BACKGROUND set to ignore" else res.error
                    )
                }

                BatchActionType.RESTORE_BACKGROUND -> {
                    val cmd1 = listOf("cmd", "appops", "set", pkg, "RUN_IN_BACKGROUND", "allow")
                    val cmd2 = listOf("cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", "allow")
                    val res1 = shizukuManager.executeCommand(cmd1)
                    val res2 = shizukuManager.executeCommand(cmd2)
                    val success = res1.isSuccess && res2.isSuccess
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = success,
                        message = if (success) "Background activity restored to allow" else "${res1.error} ${res2.error}".trim()
                    )
                }

                BatchActionType.FREEZE -> {
                    val cmd = listOf("pm", "disable-user", "--user", "0", pkg)
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Application disabled" else res.error.ifEmpty { res.output }
                    )
                }

                BatchActionType.UNFREEZE -> {
                    val cmd = listOf("pm", "enable", pkg)
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Application enabled" else res.error.ifEmpty { res.output }
                    )
                }

                BatchActionType.BATTERY_OPTIMIZATION_EXEMPT -> {
                    val cmd = listOf("cmd", "deviceidle", "whitelist", "+$pkg")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Added to battery whitelist (unrestricted)" else res.error
                    )
                }

                BatchActionType.BATTERY_OPTIMIZATION_RESTRICT -> {
                    val cmd = listOf("cmd", "deviceidle", "whitelist", "-$pkg")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Removed from battery whitelist (optimized)" else res.error
                    )
                }

                BatchActionType.CLEAR_CACHE -> {
                    val cmd = listOf("pm", "trim-caches", "999999999999")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "Application cache trimmed" else res.error
                    )
                }

                BatchActionType.DISABLE_NOTIFICATIONS -> {
                    val cmd = listOf("cmd", "appops", "set", pkg, "POST_NOTIFICATION", "ignore")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "POST_NOTIFICATION set to ignore" else res.error
                    )
                }

                BatchActionType.ENABLE_NOTIFICATIONS -> {
                    val cmd = listOf("cmd", "appops", "set", pkg, "POST_NOTIFICATION", "allow")
                    val res = shizukuManager.executeCommand(cmd)
                    SingleActionResult(
                        packageName = pkg,
                        appLabel = label,
                        actionType = action,
                        isSuccess = res.isSuccess,
                        message = if (res.isSuccess) "POST_NOTIFICATION set to allow" else res.error
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Failed to execute $action on $pkg", e)
            SingleActionResult(
                packageName = pkg,
                appLabel = label,
                actionType = action,
                isSuccess = false,
                message = e.localizedMessage ?: "Unexpected error"
            )
        }
    }
}
