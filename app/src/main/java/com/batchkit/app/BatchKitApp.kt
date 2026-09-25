package com.batchkit.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.batchkit.app.di.AppContainer

class BatchKitApp : Application() {

    /**
     * The dependency graph is built at most once, and a failure while building it
     * is captured instead of thrown: the activity shows what went wrong rather
     * than the process dying before the first frame.
     */
    private val containerResult: Result<AppContainer> by lazy { runCatching { AppContainer(this) } }

    /** The container, or null when a start-up component could not be created. */
    val containerOrNull: AppContainer? get() = containerResult.getOrNull()

    /** Why [containerOrNull] is null, if it is. */
    val containerError: Throwable? get() = containerResult.exceptionOrNull()

    /** The container for call sites that cannot continue without one. */
    val container: AppContainer
        get() = containerResult.getOrElse { error ->
            throw IllegalStateException("BatchKit could not start", error)
        }

    override fun onCreate() {
        super.onCreate()
        // Shizuku hands its binder to the process that hosts ShizukuProvider, which
        // is the main process. Every other process that wants the binder has to ask
        // for multi-process support, so this runs in every process.
        runCatching { enableShizukuMultiProcessSupport() }
            .onFailure { Log.w(TAG, "Shizuku multi-process support is not available", it) }

        // Hidden API exemptions are deliberately NOT installed here: nothing in the
        // UI process calls a hidden API (every privileged call goes through the
        // :privileged process, which installs its own exemptions).
        if (isMainProcess()) {
            runCatching { containerOrNull?.shizukuStatusProvider?.start() }
                .onFailure { Log.w(TAG, "The Shizuku status provider could not start", it) }
            containerError?.let { error ->
                Log.e(TAG, "BatchKit could not build its start-up components", error)
            }
        }
    }

    /**
     * Opts this process into Shizuku's multi-process binder sharing. Reflective
     * because older Shizuku API releases do not expose the call: without it the app
     * still works, the binder is just limited to the process that hosts the provider.
     */
    private fun enableShizukuMultiProcessSupport() {
        val provider = Class.forName("rikka.shizuku.ShizukuProvider")
        val withArgument = runCatching {
            provider.getMethod("enableMultiProcessSupport", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        }
        if (withArgument.isSuccess) return
        // Older releases expose it without the boolean.
        provider.getMethod("enableMultiProcessSupport").invoke(null)
    }

    /** The UI, the tile and the workers live here; the `:privileged` process does not need any of them. */
    private fun isMainProcess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            packageName == Application.getProcessName()

    private companion object {
        const val TAG = "BatchKit/App"
    }
}
