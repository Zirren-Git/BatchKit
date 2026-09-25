package com.batchkit.app.core.model

/** Why an action did or did not apply to one app. */
enum class FailureReason {
    NONE,
    PROTECTED_PACKAGE,
    DEVICE_ADMIN,
    SHIZUKU_UNAVAILABLE,
    PERMISSION_DENIED,
    METHOD_NOT_FOUND,
    NOT_SUPPORTED,
    REMOTE_ERROR,
    SHELL_FAILED,
    CANCELLED,
    UNKNOWN,
}

enum class OutcomeStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
}

/** Result of a privileged call for a single app and a single action. */
data class ExecutionOutcome(
    val success: Boolean,
    val reason: FailureReason = FailureReason.NONE,
    val detail: String? = null,
) {
    val status: OutcomeStatus
        get() = when {
            success -> OutcomeStatus.SUCCESS
            reason == FailureReason.PROTECTED_PACKAGE || reason == FailureReason.DEVICE_ADMIN -> OutcomeStatus.SKIPPED
            else -> OutcomeStatus.FAILED
        }

    companion object {
        fun ok(): ExecutionOutcome = ExecutionOutcome(success = true)

        fun failure(reason: FailureReason, detail: String? = null): ExecutionOutcome =
            ExecutionOutcome(success = false, reason = reason, detail = detail)
    }
}

/** One row on the results screen. */
data class AppActionResult(
    val packageName: String,
    val label: String,
    val action: BatchAction,
    val status: OutcomeStatus,
    val reason: FailureReason = FailureReason.NONE,
    val detail: String? = null,
    val durationMs: Long = 0L,
)

/** Aggregated outcome of a whole batch run. */
data class BatchRunSummary(
    val action: BatchAction,
    val results: List<AppActionResult>,
    val durationMs: Long,
) {
    val total: Int get() = results.size
    val succeeded: Int get() = results.count { it.status == OutcomeStatus.SUCCESS }
    val failed: Int get() = results.count { it.status == OutcomeStatus.FAILED }
    val skipped: Int get() = results.count { it.status == OutcomeStatus.SKIPPED }

    val failedPackages: List<String>
        get() = results.filter { it.status == OutcomeStatus.FAILED }.map { it.packageName }

    companion object {
        fun empty(action: BatchAction): BatchRunSummary = BatchRunSummary(action, emptyList(), 0L)
    }
}
