package com.batchkit.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.batchkit.app.domain.ShizukuManager
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku

/**
 * Instrumented smoke test for Shizuku connection flow.
 *
 * How to run with Wireless Debugging:
 * 1. Pair and start Shizuku on your test device via Wireless Debugging (Settings > Developer Options > Wireless Debugging).
 * 2. Connect adb over WiFi: `adb connect <device-ip>:<port>`.
 * 3. Run the instrumented test:
 *    `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.batchkit.app.ShizukuSmokeTest`
 */
@RunWith(AndroidJUnit4::class)
class ShizukuSmokeTest {

    @Test
    fun testAppContextAndShizukuInitialization() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Target context must not be null", appContext)

        val shizukuManager = ShizukuManager(appContext)
        shizukuManager.initialize()

        val info = shizukuManager.shizukuInfo.value
        assertNotNull("Shizuku info state flow must provide an initial state", info)

        // If Shizuku is running during instrumented test on device, verify binder ping
        val isBinderAlive = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

        println("Smoke Test: Shizuku binder ping result = $isBinderAlive, status = ${info.status}")
        shizukuManager.cleanup()
    }
}
