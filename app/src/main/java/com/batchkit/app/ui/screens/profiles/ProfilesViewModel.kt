package com.batchkit.app.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batchkit.app.BatchKitApp
import com.batchkit.app.core.model.Profile
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.di.AppContainer
import com.batchkit.app.work.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(private val container: AppContainer) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = container.profileRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _pinnedId = MutableStateFlow<Long?>(null)
    val pinnedId: StateFlow<Long?> = _pinnedId.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { _pinnedId.value = it.favouriteProfileId }
        }
    }

    fun pin(profile: Profile, pinned: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setFavouriteProfile(if (pinned) profile.id else null)
        }
    }

    fun delete(profile: Profile) {
        viewModelScope.launch {
            ScheduleManager.cancel(container.appContext, profile.id)
            container.profileRepository.delete(profile)
            if (_pinnedId.value == profile.id) {
                container.settingsRepository.setFavouriteProfile(null)
            }
        }
    }

    fun setSchedule(profile: Profile, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            container.profileRepository.setSchedule(profile.id, enabled, hour, minute)
            val updated = container.profileRepository.find(profile.id)
            if (updated != null) ScheduleManager.apply(container.appContext, updated)
        }
    }

    /**
     * Resolves a profile against the apps that are currently installed. Packages
     * that were uninstalled since the profile was saved are dropped silently.
     */
    suspend fun resolveTargets(profile: Profile): List<PrivilegedTarget> {
        val installed = container.appRepository.loadApps().associateBy { it.packageName }
        return profile.packages.mapNotNull { packageName ->
            installed[packageName]?.let { PrivilegedTarget.of(it) }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BatchKitApp
                ProfilesViewModel(application.container)
            }
        }
    }
}
