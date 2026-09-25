package com.batchkit.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity): Long

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE profiles SET scheduleEnabled = :enabled, scheduleHour = :hour, scheduleMinute = :minute, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSchedule(id: Long, enabled: Boolean, hour: Int, minute: Int, updatedAt: Long)
}
