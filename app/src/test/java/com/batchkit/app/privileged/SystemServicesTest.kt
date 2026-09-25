package com.batchkit.app.privileged

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The privileged calls only work when each AIDL stub is paired with the name the
 * system publishes that service under. Getting that pair wrong makes every action
 * fail at runtime with a message that looks unrelated, so it is pinned here.
 */
class SystemServicesTest {

    @Test
    fun `every stub maps to a system service`() {
        val services = SystemServices.all()

        assertThat(services).hasSize(5)
        assertThat(services.keys).allMatch { it.endsWith("\$Stub") }
        assertThat(services.keys).allMatch { it.startsWith("android.") || it.startsWith("com.android.internal.") }
        assertThat(services.values).containsExactly("activity", "package", "appops", "deviceidle", "notification")
    }

    @Test
    fun `a stub that is not mapped has no service name`() {
        assertThat(SystemServices.nameFor("android.app.INotificationManager")).isNull()
        assertThat(SystemServices.nameFor("com.example.NotAStub")).isNull()
    }

    /**
     * Guards against a new call site being added without a mapping: every stub the
     * executor looks up has to appear in [SystemServices].
     *
     * The test reads the source instead of the bytecode on purpose - it is about the
     * literal string in `remoteInterface(...)` calls. It is skipped when the file
     * cannot be found (for example when the test runs from a different working
     * directory), so it can never turn into a false failure.
     */
    @Test
    fun `every stub used by the executor is mapped`() {
        val source = File("src/main/java/com/batchkit/app/privileged/ShizukuPrivilegedExecutor.kt")
        assumeTrue("executor source not readable from ${source.absolutePath}", source.isFile)

        val used = Regex("remoteInterface\\(\"([^\"]+)\"")
            .findAll(source.readText())
            .map { it.groupValues[1] }
            .toSet()

        assertThat(used).isNotEmpty()
        used.forEach { stub ->
            assertThat(SystemServices.nameFor(stub)).isNotNull()
        }
    }
}
