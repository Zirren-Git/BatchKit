package com.batchkit.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batchkit.app.BatchKitApp
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.SingleActionResult
import com.batchkit.app.domain.BatchActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResultsFilter {
    ALL,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class ResultsUiState(
    val report: BatchExecutionReport? = null,
    val filter: ResultsFilter = ResultsFilter.ALL,
    val filteredResults: List<SingleActionResult> = emptyList(),
    val isRetrying: Boolean = false
)

class ResultsViewModel(
    private val batchActionExecutor: BatchActionExecutor = BatchKitApp.instance.batchActionExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun setReport(report: BatchExecutionReport) {
        _uiState.update {
            it.copy(
                report = report,
                filteredResults = filterResults(report.results, it.filter)
            )
        }
    }

    fun setFilter(filter: ResultsFilter) {
        _uiState.update { state ->
            val report = state.report ?: return@update state
            state.copy(
                filter = filter,
                filteredResults = filterResults(report.results, filter)
            )
        }
    }

    fun retryFailed(onComplete: (BatchExecutionReport) -> Unit) {
        val currentReport = _uiState.value.report ?: return
        val failedItems = currentReport.results.filter { !it.isSuccess && !it.isSkipped }
        if (failedItems.isEmpty()) return

        _uiState.update { it.copy(isRetrying = true) }

        viewModelScope.launch {
            val apps = failedItems.map { item ->
                AppInfo(
                    packageName = item.packageName,
                    label = item.appLabel,
                    isSystemApp = false
                )
            }

            val newReport = batchActionExecutor.executeBatch(
                apps = apps,
                action = currentReport.actionType,
                isSafeModeEnabled = false // allow retry attempt
            )

            _uiState.update {
                it.copy(
                    isRetrying = false,
                    report = newReport,
                    filteredResults = filterResults(newReport.results, it.filter)
                )
            }
            onComplete(newReport)
        }
    }

    private fun filterResults(
        results: List<SingleActionResult>,
        filter: ResultsFilter
    ): List<SingleActionResult> {
        return when (filter) {
            ResultsFilter.ALL -> results
            ResultsFilter.SUCCESS -> results.filter { it.isSuccess && !it.isSkipped }
            ResultsFilter.FAILED -> results.filter { !it.isSuccess && !it.isSkipped }
            ResultsFilter.SKIPPED -> results.filter { it.isSkipped }
        }
    }
}
