package com.batchkit.app.ui.screens.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batchkit.app.BatchKitApp
import com.batchkit.app.core.model.AppEntry
import com.batchkit.app.core.model.AppFilters
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.core.model.SortMode
import com.batchkit.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the app list.
 *
 * The selection lives in the [SavedStateHandle], so it survives configuration
 * changes and process death. It is also written to settings so the last selection
 * can be restored after a cold start.
 */
class AppsViewModel(
    private val container: AppContainer,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selected = MutableStateFlow(restoreSelection())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _filters = MutableStateFlow(AppFilters())
    val filters: StateFlow<AppFilters> = _filters.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.NAME)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    private val _runningStateAvailable = MutableStateFlow(true)
    val runningStateAvailable: StateFlow<Boolean> = _runningStateAvailable.asStateFlow()

    private val _usageAccessGranted = MutableStateFlow(true)
    val usageAccessGranted: StateFlow<Boolean> = _usageAccessGranted.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(true)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    val visibleApps: StateFlow<List<AppEntry>> =
        combine(_apps, _filters, _sortMode, _sortDescending) { apps, filters, sortMode, descending ->
            val filtered = apps.filter { filters.matches(it) }
            val sorted = when (sortMode) {
                SortMode.NAME -> filtered.sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { entry: AppEntry -> entry.label },
                )

                SortMode.LAST_USED -> filtered.sortedByDescending { it.lastUsedTime ?: it.lastUpdateTime }
                SortMode.INSTALL_DATE -> filtered.sortedByDescending { it.firstInstallTime }
            }
            if (descending) sorted.reversed() else sorted
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val selectedCount: StateFlow<Int> = combine(_selected, _apps) { selection, apps ->
        selection.count { packageName -> apps.any { it.packageName == packageName } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.settings.first()
            _sortMode.value = settings.sortMode
            _sortDescending.value = settings.sortDescending
            _filters.value = _filters.value.copy(
                showUserApps = settings.showUserApps,
                showSystemApps = settings.showSystemApps,
                runningOnly = settings.runningOnly,
                frozenOnly = settings.frozenOnly,
            )
            _onboardingCompleted.value = settings.onboardingCompleted
            if (_selected.value.isEmpty() && settings.lastSelection.isNotEmpty()) {
                updateSelection(settings.lastSelection.toSet())
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val loaded = container.appRepository.loadApps()
            _apps.value = loaded
            _runningStateAvailable.value = loaded.isEmpty() || loaded.any { it.isRunning } ||
                container.shizukuStatusProvider.status.value.ready
            _usageAccessGranted.value = container.usageStatsProvider.hasUsageAccess()
            _loading.value = false
        }
    }

    fun setQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun applyFilters(filters: AppFilters, sortMode: SortMode, descending: Boolean) {
        _filters.value = filters
        _sortMode.value = sortMode
        _sortDescending.value = descending
        viewModelScope.launch {
            container.settingsRepository.setFilters(
                showUser = filters.showUserApps,
                showSystem = filters.showSystemApps,
                runningOnly = filters.runningOnly,
                frozenOnly = filters.frozenOnly,
            )
            container.settingsRepository.setSort(sortMode, descending)
        }
    }

    fun toggleSelection(packageName: String) {
        val next = _selected.value.toMutableSet()
        if (!next.add(packageName)) next.remove(packageName)
        updateSelection(next)
    }

    fun selectAllVisible() {
        updateSelection(_selected.value + visibleApps.value.map { it.packageName })
    }

    fun clearSelection() {
        updateSelection(emptySet())
    }

    fun completeOnboarding() {
        _onboardingCompleted.value = true
        viewModelScope.launch { container.settingsRepository.setOnboardingCompleted(true) }
    }

    fun selectedTargets(): List<PrivilegedTarget> = _apps.value
        .filter { it.packageName in _selected.value }
        .map { PrivilegedTarget.of(it) }

    /** Packages the safety policy will refuse for this action, used for the warning dialog. */
    fun blockedPackages(action: BatchAction): List<String> = _apps.value
        .filter { entry -> entry.packageName in _selected.value && isBlocked(action, entry) }
        .map { it.packageName }

    fun blockedLabels(action: BatchAction): List<String> = _apps.value
        .filter { entry -> entry.packageName in _selected.value && isBlocked(action, entry) }
        .map { it.label }

    private fun isBlocked(action: BatchAction, entry: AppEntry): Boolean =
        (action.blockedForProtectedPackages && entry.isProtected) ||
            (action.blockedForDeviceAdmins && entry.isDeviceAdmin)

    private fun restoreSelection(): Set<String> =
        savedStateHandle.get<ArrayList<String>>(KEY_SELECTION)?.toSet().orEmpty()

    private fun updateSelection(selection: Set<String>) {
        _selected.value = selection
        savedStateHandle[KEY_SELECTION] = ArrayList(selection)
        viewModelScope.launch {
            container.settingsRepository.setLastSelection(selection.toList())
        }
    }

    companion object {
        private const val KEY_SELECTION = "selection"
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BatchKitApp
                AppsViewModel(application.container, createSavedStateHandle())
            }
        }
    }
}
