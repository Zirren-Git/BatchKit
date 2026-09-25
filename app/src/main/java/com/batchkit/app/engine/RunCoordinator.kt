package com.batchkit.app.engine

import com.batchkit.app.core.model.AppActionResult
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.BatchRunSummary
import com.batchkit.app.core.model.PrivilegedTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything the results screen needs to render. */
sealed interface RunState {
    data object Idle : RunState

    data class Running(
        val action: BatchAction,
        val completed: Int,
        val total: Int,
        val currentLabel: String?,
        val results: List<AppActionResult>,
    ) : RunState

    data class Finished(val summary: BatchRunSummary) : RunState
}

/**
 * Owns the "one batch at a time" state of the app.
 *
 * Runs survive navigation, so the user can walk to another tab while a batch is
 * still being applied and come back to the live counter.
 */
class RunCoordinator(
    private val runner: BatchRunner,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<RunState>(RunState.Idle)
    val state: StateFlow<RunState> = _state.asStateFlow()

    private var runningJob: Job? = null

    val isRunning: Boolean get() = runningJob?.isActive == true

    fun start(action: BatchAction, targets: List<PrivilegedTarget>) {
        if (targets.isEmpty() || isRunning) return
        val collected = ArrayList<AppActionResult>(targets.size)
        _state.value = RunState.Running(action, completed = 0, total = targets.size, currentLabel = null, results = collected)

        runningJob = scope.launch {
            val summary = runner.run(action, targets) { result, done, total ->
                collected += result
                _state.value = RunState.Running(
                    action = action,
                    completed = done,
                    total = total,
                    currentLabel = result.label,
                    results = collected.toList(),
                )
            }
            _state.value = RunState.Finished(summary)
        }
    }

    /**
     * Applies several actions to the same target set, one after the other.
     * Used by profiles, the Quick Settings tile and the scheduler.
     */
    suspend fun applySequentially(actions: List<BatchAction>, targets: List<PrivilegedTarget>) {
        if (targets.isEmpty()) return
        for (action in actions) {
            start(action, targets)
            runningJob?.join()
        }
    }

    fun reset() {
        if (isRunning) return
        _state.value = RunState.Idle
    }
}
