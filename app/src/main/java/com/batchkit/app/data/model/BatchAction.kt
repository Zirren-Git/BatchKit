package com.batchkit.app.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.batchkit.app.R

enum class BatchActionType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val isDestructive: Boolean,
    val requiresSafeModeDisabled: Boolean = false
) {
    FORCE_STOP(
        titleRes = R.string.action_force_stop,
        descriptionRes = R.string.action_force_stop_desc,
        icon = Icons.Default.PowerSettingsNew,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    BLOCK_BACKGROUND(
        titleRes = R.string.action_block_background,
        descriptionRes = R.string.action_block_background_desc,
        icon = Icons.Default.DoNotDisturbOn,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    BLOCK_ANY_BACKGROUND(
        titleRes = R.string.action_block_any_background,
        descriptionRes = R.string.action_block_any_background_desc,
        icon = Icons.Default.DoNotDisturbOn,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    RESTORE_BACKGROUND(
        titleRes = R.string.action_restore_background,
        descriptionRes = R.string.action_restore_background_desc,
        icon = Icons.Default.Restore,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    FREEZE(
        titleRes = R.string.action_freeze,
        descriptionRes = R.string.action_freeze_desc,
        icon = Icons.Default.AcUnit,
        isDestructive = true,
        requiresSafeModeDisabled = true
    ),
    UNFREEZE(
        titleRes = R.string.action_unfreeze,
        descriptionRes = R.string.action_unfreeze_desc,
        icon = Icons.Default.WbSunny,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    BATTERY_OPTIMIZATION_EXEMPT(
        titleRes = R.string.action_battery_exempt,
        descriptionRes = R.string.action_battery_exempt_desc,
        icon = Icons.Default.BatterySaver,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    BATTERY_OPTIMIZATION_RESTRICT(
        titleRes = R.string.action_battery_restrict,
        descriptionRes = R.string.action_battery_restrict_desc,
        icon = Icons.Default.BatteryAlert,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    CLEAR_CACHE(
        titleRes = R.string.action_clear_cache,
        descriptionRes = R.string.action_clear_cache_desc,
        icon = Icons.Default.DeleteSweep,
        isDestructive = true,
        requiresSafeModeDisabled = true
    ),
    DISABLE_NOTIFICATIONS(
        titleRes = R.string.action_notifications_off,
        descriptionRes = R.string.action_notifications_off_desc,
        icon = Icons.Default.NotificationsOff,
        isDestructive = false,
        requiresSafeModeDisabled = false
    ),
    ENABLE_NOTIFICATIONS(
        titleRes = R.string.action_notifications_on,
        descriptionRes = R.string.action_notifications_on_desc,
        icon = Icons.Default.NotificationsActive,
        isDestructive = false,
        requiresSafeModeDisabled = false
    )
}
