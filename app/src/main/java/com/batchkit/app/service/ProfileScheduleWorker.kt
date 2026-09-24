package com.batchkit.app.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.batchkit.app.BatchKitApp
import com.batchkit.app.R
import com.batchkit.app.domain.BatchActionExecutor

class ProfileScheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "ProfileScheduleWorker"

    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1L)
        if (profileId == -1L) {
            Log.e(tag, "No profile ID provided to worker")
            return Result.failure()
        }

        val app = BatchKitApp.instance
        val shizukuInfo = app.shizukuManager.shizukuInfo.value
        if (!shizukuInfo.isReady) {
            Log.w(tag, "Shizuku is not running; scheduled run cannot proceed.")
            postNotification(
                "BatchKit Schedule Skipped",
                "Scheduled profile execution skipped: Shizuku service is not active."
            )
            return Result.retry()
        }

        val profile = app.profileRepository.getProfileById(profileId)
        if (profile == null) {
            Log.e(tag, "Profile with ID $profileId not found")
            return Result.failure()
        }

        val installedApps = app.appRepository.getInstalledApps(showSystemApps = true)
        val targetApps = installedApps.filter { profile.targetPackageNames.contains(it.packageName) }

        var totalSuccess = 0
        var totalFailures = 0

        for (action in profile.actions) {
            val report = app.batchActionExecutor.executeBatch(
                apps = targetApps,
                action = action,
                isSafeModeEnabled = true
            )
            totalSuccess += report.successCount
            totalFailures += report.failureCount
        }

        postNotification(
            "BatchKit: ${profile.name} Executed",
            "Completed: $totalSuccess succeeded, $totalFailures failed across ${targetApps.size} apps."
        )

        return Result.success()
    }

    private fun postNotification(title: String, content: String) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(applicationContext, BatchKitApp.CHANNEL_ID_BATCH)
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(tag, "Failed to post notification", e)
        }
    }

    companion object {
        const val KEY_PROFILE_ID = "key_profile_id"
    }
}
