package com.batchkit.app.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.domain.SafetyManager
import com.batchkit.app.domain.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val safetyManager: SafetyManager,
    private val shizukuManager: ShizukuManager
) {
    private val tag = "AppRepository"

    /**
     * Loads all installed applications, queries their status, and returns enriched AppInfo objects.
     */
    suspend fun getInstalledApps(showSystemApps: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val runningPackages = getRunningAppPackages()
        val activeAdmins = safetyManager.getActiveDeviceAdminPackages()

        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES)
            }
        } catch (e: Throwable) {
            Log.e(tag, "Failed to load packages", e)
            emptyList()
        }

        val resultList = mutableListOf<AppInfo>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val packageName = pkg.packageName

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            if (!showSystemApps && isSystem) {
                continue
            }

            val label = try {
                appInfo.loadLabel(pm).toString().trim().ifEmpty { packageName }
            } catch (e: Throwable) {
                packageName
            }

            val isDeviceAdmin = activeAdmins.contains(packageName)
            val isProtected = safetyManager.isPackageProtected(packageName)
            val isFrozen = !appInfo.enabled
            val isRunning = runningPackages.contains(packageName)

            resultList.add(
                AppInfo(
                    packageName = packageName,
                    label = label,
                    isSystemApp = isSystem,
                    isDeviceAdmin = isDeviceAdmin,
                    isProtected = isProtected,
                    isFrozen = isFrozen,
                    isRunning = isRunning,
                    installTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    uid = appInfo.uid,
                    versionName = pkg.versionName ?: ""
                )
            )
        }

        resultList
    }

    /**
     * Gets set of packages currently running in background or foreground.
     */
    private suspend fun getRunningAppPackages(): Set<String> = withContext(Dispatchers.IO) {
        val runningSet = mutableSetOf<String>()

        // 1. Standard ActivityManager running app processes
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.runningAppProcesses?.forEach { processInfo ->
                processInfo.pkgList?.forEach { pkg ->
                    runningSet.add(pkg)
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Failed to get running processes from ActivityManager", e)
        }

        // 2. Privileged query via ADB ps if Shizuku is ready
        if (shizukuManager.shizukuInfo.value.isReady) {
            try {
                val cmdResult = shizukuManager.executeCommand(listOf("ps", "-A", "-o", "NAME"))
                if (cmdResult.isSuccess) {
                    cmdResult.output.lines().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && trimmed.contains(".")) {
                            runningSet.add(trimmed)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(tag, "Privileged ps query failed", e)
            }
        }

        runningSet
    }

    /**
     * Loads the icon for a given package name asynchronously.
     */
    suspend fun loadAppIcon(packageName: String): android.graphics.drawable.Drawable? = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Throwable) {
            null
        }
    }
}
