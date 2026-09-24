package com.batchkit.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.Profile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val packageNamesCsv: String,
    val actionsCsv: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toProfile(): Profile {
        val packages = if (packageNamesCsv.isBlank()) emptyList() else packageNamesCsv.split(",")
        val actionList = if (actionsCsv.isBlank()) {
            emptyList()
        } else {
            actionsCsv.split(",").mapNotNull { name ->
                try {
                    BatchActionType.valueOf(name.trim())
                } catch (e: Exception) {
                    null
                }
            }
        }
        return Profile(
            id = id,
            name = name,
            description = description,
            targetPackageNames = packages,
            actions = actionList,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromProfile(profile: Profile): ProfileEntity {
            return ProfileEntity(
                id = profile.id,
                name = profile.name,
                description = profile.description,
                packageNamesCsv = profile.targetPackageNames.joinToString(","),
                actionsCsv = profile.actions.joinToString(",") { it.name },
                createdAt = profile.createdAt
            )
        }
    }
}
