package com.batchkit.app.core.model

enum class SortMode(val id: String) {
    NAME("name"),
    LAST_USED("last_used"),
    INSTALL_DATE("install_date"),
    ;

    val descendingDefault: Boolean
        get() = this != NAME

    companion object {
        fun fromId(id: String?): SortMode = entries.firstOrNull { it.id == id } ?: NAME
    }
}

enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/** Filters applied to the installed app list. */
data class AppFilters(
    val showUserApps: Boolean = true,
    val showSystemApps: Boolean = true,
    val runningOnly: Boolean = false,
    val frozenOnly: Boolean = false,
    val query: String = "",
) {
    fun matches(entry: AppEntry): Boolean {
        if (entry.isSystem && !showSystemApps) return false
        if (!entry.isSystem && !showUserApps) return false
        if (runningOnly && !entry.isRunning) return false
        if (frozenOnly && !entry.isFrozen) return false
        if (query.isNotBlank()) {
            val normalized = query.trim().lowercase()
            val matchesLabel = entry.label.lowercase().contains(normalized)
            val matchesPackage = entry.packageName.lowercase().contains(normalized)
            if (!matchesLabel && !matchesPackage) return false
        }
        return true
    }
}

/** User settings, persisted with DataStore. */
data class AppSettings(
    val safeMode: Boolean = true,
    val advancedUnlocked: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sortMode: SortMode = SortMode.NAME,
    val sortDescending: Boolean = false,
    val showUserApps: Boolean = true,
    val showSystemApps: Boolean = true,
    val runningOnly: Boolean = false,
    val frozenOnly: Boolean = false,
    val defaultAction: BatchAction = BatchAction.FORCE_STOP,
    val favouriteProfileId: Long? = null,
    val onboardingCompleted: Boolean = false,
    /** Packages selected in the app list, restored after a cold start. */
    val lastSelection: List<String> = emptyList(),
) {
    /** True when [action] may be offered to the user right now. */
    fun allows(action: BatchAction): Boolean = !action.requiresAdvancedUnlock || advancedUnlocked || !safeMode

    fun visibleActions(): List<BatchAction> = BatchAction.ordered().filter { allows(it) }
}
