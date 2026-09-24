package com.batchkit.app.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batchkit.app.BatchKitApp
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.Profile
import com.batchkit.app.data.repository.AppRepository
import com.batchkit.app.data.repository.ProfileRepository
import com.batchkit.app.domain.BatchActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isExecutingProfile: Boolean = false,
    val showCreateDialog: Boolean = false,
    val newProfileName: String = "",
    val newProfileDesc: String = "",
    val newProfileSelectedPackages: Set<String> = emptySet(),
    val newProfileSelectedActions: Set<BatchActionType> = setOf(BatchActionType.FORCE_STOP)
)

class ProfilesViewModel(
    private val profileRepository: ProfileRepository = BatchKitApp.instance.profileRepository,
    private val appRepository: AppRepository = BatchKitApp.instance.appRepository,
    private val batchActionExecutor: BatchActionExecutor = BatchKitApp.instance.batchActionExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        // Collect profiles
        viewModelScope.launch {
            profileRepository.profilesFlow.collectLatest { list ->
                _uiState.update { it.copy(profiles = list, isLoading = false) }
            }
        }

        // Load apps for profile creation
        viewModelScope.launch {
            val apps = appRepository.getInstalledApps(showSystemApps = false)
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun openCreateDialog() {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                newProfileName = "",
                newProfileDesc = "",
                newProfileSelectedPackages = emptySet(),
                newProfileSelectedActions = setOf(BatchActionType.FORCE_STOP)
            )
        }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun updateNewProfileName(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun updateNewProfileDesc(desc: String) {
        _uiState.update { it.copy(newProfileDesc = desc) }
    }

    fun toggleNewProfilePackage(packageName: String) {
        _uiState.update { state ->
            val updated = if (state.newProfileSelectedPackages.contains(packageName)) {
                state.newProfileSelectedPackages - packageName
            } else {
                state.newProfileSelectedPackages + packageName
            }
            state.copy(newProfileSelectedPackages = updated)
        }
    }

    fun toggleNewProfileAction(action: BatchActionType) {
        _uiState.update { state ->
            val updated = if (state.newProfileSelectedActions.contains(action)) {
                if (state.newProfileSelectedActions.size > 1) {
                    state.newProfileSelectedActions - action
                } else state.newProfileSelectedActions
            } else {
                state.newProfileSelectedActions + action
            }
            state.copy(newProfileSelectedActions = updated)
        }
    }

    fun saveNewProfile() {
        val state = _uiState.value
        if (state.newProfileName.isBlank() || state.newProfileSelectedPackages.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val profile = Profile(
                name = state.newProfileName.trim(),
                description = state.newProfileDesc.trim(),
                targetPackageNames = state.newProfileSelectedPackages.toList(),
                actions = state.newProfileSelectedActions.toList()
            )
            profileRepository.saveProfile(profile)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
        }
    }

    fun runProfile(profile: Profile, onComplete: (BatchExecutionReport) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingProfile = true) }

            val apps = appRepository.getInstalledApps(showSystemApps = true)
            val targets = apps.filter { profile.targetPackageNames.contains(it.packageName) }

            var finalReport: BatchExecutionReport? = null

            for (action in profile.actions) {
                finalReport = batchActionExecutor.executeBatch(
                    apps = targets,
                    action = action,
                    isSafeModeEnabled = true
                )
            }

            _uiState.update { it.copy(isExecutingProfile = false) }
            if (finalReport != null) {
                onComplete(finalReport)
            }
        }
    }
}
