package com.batchkit.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.batchkit.app.core.model.AppEntry
import com.batchkit.app.core.safety.DeviceAdminDetector
import com.batchkit.app.core.safety.ProtectedPackageResolver
import com.batchkit.app.engine.UsageStatsProvider
import com.batchkit.app.privileged.RunningAppsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the installed app list and decorates it with everything the UI needs. */
class AppRepository(
    private val context: Context,
    private val protectedResolver: ProtectedPackageResolver,
    private val deviceAdminDetector: DeviceAdminDetector,
    private val usageStatsProvider: UsageStatsProvider,
) {

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val protectedPackages = protectedResolver.resolve()
        val admins = deviceAdminDetector.declaredAdmins()
        val running = RunningAppsQuery.runningPackages()
        val lastUsed = usageStatsProvider.lastUsedTimes()

        val installed = try {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        } catch (t: Throwable) {
            emptyList()
        }

        installed.mapNotNull { info ->
            toEntry(packageManager, info, protectedPackages.keys, admins, running, lastUsed)
        }
    }

    private fun toEntry(
        packageManager: PackageManager,
        info: ApplicationInfo,
        protectedPackages: Set<String>,
        admins: Set<String>,
        running: Set<String>?,
        lastUsed: Map<String, Long>,
    ): AppEntry? {
        val packageName = info.packageName ?: return null
        val label = runCatching { packageManager.getApplicationLabel(info).toString() }
            .getOrDefault(packageName)
        val packageInfo = runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
        val enabledSetting = runCatching { packageManager.getApplicationEnabledSetting(packageName) }
            .getOrElse {
                if (info.enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            }

        return AppEntry(
            packageName = packageName,
            label = label,
            uid = info.uid,
            userId = info.uid / PER_USER_RANGE,
            isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            enabledSetting = enabledSetting,
            isDeviceAdmin = admins.contains(packageName),
            isProtected = protectedPackages.contains(packageName),
            firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
            lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
            versionName = packageInfo?.versionName,
            lastUsedTime = lastUsed[packageName],
            isRunning = running?.contains(packageName) == true,
        )
    }

    private companion object {
        /** user id = uid / PER_USER_RANGE, the platform constant. */
        const val PER_USER_RANGE = 100000
    }
}
