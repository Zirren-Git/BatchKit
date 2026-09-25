package com.batchkit.app.engine

import com.batchkit.app.core.model.AppActionResult
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.BatchRunSummary
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.core.safety.SafetyPolicy
import com.batchkit.app.privileged.PrivilegedActionChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ties the safety policy to the privileged channel: builds the policy for the
 * current device state, opens the privileged process, dispatches the batch and
 * always closes the connection again.
 */
class BatchRunner(
    private val channel: PrivilegedActionChannel,
    private val policyProvider: suspend () -> SafetyPolicy,
) {

    suspend fun run(
        action: BatchAction,
        targets: List<PrivilegedTarget>,
        onProgress: (AppActionResult, Int, Int) -> Unit = { _, _, _ -> },
    ): BatchRunSummary {
        val policy = policyProvider()
        channel.open()
        return try {
            ActionDispatcher(channel, policy).dispatch(action, targets, onProgress)
        } finally {
            withContext(Dispatchers.IO) { channel.close() }
        }
    }
}
