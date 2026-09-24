package com.batchkit.app

import android.content.Context
import android.content.pm.PackageManager
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.domain.SafetyManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SafetyManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var safetyManager: SafetyManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageName).thenReturn("com.batchkit.app")
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        safetyManager = SafetyManager(mockContext)
    }

    @Test
    fun testSelfPackageCannotBeFrozen() {
        val result = safetyManager.checkSafety(
            packageName = "com.batchkit.app",
            action = BatchActionType.FREEZE,
            isSafeModeEnabled = false
        )
        assertFalse("BatchKit itself must never be frozen", result.isAllowed)
    }

    @Test
    fun testShizukuCannotBeFrozen() {
        val result = safetyManager.checkSafety(
            packageName = "moe.shizuku.privileged.api",
            action = BatchActionType.FREEZE,
            isSafeModeEnabled = false
        )
        assertFalse("Shizuku must never be frozen", result.isAllowed)
    }

    @Test
    fun testCoreSystemCannotBeFrozen() {
        val sysResult = safetyManager.checkSafety(
            packageName = "android",
            action = BatchActionType.FREEZE,
            isSafeModeEnabled = false
        )
        assertFalse("Android system core cannot be frozen", sysResult.isAllowed)

        val uiResult = safetyManager.checkSafety(
            packageName = "com.android.systemui",
            action = BatchActionType.FREEZE,
            isSafeModeEnabled = false
        )
        assertFalse("SystemUI cannot be frozen", uiResult.isAllowed)
    }

    @Test
    fun testSafeModeBlocksDestructiveActions() {
        val freezeResult = safetyManager.checkSafety(
            packageName = "com.example.userapp",
            action = BatchActionType.FREEZE,
            isSafeModeEnabled = true
        )
        assertFalse("Freeze requires safe mode disabled", freezeResult.isAllowed)

        val clearResult = safetyManager.checkSafety(
            packageName = "com.example.userapp",
            action = BatchActionType.CLEAR_CACHE,
            isSafeModeEnabled = true
        )
        assertFalse("Clear cache requires safe mode disabled", clearResult.isAllowed)
    }

    @Test
    fun testSafeModeAllowsForceStopAndAppOps() {
        val stopResult = safetyManager.checkSafety(
            packageName = "com.example.userapp",
            action = BatchActionType.FORCE_STOP,
            isSafeModeEnabled = true
        )
        assertTrue("Force stop is allowed in safe mode", stopResult.isAllowed)

        val blockResult = safetyManager.checkSafety(
            packageName = "com.example.userapp",
            action = BatchActionType.BLOCK_BACKGROUND,
            isSafeModeEnabled = true
        )
        assertTrue("Block background is allowed in safe mode", blockResult.isAllowed)
    }
}
