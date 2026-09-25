package com.batchkit.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.batchkit.app.core.model.Profile
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Daily scheduling built on WorkManager.
 *
 * A one-off request with an initial delay is re-armed after every run, which gives
 * a precise "runs at 22:00" behaviour (WorkManager periodic work drifts and cannot
 * target a clock time).
 */
object ScheduleManager {

    fun apply(context: Context, profile: Profile) {
        if (!profile.scheduleEnabled) {
            cancel(context, profile.id)
            return
        }
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(profile.id),
            ExistingWorkPolicy.REPLACE,
            request(context, profile.id, profile.scheduleHour, profile.scheduleMinute),
        )
    }

    fun cancel(context: Context, profileId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(profileId))
    }

    internal fun reschedule(context: Context, profile: Profile) {
        if (!profile.scheduleEnabled) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(profile.id),
            ExistingWorkPolicy.REPLACE,
            request(context, profile.id, profile.scheduleHour, profile.scheduleMinute),
        )
    }

    private fun request(
        context: Context,
        profileId: Long,
        hour: Int,
        minute: Int,
    ) = OneTimeWorkRequestBuilder<ProfileScheduleWorker>()
        .setInitialDelay(delayUntilNext(hour, minute), TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(ProfileScheduleWorker.KEY_PROFILE_ID to profileId))
        .addTag(TAG)
        .build()

    private fun delayUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    private fun workName(profileId: Long) = "batchkit-profile-$profileId"

    private const val TAG = "batchkit-schedule"
}
