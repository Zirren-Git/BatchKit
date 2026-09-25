package com.batchkit.app

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.privileged.RunningAppsQuery
import com.batchkit.app.privileged.ShizukuPrivilegedExecutor
import com.batchkit.app.privileged.ShizukuShell
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku

/**
 * Instrumented smoke test for the Shizuku bridge.
 *
 * Every test is skipped (never failed) when Shizuku is missing, not running or has
 * not granted BatchKit its permission, so the suite is safe to run on any device.
 * The run book, including the wireless debugging steps, is in docs/TESTING.md.
 */
@RunWith(AndroidJUnit4::class)
class ShizukuSmokeTest {

    private val executor = ShizukuPrivilegedExecutor()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun selfTarget(): PrivilegedTarget {
        val packageName = context.packageName
        val uid = context.packageManager.getApplicationInfo(packageName, 0).uid
        return PrivilegedTarget(
            packageName = packageName,
            label = "BatchKit (self test)",
            uid = uid,
            userId = uid / PER_USER_RANGE,
        )
    }

    private fun assumeShizukuReady() {
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        assumeTrue("Shizuku is not running", running)
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        assumeTrue("Shizuku permission has not been granted to BatchKit", granted)
    }

    @Test
    fun shizukuBinderIsReachable() {
        assumeShizukuReady()
        assertThat(executor.isReady()).isTrue()
    }

    /** Covers the shell fallback path used by the battery whitelist and cache clear. */
    @Test
    fun shellBridgeRunsAsShellUser() {
        assumeShizukuReady()
        val result = ShizukuShell.run("id -u")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.stdout.trim()).isEqualTo("2000")
    }

    /** Blocking and restoring the background app op of BatchKit is reversible. */
    @Test
    fun appOpRoundTripIsReversible() {
        assumeShizukuReady()
        val target = selfTarget()

        val blocked = executor.execute(target, BatchAction.BLOCK_BACKGROUND)
        val restored = executor.execute(target, BatchAction.RESTORE_BACKGROUND)

        assertThat(blocked.success).isTrue()
        assertThat(restored.success).isTrue()
    }

    /** Read-only privileged call: the running app list is visible to the shell user. */
    @Test
    fun runningPackagesAreVisible() {
        assumeShizukuReady()
        val running = RunningAppsQuery.runningPackages()
        assertThat(running).isNotNull()
        assertThat(running).isNotEmpty()
    }

    /** The safety policy protects the packages the tests above deliberately avoid. */
    @Test
    fun protectedPackagesAreDetected() {
        val app = context.applicationContext as BatchKitApp
        val policy = app.container.protectedPackageResolver.resolve()
        assertThat(policy.keys).contains(app.packageName)
        assertThat(policy.keys).contains(ShizukuStatusPackage)
    }

    private companion object {
        const val PER_USER_RANGE = 100000
        const val ShizukuStatusPackage = "moe.shizuku.privileged.api"
    }
}
