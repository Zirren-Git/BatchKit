package com.batchkit.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
abstract class BatchKitDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    companion object {
        private const val NAME = "batchkit.db"

        @Volatile
        private var instance: BatchKitDatabase? = null

        fun get(context: Context): BatchKitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BatchKitDatabase::class.java, NAME)
                .build()
                .also { instance = it }
        }
    }
}
