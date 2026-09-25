package com.batchkit.app.privileged

import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.batchkit.app.core.codec.SelectionCodec
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.PrivilegedTarget
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import rikka.shizuku.Shizuku

/**
 * Applies batch actions through the Shizuku binder, which runs with the identity
 * of the ADB shell user (uid 2000).
 *
 * Every hidden interface is reached through reflection so that BatchKit works
 * across Android versions and OEM ROMs: if a method is missing or renamed, the
 * call fails with a precise reason instead of crashing the app.
 */
class ShizukuPrivilegedExecutor : PrivilegedExecutor {

    private val interfaces = HashMap<String, Any>()

    override fun isReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    override fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome {
        if (!isReady()) {
            return ExecutionOutcome.failure(FailureReason.SHIZUKU_UNAVAILABLE)
        }
        return try {
            when (action) {
                BatchAction.FORCE_STOP -> forceStop(target)
                BatchAction.BLOCK_BACKGROUND -> setAppOp(target, OPSTR_RUN_IN_BACKGROUND, OP_RUN_IN_BACKGROUND, MODE_IGNORED)
                BatchAction.BLOCK_ANY_BACKGROUND -> setAppOp(target, OPSTR_RUN_ANY_IN_BACKGROUND, OP_RUN_ANY_IN_BACKGROUND, MODE_IGNORED)
                BatchAction.RESTORE_BACKGROUND -> restoreBackground(target)
                BatchAction.FREEZE -> setApplicationEnabled(target, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                BatchAction.UNFREEZE -> setApplicationEnabled(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                BatchAction.BATTERY_EXEMPT -> setBatteryExempt(target, exempt = true)
                BatchAction.BATTERY_UNEXEMPT -> setBatteryExempt(target, exempt = false)
                BatchAction.CLEAR_CACHE -> clearCache(target)
                BatchAction.NOTIFICATIONS_OFF -> setNotificationsEnabled(target, enabled = false)
                BatchAction.NOTIFICATIONS_ON -> setNotificationsEnabled(target, enabled = true)
            }
        } catch (t: Throwable) {
            val reason = classify(t)
            Log.w(TAG, "Action ${action.id} failed for ${target.packageName}: $reason", t)
            ExecutionOutcome.failure(reason, t.message ?: t.javaClass.simpleName)
        }
    }

    // ---------------------------------------------------------------- actions

    private fun forceStop(target: PrivilegedTarget): ExecutionOutcome {
        val activityManager = remoteInterface("android.app.IActivityManager\$Stub")
        invokeAny(
            activityManager,
            "forceStopPackage",
            arrayOf<Any?>(target.packageName, target.userId),
            arrayOf<Any?>(target.packageName),
        )
        return ExecutionOutcome.ok()
    }

    private fun restoreBackground(target: PrivilegedTarget): ExecutionOutcome {
        val first = setAppOp(target, OPSTR_RUN_IN_BACKGROUND, OP_RUN_IN_BACKGROUND, MODE_DEFAULT)
        val second = setAppOp(target, OPSTR_RUN_ANY_IN_BACKGROUND, OP_RUN_ANY_IN_BACKGROUND, MODE_DEFAULT)
        if (first.success && second.success) return ExecutionOutcome.ok()
        // Some ROMs refuse MODE_DEFAULT and require an explicit allow.
        val retryFirst = if (first.success) first else setAppOp(target, OPSTR_RUN_IN_BACKGROUND, OP_RUN_IN_BACKGROUND, MODE_ALLOWED)
        val retrySecond = if (second.success) second else setAppOp(target, OPSTR_RUN_ANY_IN_BACKGROUND, OP_RUN_ANY_IN_BACKGROUND, MODE_ALLOWED)
        return if (retryFirst.success && retrySecond.success) {
            ExecutionOutcome.ok()
        } else {
            val failing = if (!retryFirst.success) retryFirst else retrySecond
            ExecutionOutcome.failure(failing.reason, failing.detail)
        }
    }

    private fun setAppOp(
        target: PrivilegedTarget,
        opString: String,
        fallbackOp: Int,
        mode: Int,
    ): ExecutionOutcome {
        val service = remoteInterface("com.android.internal.app.IAppOpsService\$Stub")
        val op = opNumber(opString, fallbackOp)
        var failure: Throwable? = null

        // Preferred: per package override, the same switch the system Settings app flips.
        try {
            invokeRemote(service, "setMode", op, target.uid, target.packageName, mode)
            return reportAppOp(service, op, target, mode, opString)
        } catch (t: Throwable) {
            failure = t
        }
        // Older or stricter ROMs only accept a uid wide mode.
        try {
            invokeRemote(service, "setUidMode", op, target.uid, mode)
            return reportAppOp(service, op, target, mode, opString)
        } catch (t: Throwable) {
            if (failure == null) failure = t
        }
        return ExecutionOutcome.failure(classify(failure), failure?.message)
    }

    /** Reads the app op back so the results screen shows the state that actually applies. */
    private fun reportAppOp(
        service: Any,
        op: Int,
        target: PrivilegedTarget,
        requestedMode: Int,
        opString: String,
    ): ExecutionOutcome {
        val observed = try {
            invokeRemote(service, "checkOperation", op, target.uid, target.packageName) as? Int
        } catch (t: Throwable) {
            null
        }
        if (observed == null) {
            return ExecutionOutcome.ok()
        }
        val acceptable = if (requestedMode == MODE_IGNORED) observed == MODE_IGNORED else observed != MODE_IGNORED
        val detail = "$opString now reports mode $observed"
        return if (acceptable) {
            ExecutionOutcome(success = true, detail = detail)
        } else {
            ExecutionOutcome.failure(FailureReason.NOT_SUPPORTED, detail)
        }
    }

    private fun setApplicationEnabled(target: PrivilegedTarget, newState: Int): ExecutionOutcome {
        val packageManager = remoteInterface("android.content.pm.IPackageManager\$Stub")
        val method = findMethod(packageManager, "setApplicationEnabledSetting", 5)
            ?: findMethod(packageManager, "setApplicationEnabledSetting", 4)
            ?: throw NoSuchMethodException("setApplicationEnabledSetting")
        val arguments: Array<Any?> = if (method.parameterCount == 5) {
            arrayOf(target.packageName, newState, 0, target.userId, target.packageName)
        } else {
            arrayOf(target.packageName, newState, 0, target.userId)
        }
        method.invoke(packageManager, *adapt(method.parameterTypes, arguments))
        return ExecutionOutcome.ok()
    }

    private fun setBatteryExempt(target: PrivilegedTarget, exempt: Boolean): ExecutionOutcome {
        val methodName = if (exempt) "addPowerSaveWhitelistApp" else "removePowerSaveWhitelistApp"
        try {
            val controller = remoteInterface("android.os.IDeviceIdleController\$Stub")
            invokeRemote(controller, methodName, target.packageName)
            return ExecutionOutcome.ok()
        } catch (t: Throwable) {
            // Fall back to the shell equivalent of: cmd deviceidle whitelist +pkg
            if (!SelectionCodec.isValidPackageName(target.packageName)) throw t
            val flag = if (exempt) "+" else "-"
            val result = ShizukuShell.run("cmd deviceidle whitelist $flag${target.packageName}")
            return if (result.isSuccess) {
                ExecutionOutcome.ok()
            } else {
                ExecutionOutcome.failure(
                    FailureReason.SHELL_FAILED,
                    (result.error ?: result.stderr).ifBlank { t.message ?: "deviceidle failed" },
                )
            }
        }
    }

    private fun clearCache(target: PrivilegedTarget): ExecutionOutcome {
        // deleteApplicationCacheFiles() is the same call the system Settings app makes.
        try {
            val packageManager = remoteInterface("android.content.pm.IPackageManager\$Stub")
            invokeRemote(packageManager, "deleteApplicationCacheFiles", target.packageName, null)
            return ExecutionOutcome.ok()
        } catch (t: Throwable) {
            // Fallback: cache only clear through the package manager CLI. Never
            // call plain "pm clear", that would also wipe user data.
            if (!SelectionCodec.isValidPackageName(target.packageName)) throw t
            val result = ShizukuShell.run("pm clear --cache-only ${target.packageName}")
            val output = "${result.stdout}${result.stderr}".trim()
            return if (result.isSuccess && output.contains("Success", ignoreCase = true)) {
                ExecutionOutcome.ok()
            } else {
                ExecutionOutcome.failure(
                    FailureReason.SHELL_FAILED,
                    output.ifBlank { result.error ?: t.message ?: "cache clear failed" },
                )
            }
        }
    }

    private fun setNotificationsEnabled(target: PrivilegedTarget, enabled: Boolean): ExecutionOutcome {
        try {
            val manager = remoteInterface("android.app.INotificationManager\$Stub")
            invokeAny(
                manager,
                "setNotificationsEnabledForPackage",
                arrayOf<Any?>(target.packageName, target.uid, enabled),
                arrayOf<Any?>(target.packageName, enabled),
            )
            return ExecutionOutcome.ok()
        } catch (t: Throwable) {
            // Last resort: the notification app op blocks delivery without touching
            // the user visible per channel settings.
            val mode = if (enabled) MODE_ALLOWED else MODE_IGNORED
            val outcome = setAppOp(target, OPSTR_POST_NOTIFICATION, OP_POST_NOTIFICATION, mode)
            return if (outcome.success) outcome else ExecutionOutcome.failure(classify(t), t.message)
        }
    }

    // -------------------------------------------------------------- reflection

    private fun remoteInterface(stubClassName: String): Any = synchronized(interfaces) {
        interfaces[stubClassName] ?: createInterface(stubClassName).also { created ->
            interfaces[stubClassName] = created
        }
    }

    private fun createInterface(stubClassName: String): Any {
        val stub = Class.forName(stubClassName)
        val binder: IBinder = Shizuku.getBinder()
            ?: throw IllegalStateException("Shizuku binder is not available")
        val asInterface: Method = stub.getMethod("asInterface", IBinder::class.java)
        return asInterface.invoke(null, binder)
            ?: throw IllegalStateException("asInterface returned null for $stubClassName")
    }

    private fun invokeRemote(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = findMethod(target, methodName, args.size)
            ?: throw NoSuchMethodException("$methodName with ${args.size} argument(s)")
        return method.invoke(target, *adapt(method.parameterTypes, args))
    }

    /** Tries several argument shapes of the same method, oldest last. */
    private fun invokeAny(target: Any, methodName: String, vararg candidates: Array<Any?>): Any? {
        var lastError: Throwable? = null
        candidates.forEach { candidate ->
            try {
                return invokeRemote(target, methodName, *candidate)
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: NoSuchMethodException(methodName)
    }

    private fun findMethod(target: Any, name: String, arity: Int): Method? {
        val methods = target.javaClass.methods
        methods.firstOrNull { it.name == name && it.parameterCount == arity && !it.isSynthetic }
            ?.let { return it }
        return methods.firstOrNull { it.name == name && it.parameterCount == arity }
    }

    private fun adapt(types: Array<Class<*>>, args: Array<out Any?>): Array<Any?> =
        Array(types.size) { index -> convert(types[index], args.getOrNull(index)) }

    private fun convert(type: Class<*>, value: Any?): Any? = when {
        value == null -> if (type.isPrimitive) primitiveDefault(type) else null
        type.isInstance(value) -> value
        type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType -> (value as? Number)?.toInt()
        type == Long::class.javaPrimitiveType || type == Long::class.javaObjectType -> (value as? Number)?.toLong()
        type == Boolean::class.javaPrimitiveType || type == Boolean::class.javaObjectType -> value as? Boolean
        type == String::class.java -> value.toString()
        else -> value
    }

    private fun primitiveDefault(type: Class<*>): Any? = when (type) {
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Boolean::class.javaPrimitiveType -> false
        Short::class.javaPrimitiveType -> 0.toShort()
        Byte::class.javaPrimitiveType -> 0.toByte()
        Char::class.javaPrimitiveType -> ' '
        Float::class.javaPrimitiveType -> 0f
        Double::class.javaPrimitiveType -> 0.0
        else -> null
    }

    private fun opNumber(opString: String, fallbackOp: Int): Int = try {
        val appOpsManager = Class.forName("android.app.AppOpsManager")
        val strOpToOp = appOpsManager.getMethod("strOpToOp", String::class.java)
        (strOpToOp.invoke(null, opString) as? Int)?.takeIf { it >= 0 } ?: fallbackOp
    } catch (t: Throwable) {
        fallbackOp
    }

    private fun classify(error: Throwable?): FailureReason {
        val cause = unwrap(error) ?: return FailureReason.UNKNOWN
        return when {
            cause is NoSuchMethodException -> FailureReason.METHOD_NOT_FOUND
            cause is ClassNotFoundException -> FailureReason.METHOD_NOT_FOUND
            cause is NoClassDefFoundError -> FailureReason.METHOD_NOT_FOUND
            cause is SecurityException -> FailureReason.PERMISSION_DENIED
            cause is UnsupportedOperationException -> FailureReason.NOT_SUPPORTED
            cause is RemoteException -> FailureReason.REMOTE_ERROR
            cause is IllegalStateException && cause.message?.contains("binder", ignoreCase = true) == true ->
                FailureReason.SHIZUKU_UNAVAILABLE

            cause is IllegalStateException -> FailureReason.SHIZUKU_UNAVAILABLE
            cause is NullPointerException -> FailureReason.NOT_SUPPORTED
            else -> FailureReason.UNKNOWN
        }
    }

    private fun unwrap(error: Throwable?): Throwable? = when (error) {
        null -> null
        is InvocationTargetException -> error.targetException ?: error
        else -> error
    }

    companion object {
        private const val TAG = "BatchKit/Privileged"

        // App op strings as written by the platform. Numeric values are the stable
        // fallbacks used when AppOpsManager.strOpToOp() cannot be reached.
        private const val OPSTR_RUN_IN_BACKGROUND = "android:run_in_background"
        private const val OPSTR_RUN_ANY_IN_BACKGROUND = "android:run_any_in_background"
        private const val OPSTR_POST_NOTIFICATION = "android:post_notification"

        private const val OP_RUN_IN_BACKGROUND = 63
        private const val OP_RUN_ANY_IN_BACKGROUND = 70
        private const val OP_POST_NOTIFICATION = 11

        private const val MODE_ALLOWED = 0
        private const val MODE_IGNORED = 1
        private const val MODE_DEFAULT = 3
    }
}
