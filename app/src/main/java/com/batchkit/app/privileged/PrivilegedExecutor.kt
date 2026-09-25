package com.batchkit.app.privileged

import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.PrivilegedTarget

/**
 * Applies one [BatchAction] to one app.
 *
 * Implementations are allowed to block: callers always invoke this from a
 * background dispatcher, never from the main thread.
 */
interface PrivilegedExecutor {

    /** True when privileged calls can be made right now. */
    fun isReady(): Boolean

    /**
     * Runs [action] against [target].
     *
     * Implementations must not throw for ordinary failures; they return
     * [ExecutionOutcome] with a [com.batchkit.app.core.model.FailureReason]
     * instead. The caller still guards against unexpected throwables.
     */
    fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome
}

/** Executor that reports every call as unavailable; used when Shizuku is absent. */
object UnavailablePrivilegedExecutor : PrivilegedExecutor {
    override fun isReady(): Boolean = false

    override fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome =
        ExecutionOutcome.failure(
            com.batchkit.app.core.model.FailureReason.SHIZUKU_UNAVAILABLE,
            "No privileged executor is configured",
        )
}
