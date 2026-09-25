package com.batchkit.app.core.safety

import com.batchkit.app.core.model.BatchAction
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SafetyPolicyTest {

    private val policy = SafetyPolicy(
        protectedPackages = mapOf(
            "com.batchkit.app" to ProtectedReason.SELF,
            "moe.shizuku.privileged.api" to ProtectedReason.SHIZUKU,
            "com.android.launcher3" to ProtectedReason.LAUNCHER,
            "com.android.systemui" to ProtectedReason.SYSTEM_CRITICAL,
            "com.example.keyboard" to ProtectedReason.INPUT_METHOD,
        ),
        deviceAdminPackages = listOf("com.example.mdm"),
    )

    @Test
    fun `freeze is blocked for every protected package`() {
        listOf(
            "com.batchkit.app",
            "moe.shizuku.privileged.api",
            "com.android.launcher3",
            "com.android.systemui",
            "com.example.keyboard",
        ).forEach { packageName ->
            val decision = policy.evaluate(BatchAction.FREEZE, packageName)
            assertThat(decision).isInstanceOf(SafetyDecision.Blocked::class.java)
        }
    }

    @Test
    fun `force stop is blocked for protected packages and device admins`() {
        assertThat(policy.isBlocked(BatchAction.FORCE_STOP, "com.batchkit.app")).isTrue()
        assertThat(policy.isBlocked(BatchAction.FORCE_STOP, "com.android.systemui")).isTrue()
        assertThat(policy.isBlocked(BatchAction.FORCE_STOP, "com.example.mdm")).isTrue()
        assertThat(policy.isBlocked(BatchAction.FORCE_STOP, "com.example.game")).isFalse()
    }

    @Test
    fun `background app ops stay allowed on protected packages but warn`() {
        val decision = policy.evaluate(BatchAction.BLOCK_BACKGROUND, "com.android.systemui")
        assertThat(decision).isInstanceOf(SafetyDecision.Warn::class.java)
        assertThat((decision as SafetyDecision.Warn).reason)
            .isEqualTo(ProtectedReason.SYSTEM_CRITICAL)
    }

    @Test
    fun `device admin apps cannot be frozen or force stopped`() {
        assertThat(policy.isBlocked(BatchAction.FREEZE, "com.example.mdm")).isTrue()
        assertThat(policy.isBlocked(BatchAction.FORCE_STOP, "com.example.mdm")).isTrue()
        // Clearing the cache of an admin app is harmless.
        assertThat(policy.isBlocked(BatchAction.CLEAR_CACHE, "com.example.mdm")).isFalse()
    }

    @Test
    fun `partition splits a selection into allowed and blocked apps`() {
        val (allowed, blocked) = policy.partition(
            BatchAction.FREEZE,
            listOf("com.example.a", "com.batchkit.app", "com.example.b", "com.android.launcher3"),
        )
        assertThat(allowed).containsExactly("com.example.a", "com.example.b").inOrder()
        assertThat(blocked.map { it.first })
            .containsExactly("com.batchkit.app", "com.android.launcher3")
            .inOrder()
    }

    @Test
    fun `select all can never freeze the launcher, the app itself or Shizuku`() {
        val everything = listOf(
            "com.example.one",
            "com.batchkit.app",
            "moe.shizuku.privileged.api",
            "com.android.launcher3",
            "com.example.two",
        )
        val (allowed, blocked) = policy.partition(BatchAction.FREEZE, everything)
        assertThat(allowed).containsExactly("com.example.one", "com.example.two")
        assertThat(blocked.map { it.first }).containsExactly(
            "com.batchkit.app",
            "moe.shizuku.privileged.api",
            "com.android.launcher3",
        )
    }

    @Test
    fun `unknown packages are allowed`() {
        assertThat(policy.evaluate(BatchAction.FORCE_STOP, "com.example.unknown"))
            .isEqualTo(SafetyDecision.Allowed)
    }
}
