package com.batchkit.app.privileged

import com.google.common.truth.Truth.assertThat
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
        assertThat(services).containsKey(SystemServices.ACTIVITY_MANAGER)
        assertThat(services).containsKey(SystemServices.PACKAGE_MANAGER)
        assertThat(services).containsKey(SystemServices.APP_OPS)
        assertThat(services).containsKey(SystemServices.DEVICE_IDLE)
        assertThat(services).containsKey(SystemServices.NOTIFICATION_MANAGER)
        assertThat(services.values).containsExactly("activity", "package", "appops", "deviceidle", "notification")
        services.keys.forEach { stub ->
            assertThat(stub).endsWith("\$Stub")
            assertThat(stub.startsWith("android.") || stub.startsWith("com.android.internal.")).isTrue()
        }
    }

    @Test
    fun `a stub that is not mapped has no service name`() {
        assertThat(SystemServices.nameFor("android.app.INotificationManager")).isNull()
        assertThat(SystemServices.nameFor("com.example.NotAStub")).isNull()
    }
}
