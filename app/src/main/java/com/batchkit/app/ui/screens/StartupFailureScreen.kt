package com.batchkit.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batchkit.app.R

/**
 * Shown instead of the normal UI when a start-up component (the database, the
 * settings store or the Shizuku layer) could not be created.
 *
 * "The app does not open" gives nothing to act on, so the failure itself is what
 * this screen shows: the exception, its message and the top of its stack trace,
 * ready to be copied and sent to whoever maintains the build.
 */
@Composable
fun StartupFailureScreen(
    error: Throwable?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = describe(error)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.startup_failed_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.startup_failed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onCopy(report) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.startup_failed_copy))
            }
        }
    }
}

/** The exception class, its message and a trimmed stack trace, as one plain text block. */
private fun describe(error: Throwable?): String {
    if (error == null) return "BatchKit could not build its start-up components."
    return buildString {
        appendLine(error.javaClass.name)
        appendLine(error.message.orEmpty())
        appendLine()
        error.stackTrace.take(MAX_FRAMES).forEach { frame -> appendLine(frame.toString()) }
        var cause = error.cause
        var depth = 0
        while (cause != null && depth < MAX_CAUSES) {
            appendLine()
            appendLine("Caused by: ${cause.javaClass.name}")
            appendLine(cause.message.orEmpty())
            cause.stackTrace.take(MAX_FRAMES).forEach { frame -> appendLine(frame.toString()) }
            cause = cause.cause
            depth++
        }
    }.trim()
}

private const val MAX_FRAMES = 24
private const val MAX_CAUSES = 3
