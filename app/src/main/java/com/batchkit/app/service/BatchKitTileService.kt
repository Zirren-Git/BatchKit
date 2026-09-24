package com.batchkit.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.batchkit.app.BatchKitApp
import com.batchkit.app.MainActivity
import com.batchkit.app.R
import com.batchkit.app.data.model.BatchActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BatchKitTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = BatchKitApp.instance
        val shizukuInfo = app.shizukuManager.shizukuInfo.value

        if (!shizukuInfo.isReady) {
            Toast.makeText(this, getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        // Execute top profile or quick stop running user apps
        serviceScope.launch {
            try {
                val profiles = app.profileRepository.getAllProfiles()
                val profileToRun = profiles.firstOrNull()

                if (profileToRun != null) {
                    Toast.makeText(
                        this@BatchKitTileService,
                        getString(R.string.profile_executed, profileToRun.name),
                        Toast.LENGTH_SHORT
                    ).show()

                    val installedApps = app.appRepository.getInstalledApps(showSystemApps = false)
                    val targetApps = installedApps.filter { profileToRun.targetPackageNames.contains(it.packageName) }

                    for (action in profileToRun.actions) {
                        app.batchActionExecutor.executeBatch(
                            apps = targetApps,
                            action = action,
                            isSafeModeEnabled = true
                        )
                    }
                } else {
                    // Default fallback: force-stop running non-system apps
                    val installedApps = app.appRepository.getInstalledApps(showSystemApps = false)
                    val runningApps = installedApps.filter { it.isRunning && !it.isProtected }
                    if (runningApps.isNotEmpty()) {
                        app.batchActionExecutor.executeBatch(
                            apps = runningApps,
                            action = BatchActionType.FORCE_STOP,
                            isSafeModeEnabled = true
                        )
                        Toast.makeText(
                            this@BatchKitTileService,
                            "Stopped ${runningApps.size} apps",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@BatchKitTileService,
                            "No running apps to stop",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BatchKitTile", "Failed to execute tile action", e)
            } finally {
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isReady = BatchKitApp.instance.shizukuManager.shizukuInfo.value.isReady
        tile.state = if (isReady) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
