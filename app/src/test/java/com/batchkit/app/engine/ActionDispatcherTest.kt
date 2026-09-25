package com.batchkit.app.engine

import com.batchkit.app.core.model.AppActionResult
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.OutcomeStatus
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.core.safety.ProtectedReason
import com.batchkit.app.core.safety.SafetyPolicy
import com.batchkit.app.privileged.PrivilegedExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ActionDispatcherTest {

    private class FakeExecutor(
        private val failingPackages: Set<String> = emptySet(),
        private val ready: Boolean = true,
    ) : PrivilegedExecutor {
        val executed = mutableListOf<String>()

        override fun isReady(): Boolean = ready

        override fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome {
            executed += target.packageName
            return if (target.packageName in failingPackages) {
                ExecutionOutcome.failure(FailureReason.SHELL_FAILED, "command failed")
            } else {
                ExecutionOutcome.ok()
            }
        }
    }

    /** Deterministic clock: five milliseconds per call. */
    private class FakeClock {
        private var current = 0L

        fun now(): Long {
            current += 5L
            return current
        }
    }

    private fun target(packageName: String) = PrivilegedTarget(
        packageName = packageName,
        label = packageName.substringAfterLast('.'),
        uid = 10_000,
        userId = 0,
    )

    @Test
    fun `every app is executed exactly once and in order`() = runBlocking {
        val executor = FakeExecutor()
        val dispatcher = ActionDispatcher(executor, SafetyPolicy(), FakeClock()::now)
        val targets = listOf(target("com.a.one"), target("com.b.two"), target("com.c.three"))

        val summary = dispatcher.dispatch(BatchAction.FORCE_STOP, targets)

        assertThat(executor.executed)
            .containsExactly("com.a.one", "com.b.two", "com.c.three")
            .inOrder()
        assertThat(summary.total).isEqualTo(3)
        assertThat(summary.succeeded).isEqualTo(3)
        assertThat(summary.failed).isEqualTo(0)
        assertThat(summary.results.map { it.status })
            .containsExactly(OutcomeStatus.SUCCESS, OutcomeStatus.SUCCESS, OutcomeStatus.SUCCESS)
            .inOrder()
    }

    @Test
    fun `progress is reported after every app`() = runBlocking {
        val dispatcher = ActionDispatcher(FakeExecutor(), SafetyPolicy(), FakeClock()::now)
        val observed = mutableListOf<Pair<Int, Int>>()

        dispatcher.dispatch(
            BatchAction.FREEZE,
            listOf(target("com.a.one"), target("com.b.two")),
        ) { _, done, total -> observed += done to total }

        assertThat(observed).containsExactly(1 to 2, 2 to 2).inOrder()
    }

    @Test
    fun `failures are reported per app with their reason`() = runBlocking {
        val executor = FakeExecutor(failingPackages = setOf("com.b.two"))
        val dispatcher = ActionDispatcher(executor, SafetyPolicy(), FakeClock()::now)

        val summary = dispatcher.dispatch(
            BatchAction.BLOCK_BACKGROUND,
            listOf(target("com.a.one"), target("com.b.two"), target("com.c.three")),
        )

        assertThat(summary.succeeded).isEqualTo(2)
        assertThat(summary.failed).isEqualTo(1)
        val failed = summary.results.single { it.status == OutcomeStatus.FAILED }
        assertThat(failed.packageName).isEqualTo("com.b.two")
        assertThat(failed.reason).isEqualTo(FailureReason.SHELL_FAILED)
        assertThat(failed.detail).isEqualTo("command failed")
        assertThat(summary.failedPackages).containsExactly("com.b.two")
    }

    @Test
    fun `protected packages are skipped without calling the executor`() = runBlocking {
        val executor = FakeExecutor()
        val policy = SafetyPolicy(
            protectedPackages = mapOf(
                "com.batchkit.app" to ProtectedReason.SELF,
                "com.android.launcher3" to ProtectedReason.LAUNCHER,
            ),
        )
        val dispatcher = ActionDispatcher(executor, policy, FakeClock()::now)

        val summary = dispatcher.dispatch(
            BatchAction.FREEZE,
            listOf(target("com.example.game"), target("com.batchkit.app"), target("com.android.launcher3")),
        )

        assertThat(executor.executed).containsExactly("com.example.game")
        assertThat(summary.succeeded).isEqualTo(1)
        assertThat(summary.skipped).isEqualTo(2)
        val skipped = summary.results.filter { it.status == OutcomeStatus.SKIPPED }
        assertThat(skipped.map { it.reason })
            .containsExactly(FailureReason.PROTECTED_PACKAGE, FailureReason.PROTECTED_PACKAGE)
    }

    @Test
    fun `device admin apps are reported with the admin reason`() = runBlocking {
        val executor = FakeExecutor()
        val policy = SafetyPolicy(deviceAdminPackages = listOf("com.example.mdm"))
        val dispatcher = ActionDispatcher(executor, policy, FakeClock()::now)

        val summary = dispatcher.dispatch(BatchAction.FORCE_STOP, listOf(target("com.example.mdm")))

        assertThat(executor.executed).isEmpty()
        assertThat(summary.results.single().reason).isEqualTo(FailureReason.DEVICE_ADMIN)
        assertThat(summary.results.single().status).isEqualTo(OutcomeStatus.SKIPPED)
    }

    @Test
    fun `stopping a batch marks the remaining apps as skipped`() = runBlocking {
        val executor = FakeExecutor()
        val dispatcher = ActionDispatcher(executor, SafetyPolicy(), FakeClock()::now)
        val targets = (1..5).map { target("com.example.app$it") }
        var summary: com.batchkit.app.core.model.BatchRunSummary? = null
        var job: Job? = null

        job = launch(Dispatchers.Default) {
            summary = dispatcher.dispatch(BatchAction.FORCE_STOP, targets) { _, done, _ ->
                if (done == 2) job?.cancel()
            }
        }
        job.join()

        val results = requireNotNull(summary).results
        assertThat(results.map { it.packageName })
            .containsExactly(
                "com.example.app1",
                "com.example.app2",
                "com.example.app3",
                "com.example.app4",
                "com.example.app5",
            )
            .inOrder()
        assertThat(results.take(2).map { it.status })
            .containsExactly(OutcomeStatus.SUCCESS, OutcomeStatus.SUCCESS)
        assertThat(results.drop(2).map { it.status })
            .containsExactly(OutcomeStatus.SKIPPED, OutcomeStatus.SKIPPED, OutcomeStatus.SKIPPED)
        assertThat(results.drop(2).map { it.reason })
            .containsExactly(
                FailureReason.CANCELLED,
                FailureReason.CANCELLED,
                FailureReason.CANCELLED,
            )
    }

    @Test
    fun `executor exceptions never escape the dispatcher`() = runBlocking {
        val executor = object : PrivilegedExecutor {
            override fun isReady(): Boolean = true
            override fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome =
                throw IllegalStateException("binder died")
        }
        val dispatcher = ActionDispatcher(executor, SafetyPolicy(), FakeClock()::now)

        val summary = dispatcher.dispatch(BatchAction.NOTIFICATIONS_OFF, listOf(target("com.a.one")))

        val result: AppActionResult = summary.results.single()
        assertThat(result.status).isEqualTo(OutcomeStatus.FAILED)
        assertThat(result.detail).isEqualTo("binder died")
    }

    @Test
    fun `summary aggregates mixed results`() = runBlocking {
        val executor = FakeExecutor(failingPackages = setOf("com.b.two"))
        val policy = SafetyPolicy(deviceAdminPackages = listOf("com.example.mdm"))
        val dispatcher = ActionDispatcher(executor, policy, FakeClock()::now)

        val summary = dispatcher.dispatch(
            BatchAction.FORCE_STOP,
            listOf(target("com.a.one"), target("com.b.two"), target("com.example.mdm")),
        )

        assertThat(summary.total).isEqualTo(3)
        assertThat(summary.succeeded).isEqualTo(1)
        assertThat(summary.failed).isEqualTo(1)
        assertThat(summary.skipped).isEqualTo(1)
        assertThat(summary.durationMs).isGreaterThan(0L)
    }
}
