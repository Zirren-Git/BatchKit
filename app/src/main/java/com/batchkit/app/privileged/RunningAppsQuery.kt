package com.batchkit.app.privileged

import android.app.ActivityManager
import android.os.IBinder
import com.batchkit.app.core.codec.SelectionCodec
import rikka.shizuku.Shizuku

/**
 * "Currently running" needs ADB level visibility: since Android 5 a normal app
 * only sees its own processes. Shizuku runs as the shell user, which is allowed
 * to enumerate every process.
 */
object RunningAppsQuery {

    /** Returns null when the running state cannot be determined. */
    fun runningPackages(): Set<String>? = try {
        val stub = Class.forName("android.app.IActivityManager\$Stub")
        val binder: IBinder = Shizuku.getBinder() ?: return null
        val asInterface = stub.getMethod("asInterface", IBinder::class.java)
        val activityManager = asInterface.invoke(null, binder) ?: return null
        val method = activityManager.javaClass.methods.firstOrNull {
            it.name == "getRunningAppProcesses" && it.parameterCount == 0
        } ?: return null
        val processes = method.invoke(activityManager) as? List<*> ?: return null
        processes.mapNotNull { element ->
            (element as? ActivityManager.RunningAppProcessInfo)?.processName
        }
            .map { it.substringBefore(':') }
            .filter { SelectionCodec.isValidPackageName(it) }
            .toSet()
    } catch (t: Throwable) {
        null
    }
}
