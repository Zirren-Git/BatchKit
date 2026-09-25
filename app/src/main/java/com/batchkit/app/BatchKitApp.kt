package com.batchkit.app

import android.app.Application
import android.util.Log
import com.batchkit.app.di.AppContainer
import com.batchkit.app.privileged.HiddenApiBootstrap

class BatchKitApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        enableShizukuMultiProcessSupport()
        HiddenApiBootstrap.install()
        container.shizukuStatusProvider.start()
    }

    /**
     * BatchKit executes privileged calls in a dedicated `:privileged` process.
     * Shizuku only hands the binder to the process that hosts its provider, so
     * multi process support has to be requested explicitly. The call is reflective
     * because older Shizuku API releases do not expose it: without it the app
     * still works, it just runs the calls in this process.
     */
    private fun enableShizukuMultiProcessSupport() {
        try {
            Class.forName("rikka.shizuku.ShizukuProvider")
                .getMethod("enableMultiProcessSupport", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (t: Throwable) {
            Log.w(TAG, "Shizuku multi-process support is not available", t)
        }
    }

    private companion object {
        const val TAG = "BatchKit/App"
    }
}
