package com.batchkit.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "batchkit_settings")

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    PACKAGE,
    INSTALL_DATE,
    LAST_UPDATE
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val isSafeModeEnabled: Boolean = true,
    val showSystemApps: Boolean = false,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SAFE_MODE = booleanPreferencesKey("safe_mode_enabled")
        val SHOW_SYSTEM = booleanPreferencesKey("show_system_apps")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val safeMode = preferences[PreferencesKeys.SAFE_MODE] ?: true
        val showSystem = preferences[PreferencesKeys.SHOW_SYSTEM] ?: false
        val sortName = preferences[PreferencesKeys.SORT_OPTION] ?: SortOption.NAME_ASC.name
        val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name

        val sortOption = try {
            SortOption.valueOf(sortName)
        } catch (e: Exception) {
            SortOption.NAME_ASC
        }

        val themeMode = try {
            ThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        UserSettings(
            isSafeModeEnabled = safeMode,
            showSystemApps = showSystem,
            sortOption = sortOption,
            themeMode = themeMode
        )
    }

    suspend fun setSafeMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAFE_MODE] = enabled
        }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SYSTEM] = show
        }
    }

    suspend fun setSortOption(sortOption: SortOption) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_OPTION] = sortOption.name
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }
}
