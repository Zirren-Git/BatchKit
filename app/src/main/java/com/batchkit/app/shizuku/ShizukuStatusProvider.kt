package com.batchkit.app.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.batchkit.app.core.model.ShizukuPhase
import com.batchkit.app.core.model.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Single source of truth for the Shizuku connection.
 *
 * The provider listens to the Shizuku binder lifecycle, so the status chip and
 * every screen stay correct when the user starts Shizuku, kills it, or when the
 * session dies after a reboot.
 */
class ShizukuStatusProvider(private val context: Context) {

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }.onFailure { Log.w(TAG, "Could not register Shizuku listeners", it) }
        refresh()
    }

    fun stop() {
        if (!listening) return
        listening = false
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }

    fun refresh() {
        _status.value = evaluate()
    }

    /** Opens the Shizuku permission dialog (Android 11+ shows it in the Shizuku app). */
    fun requestPermission() {
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
            .onFailure { Log.w(TAG, "Could not request Shizuku permission", it) }
        refresh()
    }

    private fun isShizukuInstalled(): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(ShizukuStatus.SHIZUKU_PACKAGE, 0)
        true
    } catch (t: Throwable) {
        false
    }

    private fun evaluate(): ShizukuStatus {
        val installed = isShizukuInstalled()
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = running && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        val version = if (running) runCatching { Shizuku.getVersion() }.getOrDefault(-1) else -1
        val uid = if (running) runCatching { Shizuku.getUid() }.getOrDefault(-1) else -1

        val phase = when {
            !installed -> ShizukuPhase.NOT_INSTALLED
            !running -> ShizukuPhase.NOT_RUNNING
            !granted -> ShizukuPhase.PERMISSION_REQUIRED
            else -> ShizukuPhase.READY
        }
        return ShizukuStatus(
            phase = phase,
            installed = installed,
            running = running,
            permissionGranted = granted,
            version = version,
            uid = uid,
        )
    }

    private companion object {
        const val TAG = "BatchKit/Shizuku"
        const val PERMISSION_REQUEST_CODE = 4210
    }
}
