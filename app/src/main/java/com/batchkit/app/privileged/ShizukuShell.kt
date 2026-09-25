package com.batchkit.app.privileged

import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Runs a shell command with the identity Shizuku was started with (ADB shell).
 *
 * Commands are only used as a fallback for privileged calls that have no stable
 * binder interface across Android versions. Everything is invoked reflectively so
 * that a missing Shizuku API degrades into a reported failure instead of a crash.
 */
object ShizukuShell {

    private const val SHIZUKU_CLASS = "rikka.shizuku.Shizuku"

    data class ShellResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val error: String? = null,
    ) {
        val isSuccess: Boolean get() = error == null && exitCode == 0
    }

    fun run(command: String, timeoutSeconds: Long = 30L): ShellResult = try {
        val shizuku = Class.forName(SHIZUKU_CLASS)
        val newProcess = shizuku.getMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        val process = newProcess.invoke(null, arrayOf("sh", "-c", command), null, null)
            ?: return ShellResult(-1, "", "", "Shizuku.newProcess returned null")
        waitFor(process, timeoutSeconds)
        val stdout = readStream(process, "getInputStream")
        val stderr = readStream(process, "getErrorStream")
        val exit = runCatching { call(process, "exitValue") as? Int ?: -1 }.getOrDefault(-1)
        ShellResult(exit, stdout, stderr)
    } catch (t: Throwable) {
        ShellResult(-1, "", "", t.message ?: t.javaClass.simpleName)
    }

    private fun waitFor(process: Any, timeoutSeconds: Long) {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val task = executor.submit { call(process, "waitFor") }
            try {
                task.get(timeoutSeconds, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                runCatching { call(process, "destroy") }
                task.cancel(true)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun readStream(process: Any, getter: String): String = try {
        val stream = call(process, getter) as? InputStream
        stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    } catch (t: Throwable) {
        ""
    }

    private fun call(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = target.javaClass.methods.firstOrNull {
            it.name == methodName && it.parameterCount == args.size
        } ?: throw NoSuchMethodException("$methodName/${args.size}")
        return method.invoke(target, *args)
    }
}
