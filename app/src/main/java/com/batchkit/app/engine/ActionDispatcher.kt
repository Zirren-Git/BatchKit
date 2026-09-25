package com.batchkit.app.engine

import android.os.SystemClock
import com.batchkit.app.core.model.AppActionResult
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.BatchRunSummary
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.OutcomeStatus
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.core.safety.ProtectedReason
import com.batchkit.app.core.safety.SafetyDecision
import com.batchkit.app.core.safety.SafetyPolicy
import com.batchkit.app.privileged.PrivilegedExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Runs one action over many apps, strictly sequentially.
 *
 * The dispatcher owns three responsibilities: enforcing the safety policy before
 * anything is executed, collecting a per app result, and reporting progress so the
 * UI can show a live counter. It has no Android dependencies except for the clock
 * default, so the whole dispatch and aggregation logic is covered by unit tests.
 */
class ActionDispatcher(
    private val executor: PrivilegedExecutor,
    private val safetyPolicy: SafetyPolicy,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {

    suspend fun dispatch(
        action: BatchAction,
        targets: List<PrivilegedTarget>,
        onProgress: (AppActionResult, Int, Int) -> Unit = { _, _, _ -> },
    ): BatchRunSummary {
        val batchStarted = clock()
        val results = ArrayList<AppActionResult>(targets.size)
        var index = 0

        try {
            withContext(Dispatchers.IO) {
                while (index < targets.size) {
                    if (!currentCoroutineContext().isActive) {
                        markSkipped(action, targets, index, results)
                        return@withContext
                    }
                    val target = targets[index]
                    val itemStarted = clock()
                    val outcome = evaluateAndRun(action, target)
                    results += AppActionResult(
                        packageName = target.packageName,
                        label = target.label,
                        action = action,
                        status = outcome.status,
                        reason = outcome.reason,
                        detail = outcome.detail,
                        durationMs = clock() - itemStarted,
                    )
                    index++
                    onProgress(results.last(), index, targets.size)
                }
            }
        } catch (cancelled: CancellationException) {
            // The batch was stopped. Remaining apps are reported as skipped and the
            // summary still reaches the results screen instead of vanishing.
            markSkipped(action, targets, index, results)
        }

        return BatchRunSummary(action, results, clock() - batchStarted)
    }

    private fun evaluateAndRun(action: BatchAction, target: PrivilegedTarget): ExecutionOutcome =
        when (val decision = safetyPolicy.evaluate(action, target.packageName)) {
            is SafetyDecision.Blocked -> ExecutionOutcome.failure(
                reason = if (decision.reason == ProtectedReason.DEVICE_ADMIN) {
                    FailureReason.DEVICE_ADMIN
                } else {
                    FailureReason.PROTECTED_PACKAGE
                },
                detail = decision.reason.name,
            )

            is SafetyDecision.Allowed, is SafetyDecision.Warn -> try {
                executor.execute(target, action)
            } catch (t: Throwable) {
                ExecutionOutcome.failure(FailureReason.UNKNOWN, t.message ?: t.javaClass.simpleName)
            }
        }

    /**
     * Marks every app that has no result yet as skipped because the run was stopped.
     *
     * The call is idempotent: the loop and the cancellation handler can both reach
     * it for the same run.
     */
    private fun markSkipped(
        action: BatchAction,
        targets: List<PrivilegedTarget>,
        fromIndex: Int,
        results: MutableList<AppActionResult>,
    ) {
        val start = maxOf(fromIndex, results.size)
        for (index in start until targets.size) {
            results += AppActionResult(
                packageName = targets[index].packageName,
                label = targets[index].label,
                action = action,
                status = OutcomeStatus.SKIPPED,
                reason = FailureReason.CANCELLED,
            )
        }
    }
}
