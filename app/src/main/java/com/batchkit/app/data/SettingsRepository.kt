package com.batchkit.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batchkit.app.core.codec.SelectionCodec
import com.batchkit.app.core.model.AppSettings
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.SortMode
import com.batchkit.app.core.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "batchkit_settings")

/** All user settings. Only DataStore is used: no network, no analytics, no accounts. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toSettings() }

    suspend fun setSafeMode(enabled: Boolean) = edit { it[Keys.SAFE_MODE] = enabled }

    suspend fun setAdvancedUnlocked(enabled: Boolean) = edit { it[Keys.ADVANCED_UNLOCKED] = enabled }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.id }

    suspend fun setSort(mode: SortMode, descending: Boolean) = edit {
        it[Keys.SORT] = mode.id
        it[Keys.SORT_DESCENDING] = descending
    }

    suspend fun setFilters(showUser: Boolean, showSystem: Boolean, runningOnly: Boolean, frozenOnly: Boolean) =
        edit {
            it[Keys.SHOW_USER_APPS] = showUser
            it[Keys.SHOW_SYSTEM_APPS] = showSystem
            it[Keys.RUNNING_ONLY] = runningOnly
            it[Keys.FROZEN_ONLY] = frozenOnly
        }

    suspend fun setDefaultAction(action: BatchAction) = edit { it[Keys.DEFAULT_ACTION] = action.id }

    suspend fun setFavouriteProfile(id: Long?) = edit { preferences ->
        if (id == null) preferences.remove(Keys.FAVOURITE_PROFILE_ID) else preferences[Keys.FAVOURITE_PROFILE_ID] = id
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDING_DONE] = completed }

    suspend fun setLastSelection(packages: List<String>) = edit {
        it[Keys.LAST_SELECTION] = SelectionCodec.encodePackages(packages)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { preferences -> block(preferences) }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        safeMode = this[Keys.SAFE_MODE] ?: true,
        advancedUnlocked = this[Keys.ADVANCED_UNLOCKED] ?: false,
        themeMode = ThemeMode.fromId(this[Keys.THEME]),
        sortMode = SortMode.fromId(this[Keys.SORT]),
        sortDescending = this[Keys.SORT_DESCENDING] ?: false,
        showUserApps = this[Keys.SHOW_USER_APPS] ?: true,
        showSystemApps = this[Keys.SHOW_SYSTEM_APPS] ?: true,
        runningOnly = this[Keys.RUNNING_ONLY] ?: false,
        frozenOnly = this[Keys.FROZEN_ONLY] ?: false,
        defaultAction = this[Keys.DEFAULT_ACTION]?.let { BatchAction.fromId(it) } ?: BatchAction.FORCE_STOP,
        favouriteProfileId = this[Keys.FAVOURITE_PROFILE_ID],
        onboardingCompleted = this[Keys.ONBOARDING_DONE] ?: false,
        lastSelection = SelectionCodec.decodePackages(this[Keys.LAST_SELECTION] ?: ""),
    )

    private object Keys {
        val SAFE_MODE = booleanPreferencesKey("safe_mode")
        val ADVANCED_UNLOCKED = booleanPreferencesKey("advanced_unlocked")
        val THEME = stringPreferencesKey("theme")
        val SORT = stringPreferencesKey("sort_mode")
        val SORT_DESCENDING = booleanPreferencesKey("sort_descending")
        val SHOW_USER_APPS = booleanPreferencesKey("show_user_apps")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val RUNNING_ONLY = booleanPreferencesKey("running_only")
        val FROZEN_ONLY = booleanPreferencesKey("frozen_only")
        val DEFAULT_ACTION = stringPreferencesKey("default_action")
        val FAVOURITE_PROFILE_ID = longPreferencesKey("favourite_profile_id")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SELECTION = stringPreferencesKey("last_selection")
    }
}
