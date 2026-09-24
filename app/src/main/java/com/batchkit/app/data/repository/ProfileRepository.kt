package com.batchkit.app.data.repository

import com.batchkit.app.data.local.ProfileDao
import com.batchkit.app.data.local.ProfileEntity
import com.batchkit.app.data.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val profileDao: ProfileDao) {

    val profilesFlow: Flow<List<Profile>> = profileDao.getAllProfilesFlow().map { entities ->
        entities.map { it.toProfile() }
    }

    suspend fun getAllProfiles(): List<Profile> {
        return profileDao.getAllProfiles().map { it.toProfile() }
    }

    suspend fun getProfileById(id: Long): Profile? {
        return profileDao.getProfileById(id)?.toProfile()
    }

    suspend fun saveProfile(profile: Profile): Long {
        return profileDao.insertProfile(ProfileEntity.fromProfile(profile))
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(ProfileEntity.fromProfile(profile))
    }

    suspend fun deleteProfileById(id: Long) {
        profileDao.deleteProfileById(id)
    }
}
