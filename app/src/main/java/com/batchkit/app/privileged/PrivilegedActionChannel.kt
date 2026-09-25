package com.batchkit.app.privileged

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.ExecutionOutcome
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.PrivilegedTarget
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Client side of the privileged bridge.
 *
 * Every call is forwarded to [PrivilegedActionService] in the `:privileged`
 * process. If that process cannot be started - or cannot reach Shizuku, which
 * happens on ROMs that never deliver the binder to secondary processes - the
 * channel degrades to running the same executor in this process on the calling
 * background thread, so actions keep working.
 */
class PrivilegedActionChannel(
    private val context: Context,
    private val localExecutor: PrivilegedExecutor = ShizukuPrivilegedExecutor(),
) : PrivilegedExecutor {

    private var session: RemoteSession? = null

    @Volatile
    private var useLocalExecutorOnly = false

    override fun isReady(): Boolean = localExecutor.isReady()

    /** Binds the privileged process. Safe to call before every batch. */
    fun open() {
        if (useLocalExecutorOnly || session != null) return
        val candidate = RemoteSession(context)
        if (candidate.connect()) {
            session = candidate
        } else {
            Log.w(TAG, "Privileged process unavailable, running in the app process instead")
            useLocalExecutorOnly = true
        }
    }

    /** Unbinds the privileged process once a batch finished. */
    fun close() {
        session?.disconnect()
        session = null
    }

    override fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome {
        val active = session
        if (useLocalExecutorOnly || active == null) {
            return localExecutor.execute(target, action)
        }
        val outcome = active.execute(target, action)
        if (!outcome.success && outcome.reason == FailureReason.SHIZUKU_UNAVAILABLE) {
            Log.w(TAG, "Privileged process has no Shizuku binder, falling back to in-process execution")
            useLocalExecutorOnly = true
            return localExecutor.execute(target, action)
        }
        return outcome
    }

    private class RemoteSession(private val context: Context) {

        private val requestIds = AtomicInteger(0)
        private val pending = ConcurrentHashMap<Int, ArrayBlockingQueue<ExecutionOutcome>>()
        private val connected = CountDownLatch(1)

        private var connection: ServiceConnection? = null
        private var outbound: Messenger? = null

        @SuppressLint("HandlerLeak")
        private val resultMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what != PrivilegedProtocol.MSG_RESULT) return
                val data = msg.data ?: return
                val requestId = data.getInt(PrivilegedProtocol.KEY_REQUEST_ID)
                val outcome = ExecutionOutcome(
                    success = data.getBoolean(PrivilegedProtocol.KEY_SUCCESS),
                    reason = parseReason(data.getString(PrivilegedProtocol.KEY_REASON)),
                    detail = data.getString(PrivilegedProtocol.KEY_DETAIL),
                )
                pending[requestId]?.offer(outcome)
            }
        })

        fun connect(): Boolean {
            val intent = Intent(context, PrivilegedActionService::class.java)
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    outbound = service?.let { Messenger(it) }
                    connected.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    outbound = null
                    connected.countDown()
                }
            }
            connection = serviceConnection
            val bound = try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (t: Throwable) {
                Log.w(TAG, "bindService failed", t)
                false
            }
            if (!bound) {
                connection = null
                return false
            }
            return try {
                connected.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS) && outbound != null
            } catch (t: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        }

        fun execute(target: PrivilegedTarget, action: BatchAction): ExecutionOutcome {
            val messenger = outbound
                ?: return ExecutionOutcome.failure(FailureReason.SHIZUKU_UNAVAILABLE, "Privileged process is not connected")
            val requestId = requestIds.incrementAndGet()
            val queue = ArrayBlockingQueue<ExecutionOutcome>(1)
            pending[requestId] = queue
            return try {
                val bundle = Bundle().apply {
                    putInt(PrivilegedProtocol.KEY_REQUEST_ID, requestId)
                    putString(PrivilegedProtocol.KEY_ACTION_ID, action.id)
                    putString(PrivilegedProtocol.KEY_PACKAGE, target.packageName)
                    putString(PrivilegedProtocol.KEY_LABEL, target.label)
                    putInt(PrivilegedProtocol.KEY_UID, target.uid)
                    putInt(PrivilegedProtocol.KEY_USER_ID, target.userId)
                    putBoolean(PrivilegedProtocol.KEY_IS_SYSTEM, target.isSystem)
                    putBoolean(PrivilegedProtocol.KEY_IS_ADMIN, target.isDeviceAdmin)
                }
                val message = Message.obtain(null, PrivilegedProtocol.MSG_EXECUTE)
                message.data = bundle
                message.replyTo = resultMessenger
                messenger.send(message)
                queue.poll(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    ?: ExecutionOutcome.failure(FailureReason.REMOTE_ERROR, "Privileged call timed out")
            } catch (t: Throwable) {
                ExecutionOutcome.failure(FailureReason.REMOTE_ERROR, t.message ?: t.javaClass.simpleName)
            } finally {
                pending.remove(requestId)
            }
        }

        fun disconnect() {
            connection?.let { runCatching { context.unbindService(it) } }
            connection = null
            outbound = null
            pending.clear()
        }

        private fun parseReason(raw: String?): FailureReason = try {
            raw?.let { FailureReason.valueOf(it) } ?: FailureReason.NONE
        } catch (t: IllegalArgumentException) {
            FailureReason.UNKNOWN
        }
    }

    private companion object {
        const val TAG = "BatchKit/PrivilegedChannel"
        const val CONNECT_TIMEOUT_SECONDS = 3L
        const val RESULT_TIMEOUT_SECONDS = 60L
    }
}
