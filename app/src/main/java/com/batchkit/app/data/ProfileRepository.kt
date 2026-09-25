package com.batchkit.app.data

import com.batchkit.app.core.model.Profile
import com.batchkit.app.data.db.ProfileDao
import com.batchkit.app.data.db.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dao: ProfileDao) {

    val profiles: Flow<List<Profile>> = dao.observeAll().map { entities ->
        entities.map { it.toProfile() }
    }

    suspend fun find(id: Long): Profile? = dao.findById(id)?.toProfile()

    /** Inserts or updates, returning the row id. */
    suspend fun save(profile: Profile): Long = dao.upsert(ProfileEntity.from(profile))

    suspend fun delete(profile: Profile) = dao.deleteById(profile.id)

    suspend fun setSchedule(profileId: Long, enabled: Boolean, hour: Int, minute: Int) =
        dao.updateSchedule(
            id = profileId,
            enabled = enabled,
            hour = hour,
            minute = minute,
            updatedAt = System.currentTimeMillis(),
        )
}
