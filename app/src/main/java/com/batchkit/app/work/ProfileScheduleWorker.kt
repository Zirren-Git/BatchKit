package com.batchkit.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.batchkit.app.BatchKitApp
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.engine.RunState

/**
 * Applies a scheduled profile.
 *
 * Disclaimer that the UI repeats to the user: a scheduled run can only work while
 * the Shizuku session is alive. When it is not, the worker records a skipped run
 * instead of failing silently.
 */
class ProfileScheduleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? BatchKitApp)?.container ?: return Result.failure()
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1L)
        if (profileId <= 0L) return Result.failure()

        val profile = container.profileRepository.find(profileId)
            ?: return Result.success(workDataOf(KEY_RESULT to RESULT_PROFILE_MISSING))

        if (!container.shizukuStatusProvider.status.value.ready) {
            return Result.success(workDataOf(KEY_RESULT to RESULT_SHIZUKU_UNAVAILABLE))
        }

        val installed = container.appRepository.loadApps().associateBy { it.packageName }
        val targets = profile.packages.mapNotNull { packageName ->
            installed[packageName]?.let { PrivilegedTarget.of(it) }
        }
        if (targets.isEmpty()) {
            return Result.success(workDataOf(KEY_RESULT to RESULT_NO_APPS))
        }

        container.runCoordinator.applySequentially(profile.actions, targets)

        // Chain the next run so a daily schedule keeps firing.
        ScheduleManager.reschedule(applicationContext, profile)

        return Result.success(workDataOf(KEY_RESULT to RESULT_APPLIED))
    }

    companion object {
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_RESULT = "result"
        const val RESULT_APPLIED = "applied"
        const val RESULT_SHIZUKU_UNAVAILABLE = "shizuku_unavailable"
        const val RESULT_NO_APPS = "no_apps"
        const val RESULT_PROFILE_MISSING = "profile_missing"
    }
}
