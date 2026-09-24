package com.batchkit.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batchkit.app.BatchKitApp
import com.batchkit.app.data.repository.SettingsRepository
import com.batchkit.app.data.repository.ThemeMode
import com.batchkit.app.data.repository.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = BatchKitApp.instance.settingsRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun toggleSafeMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSafeMode(enable)
        }
    }

    fun toggleShowSystemApps(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowSystemApps(show)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }
}
