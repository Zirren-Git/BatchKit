package com.batchkit.app.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.batchkit.app.BatchKitApp
import com.batchkit.app.MainActivity
import com.batchkit.app.R
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.engine.RunState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: one tap applies the profile the user pinned.
 *
 * The tile is honest about its preconditions: it turns inactive when Shizuku is
 * not connected or no profile is pinned, and tapping then opens the app instead
 * of pretending to have done something.
 */
class ProfileTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val container = (application as? BatchKitApp)?.container ?: return
        scope.launch {
            val pinnedId = container.settingsRepository.settings.first().favouriteProfileId
            val profile = pinnedId?.let { container.profileRepository.find(it) }
            val ready = container.shizukuStatusProvider.status.value.ready
            if (profile == null || !ready) {
                openApp()
                return@launch
            }
            val apps = container.appRepository.loadApps().associateBy { it.packageName }
            val targets = profile.packages.mapNotNull { packageName ->
                apps[packageName]?.let { PrivilegedTarget.of(it) }
            }
            if (targets.isEmpty()) {
                openApp()
                return@launch
            }
            container.runCoordinator.applySequentially(profile.actions, targets)
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTile() {
        val container = (application as? BatchKitApp)?.container
        val tile = qsTile ?: return
        val ready = container?.shizukuStatusProvider?.status?.value?.ready == true
        tile.state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (ready) {
                getString(R.string.tile_label)
            } else {
                getString(R.string.tile_not_ready)
            }
        }
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
