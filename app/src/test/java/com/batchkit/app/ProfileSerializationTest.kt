package com.batchkit.app

import com.batchkit.app.data.local.ProfileEntity
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSerializationTest {

    @Test
    fun testProfileEntityConversion() {
        val original = Profile(
            id = 42L,
            name = "Night Chill",
            description = "Freeze social apps",
            targetPackageNames = listOf("com.twitter.android", "com.instagram.android"),
            actions = listOf(BatchActionType.FORCE_STOP, BatchActionType.BLOCK_BACKGROUND)
        )

        val entity = ProfileEntity.fromProfile(original)
        val restored = entity.toProfile()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.description, restored.description)
        assertEquals(original.targetPackageNames, restored.targetPackageNames)
        assertEquals(original.actions, restored.actions)
    }
}
