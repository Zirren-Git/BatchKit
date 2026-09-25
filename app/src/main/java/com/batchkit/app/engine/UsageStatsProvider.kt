package com.batchkit.app.engine

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Supplies the "last used" timestamps used by the app list sorting.
 *
 * Usage access is an optional, user granted special permission. When it is not
 * granted, sorting by last used silently falls back to the install date.
 */
class UsageStatsProvider(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = try {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } catch (t: Throwable) {
            AppOpsManager.MODE_ERRORED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Package name to last-used timestamp, empty when the permission is missing. */
    fun lastUsedTimes(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val now = System.currentTimeMillis()
        val windowStart = now - DAY_MILLIS
        return try {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, windowStart, now)
                .orEmpty()
                .filter { it.lastTimeUsed > 0L }
                .associate { it.packageName to it.lastTimeUsed }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
