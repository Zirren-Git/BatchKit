package com.batchkit.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.batchkit.app.core.codec.SelectionCodec
import com.batchkit.app.core.model.Profile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val packageList: String,
    val actionList: String,
    val createdAt: Long,
    val updatedAt: Long,
    val scheduleEnabled: Boolean = false,
    val scheduleHour: Int = 22,
    val scheduleMinute: Int = 0,
) {
    fun toProfile(): Profile = Profile(
        id = id,
        name = name,
        packages = SelectionCodec.decodePackages(packageList),
        actions = SelectionCodec.decodeActions(actionList),
        createdAt = createdAt,
        updatedAt = updatedAt,
        scheduleEnabled = scheduleEnabled,
        scheduleHour = scheduleHour,
        scheduleMinute = scheduleMinute,
    )

    companion object {
        fun from(profile: Profile, now: Long = System.currentTimeMillis()): ProfileEntity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            packageList = SelectionCodec.encodePackages(profile.packages),
            actionList = SelectionCodec.encodeActions(profile.actions),
            createdAt = if (profile.createdAt == 0L) now else profile.createdAt,
            updatedAt = now,
            scheduleEnabled = profile.scheduleEnabled,
            scheduleHour = profile.scheduleHour,
            scheduleMinute = profile.scheduleMinute,
        )
    }
}
