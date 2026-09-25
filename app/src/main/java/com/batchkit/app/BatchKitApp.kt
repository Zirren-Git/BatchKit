package com.batchkit.app

import android.app.Application
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
        runCatching { enableShizukuMultiProcessSupport() }
            .onFailure { Log.w(TAG, "Shizuku multi-process support is not available", it) }

        // Start-up must never take the app down. Note that the hidden API exemptions
        // are deliberately NOT installed here: nothing in the UI process calls a
        // hidden API (every privileged call goes through the :privileged process,
        // which installs the exemptions itself). Reflecting into platform internals
        // from the main process buys nothing and is the kind of call that a newer
        // platform release can reject outright.
        runCatching { containerOrNull?.shizukuStatusProvider?.start() }
            .onFailure { Log.w(TAG, "The Shizuku status provider could not start", it) }
    }

    /**
     * BatchKit executes privileged calls in a dedicated `:privileged` process.
     * Shizuku only hands the binder to the process that hosts its provider, so
     * multi process support has to be requested explicitly. The call is reflective
     * because older Shizuku API releases do not expose it: without it the app
     * still works, it just runs the calls in this process.
     */
    private fun enableShizukuMultiProcessSupport() {
        Class.forName("rikka.shizuku.ShizukuProvider")
            .getMethod("enableMultiProcessSupport", Boolean::class.javaPrimitiveType)
            .invoke(null, true)
    }

    private companion object {
        const val TAG = "BatchKit/App"
    }
}
