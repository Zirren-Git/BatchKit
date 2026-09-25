package com.batchkit.app.privileged

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.batchkit.app.core.codec.SelectionCodec
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.PrivilegedTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs privileged work inside its own process (`:privileged`).
 *
 * Keeping the Shizuku binder calls out of the UI process means a slow or blocked
 * binder call can never freeze the interface, and a crashed privileged call cannot
 * take the UI down with it. Requests arrive through a [Messenger], are executed
 * one at a time on a background dispatcher, and answered per app.
 */
class PrivilegedActionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val executionMutex = Mutex()
    private val executor: PrivilegedExecutor by lazy { ShizukuPrivilegedExecutor() }

    private lateinit var messenger: Messenger

    override fun onCreate() {
        super.onCreate()
        HiddenApiBootstrap.install()
        messenger = Messenger(IncomingHandler())
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("HandlerLeak")
    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PrivilegedProtocol.MSG_EXECUTE -> handleExecute(msg)
                PrivilegedProtocol.MSG_DIAGNOSE -> handleDiagnose(msg)
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun handleExecute(msg: Message) {
        val replyTo = msg.replyTo ?: return
        val data = msg.data ?: return
        val requestId = data.getInt(PrivilegedProtocol.KEY_REQUEST_ID)
        val packageName = data.getString(PrivilegedProtocol.KEY_PACKAGE).orEmpty()
        val action = data.getString(PrivilegedProtocol.KEY_ACTION_ID)?.let { BatchAction.fromId(it) }

        if (action == null || !SelectionCodec.isValidPackageName(packageName)) {
            reply(replyTo, requestId, ExecutionOutcome.failure(FailureReason.UNKNOWN, "Malformed privileged request"))
            return
        }

        val target = PrivilegedTarget(
            packageName = packageName,
            label = data.getString(PrivilegedProtocol.KEY_LABEL) ?: packageName,
            uid = data.getInt(PrivilegedProtocol.KEY_UID),
            userId = data.getInt(PrivilegedProtocol.KEY_USER_ID),
            isSystem = data.getBoolean(PrivilegedProtocol.KEY_IS_SYSTEM),
            isDeviceAdmin = data.getBoolean(PrivilegedProtocol.KEY_IS_ADMIN),
        )

        scope.launch {
            val outcome = executionMutex.withLock {
                try {
                    executor.execute(target, action)
                } catch (t: Throwable) {
                    Log.w(TAG, "Privileged call crashed for $packageName", t)
                    ExecutionOutcome.failure(FailureReason.UNKNOWN, t.message ?: t.javaClass.simpleName)
                }
            }
            reply(replyTo, requestId, outcome)
        }
    }

    private fun handleDiagnose(msg: Message) {
        val replyTo = msg.replyTo ?: return
        scope.launch {
            val report = runCatching { PrivilegedDiagnostics.run(this@PrivilegedActionService) }
                .getOrElse { error -> "diagnostics failed: ${error.javaClass.name}: ${error.message}" }
            val bundle = Bundle().apply {
                putInt(PrivilegedProtocol.KEY_REQUEST_ID, msg.data?.getInt(PrivilegedProtocol.KEY_REQUEST_ID) ?: 0)
                putString(PrivilegedProtocol.KEY_DIAGNOSTICS, report)
            }
            val response = Message.obtain(null, PrivilegedProtocol.MSG_DIAGNOSE_RESULT)
            response.data = bundle
            runCatching { replyTo.send(response) }
                .onFailure { Log.w(TAG, "Could not deliver diagnostics", it) }
        }
    }

    private fun reply(replyTo: Messenger, requestId: Int, outcome: ExecutionOutcome) {
        val bundle = Bundle().apply {
            putInt(PrivilegedProtocol.KEY_REQUEST_ID, requestId)
            putBoolean(PrivilegedProtocol.KEY_SUCCESS, outcome.success)
            putString(PrivilegedProtocol.KEY_REASON, outcome.reason.name)
            putString(PrivilegedProtocol.KEY_DETAIL, outcome.detail)
        }
        val message = Message.obtain(null, PrivilegedProtocol.MSG_RESULT)
        message.data = bundle
        try {
            replyTo.send(message)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not deliver privileged result", t)
        }
    }

    private companion object {
        const val TAG = "BatchKit/PrivilegedService"
    }
}
