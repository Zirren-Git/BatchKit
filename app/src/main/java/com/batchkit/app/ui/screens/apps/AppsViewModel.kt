package com.batchkit.app.ui.screens.apps

import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batchkit.app.BatchKitApp
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.repository.AppRepository
import com.batchkit.app.data.repository.SettingsRepository
import com.batchkit.app.data.repository.SortOption
import com.batchkit.app.domain.ActionProgress
import com.batchkit.app.domain.BatchActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FilterType {
    ALL,
    USER,
    SYSTEM,
    RUNNING,
    FROZEN
}

data class AppsUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = true,
    val isSafeModeEnabled: Boolean = true,
    val isExecuting: Boolean = false,
    val executionProgress: ActionProgress? = null,
    val pendingConfirmAction: BatchActionType? = null
)

class AppsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository = BatchKitApp.instance.appRepository,
    private val settingsRepository: SettingsRepository = BatchKitApp.instance.settingsRepository,
    private val batchActionExecutor: BatchActionExecutor = BatchKitApp.instance.batchActionExecutor
) : ViewModel() {

    private val KEY_SELECTED_PACKAGES = "selected_packages"
    private val KEY_SEARCH_QUERY = "search_query"
    private val KEY_FILTER_TYPE = "filter_type"

    private val _uiState = MutableStateFlow(
        AppsUiState(
            selectedPackages = savedStateHandle.get<List<String>>(KEY_SELECTED_PACKAGES)?.toSet() ?: emptySet(),
            searchQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: "",
            filterType = savedStateHandle.get<String>(KEY_FILTER_TYPE)?.let {
                try { FilterType.valueOf(it) } catch (e: Exception) { FilterType.ALL }
            } ?: FilterType.ALL
        )
    )
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        // Collect user settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        isSafeModeEnabled = settings.isSafeModeEnabled,
                        sortOption = settings.sortOption
                    )
                }
                loadApps(showSystem = settings.showSystemApps)
            }
        }

        // Collect execution progress
        viewModelScope.launch {
            batchActionExecutor.progressFlow.collectLatest { progress ->
                _uiState.update { it.copy(executionProgress = progress) }
            }
        }
    }

    fun loadApps(showSystem: Boolean? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apps = appRepository.getInstalledApps(
                showSystemApps = showSystem ?: true
            )
            _uiState.update { state ->
                val filtered = applyFilterAndSort(
                    apps = apps,
                    query = state.searchQuery,
                    filter = state.filterType,
                    sort = state.sortOption
                )
                state.copy(
                    allApps = apps,
                    filteredApps = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val updated = if (state.selectedPackages.contains(packageName)) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            savedStateHandle[KEY_SELECTED_PACKAGES] = updated.toList()
            state.copy(selectedPackages = updated)
        }
    }

    fun selectAllFiltered() {
        _uiState.update { state ->
            val allFilteredPackages = state.filteredApps.map { it.packageName }.toSet()
            val updated = state.selectedPackages + allFilteredPackages
            savedStateHandle[KEY_SELECTED_PACKAGES] = updated.toList()
            state.copy(selectedPackages = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            savedStateHandle[KEY_SELECTED_PACKAGES] = emptyList<String>()
            state.copy(selectedPackages = emptySet())
        }
    }

    fun invertSelection() {
        _uiState.update { state ->
            val filteredPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelected = (filteredPackages - state.selectedPackages)
            savedStateHandle[KEY_SELECTED_PACKAGES] = newSelected.toList()
            state.copy(selectedPackages = newSelected)
        }
    }

    fun setSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.allApps,
                query = query,
                filter = state.filterType,
                sort = state.sortOption
            )
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun setFilterType(filterType: FilterType) {
        savedStateHandle[KEY_FILTER_TYPE] = filterType.name
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.allApps,
                query = state.searchQuery,
                filter = filterType,
                sort = state.sortOption
            )
            state.copy(filterType = filterType, filteredApps = filtered)
        }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                apps = state.allApps,
                query = state.searchQuery,
                filter = state.filterType,
                sort = sortOption
            )
            state.copy(sortOption = sortOption, filteredApps = filtered)
        }
    }

    fun onActionTrigger(action: BatchActionType) {
        if (action.isDestructive) {
            _uiState.update { it.copy(pendingConfirmAction = action) }
        } else {
            _uiState.update { it.copy(pendingConfirmAction = null) }
        }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(pendingConfirmAction = null) }
    }

    fun executeBatch(
        action: BatchActionType,
        onComplete: (BatchExecutionReport) -> Unit
    ) {
        _uiState.update { it.copy(pendingConfirmAction = null, isExecuting = true) }
        viewModelScope.launch {
            val selectedApps = _uiState.value.allApps.filter {
                _uiState.value.selectedPackages.contains(it.packageName)
            }

            val report = batchActionExecutor.executeBatch(
                apps = selectedApps,
                action = action,
                isSafeModeEnabled = _uiState.value.isSafeModeEnabled
            )

            _uiState.update { it.copy(isExecuting = false, executionProgress = null) }
            // Reload app statuses after modification
            loadApps()
            onComplete(report)
        }
    }

    suspend fun loadIcon(packageName: String): Drawable? {
        return appRepository.loadAppIcon(packageName)
    }

    private fun applyFilterAndSort(
        apps: List<AppInfo>,
        query: String,
        filter: FilterType,
        sort: SortOption
    ): List<AppInfo> {
        val q = query.trim().lowercase()

        val filtered = apps.filter { app ->
            val matchesQuery = if (q.isEmpty()) true else {
                app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
            }
            if (!matchesQuery) return@filter false

            when (filter) {
                FilterType.ALL -> true
                FilterType.USER -> !app.isSystemApp
                FilterType.SYSTEM -> app.isSystemApp
                FilterType.RUNNING -> app.isRunning
                FilterType.FROZEN -> app.isFrozen
            }
        }

        return when (sort) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.label.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.label.lowercase() }
            SortOption.PACKAGE -> filtered.sortedBy { it.packageName.lowercase() }
            SortOption.INSTALL_DATE -> filtered.sortedByDescending { it.installTime }
            SortOption.LAST_UPDATE -> filtered.sortedByDescending { it.lastUpdateTime }
        }
    }
}
