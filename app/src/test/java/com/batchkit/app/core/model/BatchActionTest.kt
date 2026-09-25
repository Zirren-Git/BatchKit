package com.batchkit.app.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BatchActionTest {

    @Test
    fun `action ids are unique and stable`() {
        val ids = BatchAction.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
        assertThat(ids).containsExactly(
            "force_stop",
            "block_background",
            "block_any_background",
            "restore_background",
            "freeze",
            "unfreeze",
            "battery_exempt",
            "battery_uneempt",
            "clear_cache",
            "notifications_off",
            "notifications_on",
        )
    }

    @Test
    fun `safe mode only offers the reversible background actions`() {
        assertThat(BatchAction.safeModeActions).containsExactly(
            BatchAction.FORCE_STOP,
            BatchAction.BLOCK_BACKGROUND,
            BatchAction.BLOCK_ANY_BACKGROUND,
            BatchAction.RESTORE_BACKGROUND,
        )
        assertThat(BatchAction.safeModeActions).containsNoneOf(BatchAction.FREEZE, BatchAction.CLEAR_CACHE)
    }

    @Test
    fun `freezing and clearing the cache are destructive but reversible`() {
        listOf(BatchAction.FREEZE, BatchAction.CLEAR_CACHE).forEach { action ->
            assertThat(action.isDestructive).isTrue()
            assertThat(action.isReversible).isTrue()
        }
    }

    @Test
    fun `every action can be resolved from its persisted id`() {
        BatchAction.entries.forEach { action ->
            assertThat(BatchAction.fromId(action.id)).isEqualTo(action)
        }
        assertThat(BatchAction.fromId("nope")).isNull()
    }

    @Test
    fun `freeze and force stop are the actions that need protection`() {
        assertThat(BatchAction.FREEZE.blockedForProtectedPackages).isTrue()
        assertThat(BatchAction.FORCE_STOP.blockedForProtectedPackages).isTrue()
        assertThat(BatchAction.UNFREEZE.blockedForProtectedPackages).isFalse()
        assertThat(BatchAction.BLOCK_BACKGROUND.blockedForProtectedPackages).isFalse()
    }

    @Test
    fun `safe mode settings hide actions that need an explicit unlock`() {
        val safeDefaults = AppSettings()
        assertThat(safeDefaults.allows(BatchAction.FORCE_STOP)).isTrue()
        assertThat(safeDefaults.allows(BatchAction.FREEZE)).isFalse()
        assertThat(safeDefaults.allows(BatchAction.CLEAR_CACHE)).isFalse()

        val unlocked = safeDefaults.copy(advancedUnlocked = true)
        assertThat(unlocked.allows(BatchAction.FREEZE)).isTrue()
        assertThat(unlocked.visibleActions()).contains(BatchAction.FREEZE)
    }
}
