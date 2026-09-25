package com.batchkit.app.privileged

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.batchkit.app.core.model.ShizukuStatus
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper

/**
 * Turns "the actions fail" into something precise.
 *
 * The self-test runs in the process it is called from and reports everything that
 * can break the privileged path: whether the Shizuku binder actually arrived in
 * *this* process, whether the permission is granted, whether the hidden API
 * exemptions are installed, whether every system service BatchKit needs can be
 * looked up, and whether the shell bridge works.
 *
 * Both processes are interesting, and they fail differently: the UI process can
 * look perfectly healthy while the `:privileged` process never received a binder.
 * That is exactly the bug this test was written for.
 */
object PrivilegedDiagnostics {

    fun run(context: Context?): String = buildString {
        appendLine("process: ${processName()}")
        appendLine("android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("shizuku installed: ${isShizukuInstalled(context)}")
        appendLine("shizuku binder: ${if (ping()) "alive" else "dead"}")

        val version = runCatching { Shizuku.getVersion() }.getOrDefault(-1)
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        appendLine("shizuku version: ${if (version > 0) version else "unknown"}")
        appendLine(
            "shizuku uid: " + when {
                uid == 0 -> "0 (root)"
                uid > 0 -> "$uid (shell)"
                else -> "unknown"
            },
        )

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        appendLine("permission: ${if (granted) "granted" else "not granted"}")

        // Installing here is deliberate: it is what this process needs to make the
        // reflective calls below, and it reports whether the bypass is available.
        appendLine("hidden api exemptions: ${HiddenApiBootstrap.install()}")

        appendLine("system services:")
        SystemServices.all().forEach { (stub, service) ->
            val name = stub.substringAfterLast('.').removeSuffix("\$Stub")
            val state = runCatching {
                // Only the lookup is exercised: it is the step that fails when the
                // binder never arrived or an OEM ROM publishes a different service.
                SystemServiceHelper.getSystemService(service)
                    ?: error("service returned null")
                "$name ok"
            }.getOrElse { error ->
                "$name FAILED: ${error.javaClass.simpleName}: ${error.message}"
            }
            appendLine("  $service -> $state")
        }

        val shell = ShizukuShell.run("id -u")
        appendLine(
            "shell bridge: " + if (shell.isSuccess) {
                "ok, uid ${shell.stdout.trim()}"
            } else {
                "FAILED: ${shell.error ?: shell.stderr.ifBlank { "exit ${shell.exitCode}" }}"
            },
        )
    }

    private fun processName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { Application.getProcessName() }.getOrDefault("unknown")
        } else {
            "unknown"
        }

    private fun ping(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    private fun isShizukuInstalled(context: Context?): Boolean {
        if (context == null) return false
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(ShizukuStatus.SHIZUKU_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }
}
