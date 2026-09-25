package com.batchkit.app.privileged

import android.util.Log

/**
 * Installs the hidden API exemptions BatchKit needs to reach the system binder
 * interfaces ([android.app.IActivityManager], [android.content.pm.IPackageManager],
 * [com.android.internal.app.IAppOpsService], ...).
 *
 * The LSPosed `hiddenapibypass` library is invoked reflectively on purpose: the
 * app keeps working (with a log warning) even if the library is not present or
 * its API changes between releases.
 */
object HiddenApiBootstrap {

    private const val TAG = "BatchKit/HiddenApi"
    private const val BYPASS_CLASS = "org.lsposed.hiddenapibypass.HiddenApiBypass"

    @Volatile
    private var installed = false

    fun install(): Boolean {
        if (installed) return true
        return try {
            val bypass = Class.forName(BYPASS_CLASS)
            // An empty prefix exempts every hidden member, which is what the
            // Shizuku documentation recommends for privileged clients.
            val add = bypass.getMethod("addHiddenApiExemptions", Array<String>::class.java)
            add.invoke(null, arrayOf(""))
            installed = true
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Hidden API exemptions unavailable, falling back to plain reflection", t)
            installed = false
            false
        }
    }
}
