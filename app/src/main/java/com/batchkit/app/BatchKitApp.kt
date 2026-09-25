package com.batchkit.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.batchkit.app.di.AppContainer
import rikka.shizuku.ShizukuProvider

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
        runCatching { setupShizukuBinderSharing() }
            .onFailure { Log.w(TAG, "Shizuku binder sharing is not available", it) }

        // Hidden API exemptions are deliberately NOT installed here: the executor
        // installs them in whatever process it runs in, and it normally runs in
        // :privileged. The UI process only does that if it has to fall back.
        if (isMainProcess()) {
            runCatching { containerOrNull?.shizukuStatusProvider?.start() }
                .onFailure { Log.w(TAG, "The Shizuku status provider could not start", it) }
            containerError?.let { error ->
                Log.e(TAG, "BatchKit could not build its start-up components", error)
            }
        }
    }

    /**
     * Sets up Shizuku's binder sharing between this app's processes.
     *
     * Shizuku hands its binder to the process that hosts ShizukuProvider, which is
     * the main process. The boolean of `enableMultiProcessSupport` does NOT mean
     * "enable": it is `isProviderProcess`, i.e. which side of that split this process
     * is on. A non-provider process additionally has to ask the provider for the
     * binder. Calling it with `true` everywhere - as this app did - told every
     * process it already held a binder, so the `:privileged` process ran without
     * one and every single action failed as "Shizuku is not available".
     */
    private fun setupShizukuBinderSharing() {
        val providerProcess = isMainProcess()
        ShizukuProvider.enableMultiProcessSupport(providerProcess)
        if (!providerProcess) {
            ShizukuProvider.requestBinderForNonProviderProcess(this)
        }
    }

    /** The UI, the tile and the workers live here; the `:privileged` process does not need any of them. */
    private fun isMainProcess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            packageName == Application.getProcessName()

    private companion object {
        const val TAG = "BatchKit/App"
    }
}
