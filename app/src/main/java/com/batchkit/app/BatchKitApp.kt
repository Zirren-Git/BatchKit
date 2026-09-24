package com.batchkit.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.batchkit.app.data.local.AppDatabase
import com.batchkit.app.data.repository.AppRepository
import com.batchkit.app.data.repository.ProfileRepository
import com.batchkit.app.data.repository.SettingsRepository
import com.batchkit.app.domain.BatchActionExecutor
import com.batchkit.app.domain.SafetyManager
import com.batchkit.app.domain.ShizukuManager

class BatchKitApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var safetyManager: SafetyManager
        private set

    lateinit var shizukuManager: ShizukuManager
        private set

    lateinit var appRepository: AppRepository
        private set

    lateinit var batchActionExecutor: BatchActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        profileRepository = ProfileRepository(database.profileDao())
        safetyManager = SafetyManager(this)
        shizukuManager = ShizukuManager(this)
        appRepository = AppRepository(this, safetyManager, shizukuManager)
        batchActionExecutor = BatchActionExecutor(this, shizukuManager, safetyManager)

        shizukuManager.initialize()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_BATCH,
                "BatchKit Operations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for batch app operations and scheduled profiles"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_BATCH = "batchkit_channel_operations"

        lateinit var instance: BatchKitApp
            private set
    }
}
