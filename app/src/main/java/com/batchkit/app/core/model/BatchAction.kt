package com.batchkit.app.core.model

import androidx.annotation.StringRes
import com.batchkit.app.R

/** Grouping used by the action picker. */
enum class ActionGroup(@StringRes val labelRes: Int) {
    BACKGROUND(R.string.group_background),
    LIFECYCLE(R.string.group_lifecycle),
    POWER(R.string.group_power),
    STORAGE(R.string.group_storage),
    NOTIFICATIONS(R.string.group_notifications),
}

/**
 * Every batch action BatchKit can apply to a selected set of apps.
 *
 * [id] is the stable identifier used for persistence (profiles, settings), so it
 * must never change once shipped.
 */
enum class BatchAction(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val group: ActionGroup,
    val isDestructive: Boolean = false,
    val requiresAdvancedUnlock: Boolean = false,
    val blockedForProtectedPackages: Boolean = false,
    val blockedForDeviceAdmins: Boolean = false,
) {
    FORCE_STOP(
        id = "force_stop",
        labelRes = R.string.action_force_stop,
        descriptionRes = R.string.action_force_stop_desc,
        group = ActionGroup.LIFECYCLE,
        isDestructive = true,
        blockedForProtectedPackages = true,
        blockedForDeviceAdmins = true,
    ),
    BLOCK_BACKGROUND(
        id = "block_background",
        labelRes = R.string.action_block_background,
        descriptionRes = R.string.action_block_background_desc,
        group = ActionGroup.BACKGROUND,
    ),
    BLOCK_ANY_BACKGROUND(
        id = "block_any_background",
        labelRes = R.string.action_block_any_background,
        descriptionRes = R.string.action_block_any_background_desc,
        group = ActionGroup.BACKGROUND,
    ),
    RESTORE_BACKGROUND(
        id = "restore_background",
        labelRes = R.string.action_restore_background,
        descriptionRes = R.string.action_restore_background_desc,
        group = ActionGroup.BACKGROUND,
    ),
    FREEZE(
        id = "freeze",
        labelRes = R.string.action_freeze,
        descriptionRes = R.string.action_freeze_desc,
        group = ActionGroup.LIFECYCLE,
        isDestructive = true,
        requiresAdvancedUnlock = true,
        blockedForProtectedPackages = true,
        blockedForDeviceAdmins = true,
    ),
    UNFREEZE(
        id = "unfreeze",
        labelRes = R.string.action_unfreeze,
        descriptionRes = R.string.action_unfreeze_desc,
        group = ActionGroup.LIFECYCLE,
        requiresAdvancedUnlock = true,
    ),
    BATTERY_EXEMPT(
        id = "battery_exempt",
        labelRes = R.string.action_battery_exempt,
        descriptionRes = R.string.action_battery_exempt_desc,
        group = ActionGroup.POWER,
    ),
    BATTERY_UNEXEMPT(
        id = "battery_uneempt",
        labelRes = R.string.action_battery_uneempt,
        descriptionRes = R.string.action_battery_uneempt_desc,
        group = ActionGroup.POWER,
    ),
    CLEAR_CACHE(
        id = "clear_cache",
        labelRes = R.string.action_clear_cache,
        descriptionRes = R.string.action_clear_cache_desc,
        group = ActionGroup.STORAGE,
        isDestructive = true,
        requiresAdvancedUnlock = true,
        blockedForProtectedPackages = true,
    ),
    NOTIFICATIONS_OFF(
        id = "notifications_off",
        labelRes = R.string.action_notifications_off,
        descriptionRes = R.string.action_notifications_off_desc,
        group = ActionGroup.NOTIFICATIONS,
        requiresAdvancedUnlock = true,
    ),
    NOTIFICATIONS_ON(
        id = "notifications_on",
        labelRes = R.string.action_notifications_on,
        descriptionRes = R.string.action_notifications_on_desc,
        group = ActionGroup.NOTIFICATIONS,
        requiresAdvancedUnlock = true,
    ),
    ;

    /** True when safe mode does not allow the action without an explicit unlock. */
    val requiresShizuku: Boolean get() = true

    val isReversible: Boolean
        get() = !isDestructive || this == FREEZE || this == CLEAR_CACHE

    companion object {
        /** Actions available in safe mode, which is the default for new installs. */
        val safeModeActions: List<BatchAction> = listOf(
            FORCE_STOP,
            BLOCK_BACKGROUND,
            BLOCK_ANY_BACKGROUND,
            RESTORE_BACKGROUND,
        )

        fun fromId(id: String): BatchAction? = entries.firstOrNull { it.id == id }

        fun ordered(): List<BatchAction> = entries.sortedWith(
            compareBy({ it.group.ordinal }, { it.ordinal }),
        )
    }
}
