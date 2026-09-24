package com.batchkit.app.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.batchkit.app.data.model.ShizukuInfo
import com.batchkit.app.data.model.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class ShizukuManager(private val context: Context) {

    private val tag = "ShizukuManager"

    private val _shizukuInfo = MutableStateFlow(ShizukuInfo())
    val shizukuInfo: StateFlow<ShizukuInfo> = _shizukuInfo.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(tag, "Shizuku binder received")
        updateState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(tag, "Shizuku binder died")
        updateState()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Log.d(tag, "Shizuku permission result: req=$requestCode, result=$grantResult")
            updateState()
        }

    fun initialize() {
        // Bypass Android 9+ hidden API restrictions safely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
                Log.d(tag, "HiddenApiBypass initialized successfully")
            } catch (e: Throwable) {
                Log.w(tag, "Failed to initialize HiddenApiBypass", e)
            }
        }

        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            Log.w(tag, "Error registering Shizuku listeners", e)
        }

        updateState()
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            Log.w(tag, "Error cleaning up Shizuku listeners", e)
        }
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateState() {
        val installed = isShizukuInstalled()
        if (!installed) {
            val rootAvailable = isRootAvailable()
            _shizukuInfo.value = ShizukuInfo(
                status = ShizukuStatus.NOT_INSTALLED,
                isRoot = rootAvailable
            )
            return
        }

        val isAlive = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

        if (!isAlive) {
            val rootAvailable = isRootAvailable()
            _shizukuInfo.value = ShizukuInfo(
                status = ShizukuStatus.NOT_RUNNING,
                isRoot = rootAvailable
            )
            return
        }

        val hasPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }

        if (!hasPermission) {
            _shizukuInfo.value = ShizukuInfo(
                status = ShizukuStatus.PERMISSION_DENIED,
                version = try { Shizuku.getVersion() } catch (e: Throwable) { 0 },
                uid = try { Shizuku.getUid() } catch (e: Throwable) { -1 }
            )
            return
        }

        val version = try { Shizuku.getVersion() } catch (e: Throwable) { 0 }
        val uid = try { Shizuku.getUid() } catch (e: Throwable) { 2000 }

        _shizukuInfo.value = ShizukuInfo(
            status = ShizukuStatus.READY,
            version = version,
            uid = uid,
            isRoot = uid == 0
        )
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(requestCode)
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Executes an ADB command privileged process via Shizuku or fallback to root su.
     * Guaranteed to run off the main thread.
     */
    suspend fun executeCommand(args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        if (_shizukuInfo.value.isReady) {
            try {
                val process = Shizuku.newProcess(args.toTypedArray(), null, null)
                val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()
                return@withContext CommandResult(exitCode, stdout.trim(), stderr.trim())
            } catch (e: Throwable) {
                Log.e(tag, "Shizuku process execution failed for: $args", e)
                return@withContext CommandResult(-1, "", e.message ?: "Execution error")
            }
        }

        // Fallback to root su if Shizuku is not running
        if (isRootAvailable()) {
            try {
                val commandString = args.joinToString(" ")
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", commandString))
                val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()
                return@withContext CommandResult(exitCode, stdout.trim(), stderr.trim())
            } catch (e: Throwable) {
                Log.e(tag, "Root su execution failed for: $args", e)
                return@withContext CommandResult(-1, "", e.message ?: "Root execution error")
            }
        }

        CommandResult(
            exitCode = -1,
            output = "",
            error = "Privileged backend unavailable. Shizuku is not running or permission is denied."
        )
    }
}
