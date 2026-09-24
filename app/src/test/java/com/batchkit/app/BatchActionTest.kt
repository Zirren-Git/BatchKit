package com.batchkit.app

import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.SingleActionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchActionTest {

    @Test
    fun testActionDestructiveFlags() {
        assertTrue(BatchActionType.FREEZE.isDestructive)
        assertTrue(BatchActionType.CLEAR_CACHE.isDestructive)

        assertFalse(BatchActionType.FORCE_STOP.isDestructive)
        assertFalse(BatchActionType.BLOCK_BACKGROUND.isDestructive)
        assertFalse(BatchActionType.RESTORE_BACKGROUND.isDestructive)
        assertFalse(BatchActionType.UNFREEZE.isDestructive)
    }

    @Test
    fun testBatchExecutionReportCounts() {
        val results = listOf(
            SingleActionResult("pkg1", "App 1", BatchActionType.FORCE_STOP, isSuccess = true),
            SingleActionResult("pkg2", "App 2", BatchActionType.FORCE_STOP, isSuccess = true),
            SingleActionResult("pkg3", "App 3", BatchActionType.FORCE_STOP, isSuccess = false, message = "Failed to kill"),
            SingleActionResult("pkg4", "App 4", BatchActionType.FORCE_STOP, isSuccess = false, isSkipped = true, message = "Protected")
        )

        val report = BatchExecutionReport(
            actionType = BatchActionType.FORCE_STOP,
            results = results
        )

        assertEquals(4, report.totalCount)
        assertEquals(2, report.successCount)
        assertEquals(1, report.failureCount)
        assertEquals(1, report.skippedCount)
    }
}
