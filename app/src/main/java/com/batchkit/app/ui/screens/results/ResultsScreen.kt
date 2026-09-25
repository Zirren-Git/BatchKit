package com.batchkit.app.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batchkit.app.R
import com.batchkit.app.core.model.AppActionResult
import com.batchkit.app.core.model.FailureReason
import com.batchkit.app.core.model.OutcomeStatus
import com.batchkit.app.di.AppContainer
import com.batchkit.app.engine.RunState
import com.batchkit.app.ui.components.EmptyState

private fun formatSeconds(millis: Long): String =
    java.util.Locale.getDefault().let { locale ->
        String.format(locale, "%.1f", millis / 1000.0)
    }

@Composable
fun ResultsScreen(
    container: AppContainer,
    onRetryFailed: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by container.runCoordinator.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.results_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        when (val current = state) {
            is RunState.Idle -> EmptyState(
                iconRes = R.drawable.ic_inbox,
                title = stringResource(R.string.results_empty),
                body = stringResource(R.string.results_nothing_to_retry),
                modifier = Modifier.fillMaxSize(),
            )

            is RunState.Running -> RunningView(current)

            is RunState.Finished -> FinishedView(
                summary = current,
                onRetryFailed = onRetryFailed,
            )
        }
    }
}

@Composable
private fun RunningView(state: RunState.Running) {
    val progress = if (state.total == 0) 0f else state.completed.toFloat() / state.total
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(state.action.labelRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.completed} / ${state.total}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.currentLabel?.let { label ->
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(items = state.results, key = { it.packageName }) { result ->
                ResultRow(result)
            }
        }
    }
}

@Composable
private fun FinishedView(
    summary: RunState.Finished,
    onRetryFailed: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val run = summary.summary
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(run.action.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.results_summary,
                        run.succeeded,
                        run.failed,
                        run.skipped,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.results_duration, formatSeconds(run.durationMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (run.failedPackages.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRetryFailed(run.failedPackages) }) {
                            Text(stringResource(R.string.results_retry_failed))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = run.results, key = { it.packageName }) { result ->
                ResultRow(result)
            }
        }
    }
}

@Composable
private fun ResultRow(result: AppActionResult) {
    val (icon, tint) = when (result.status) {
        OutcomeStatus.SUCCESS -> painterResource(R.drawable.ic_check_circle) to Color(0xFF2E9E5B)
        OutcomeStatus.FAILED -> painterResource(R.drawable.ic_error) to MaterialTheme.colorScheme.error
        OutcomeStatus.SKIPPED -> painterResource(R.drawable.ic_hourglass) to MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = statusText(result),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.duration_ms, result.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun statusText(result: AppActionResult): String = when (result.status) {
    OutcomeStatus.SUCCESS -> stringResource(R.string.results_status_success)
    OutcomeStatus.SKIPPED -> stringResource(R.string.results_status_skipped)
    OutcomeStatus.FAILED -> {
        val reason = failureReasonText(result.reason)
        if (result.detail.isNullOrBlank()) reason else "$reason (${result.detail})"
    }
}

@Composable
private fun failureReasonText(reason: FailureReason): String = when (reason) {
    FailureReason.NONE -> stringResource(R.string.reason_unknown)
    FailureReason.SHIZUKU_UNAVAILABLE -> stringResource(R.string.reason_shizuku_unavailable)
    FailureReason.PERMISSION_DENIED -> stringResource(R.string.reason_permission_denied)
    FailureReason.PROTECTED_PACKAGE -> stringResource(R.string.reason_protected)
    FailureReason.DEVICE_ADMIN -> stringResource(R.string.reason_device_admin)
    FailureReason.NOT_SUPPORTED -> stringResource(R.string.reason_not_supported)
    FailureReason.METHOD_NOT_FOUND -> stringResource(R.string.reason_method_missing)
    FailureReason.SHELL_FAILED -> stringResource(R.string.reason_shell_failed)
    FailureReason.REMOTE_ERROR -> stringResource(R.string.reason_remote_error)
    FailureReason.CANCELLED -> stringResource(R.string.reason_cancelled)
    FailureReason.UNKNOWN -> stringResource(R.string.reason_unknown)
}
