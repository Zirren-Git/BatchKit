package com.batchkit.app.di

import android.content.Context
import com.batchkit.app.core.safety.DeviceAdminDetector
import com.batchkit.app.core.safety.ProtectedPackageResolver
import com.batchkit.app.core.safety.SafetyPolicy
import com.batchkit.app.data.AppIconLoader
import com.batchkit.app.data.AppRepository
import com.batchkit.app.data.ProfileRepository
import com.batchkit.app.data.SettingsRepository
import com.batchkit.app.data.db.BatchKitDatabase
import com.batchkit.app.engine.BatchRunner
import com.batchkit.app.engine.RunCoordinator
import com.batchkit.app.engine.UsageStatsProvider
import com.batchkit.app.privileged.PrivilegedActionChannel
import com.batchkit.app.shizuku.ShizukuStatusProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand rolled dependency container.
 *
 * A DI framework would add build complexity for a single module app; the graph is
 * small and explicit, and everything is created lazily on first use.
 */
class AppContainer(context: Context) {

    /** Application context, also handed to workers and schedulers. */
    val appContext: Context = context.applicationContext

    /** Application scoped, used for batch runs launched from the UI or a tile. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val shizukuStatusProvider = ShizukuStatusProvider(appContext)

    val settingsRepository = SettingsRepository(appContext)

    val profileRepository = ProfileRepository(BatchKitDatabase.get(appContext).profileDao())

    val protectedPackageResolver = ProtectedPackageResolver(appContext)

    val deviceAdminDetector = DeviceAdminDetector(appContext)

    val usageStatsProvider = UsageStatsProvider(appContext)

    val appRepository = AppRepository(
        context = appContext,
        protectedResolver = protectedPackageResolver,
        deviceAdminDetector = deviceAdminDetector,
        usageStatsProvider = usageStatsProvider,
    )

    val appIconLoader = AppIconLoader(appContext)

    val privilegedChannel = PrivilegedActionChannel(appContext)

    val batchRunner = BatchRunner(privilegedChannel) { safetyPolicy() }

    val runCoordinator = RunCoordinator(batchRunner, applicationScope)

    /** The protected list plus the currently activated device admins. */
    suspend fun safetyPolicy(): SafetyPolicy {
        val protectedPackages = protectedPackageResolver.resolve()
        val activeAdmins = deviceAdminDetector.activeAdmins()
        return SafetyPolicy(
            protectedPackages = protectedPackages,
            deviceAdminPackages = activeAdmins,
        )
    }
}
