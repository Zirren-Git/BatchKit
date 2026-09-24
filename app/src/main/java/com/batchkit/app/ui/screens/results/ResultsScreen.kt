package com.batchkit.app.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batchkit.app.R
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.SingleActionResult
import com.batchkit.app.ui.theme.StatusGreen
import com.batchkit.app.ui.theme.StatusRed
import com.batchkit.app.ui.theme.StatusYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.results_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (report != null && report.failureCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.retryFailed { viewModel.setReport(it) } },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isRetrying
                    ) {
                        Text(stringResource(R.string.results_retry_failed))
                    }
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.results_done))
                }
            }
        }
    ) { paddingValues ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No report available")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Summary card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(report.actionType.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.results_summary,
                                report.successCount,
                                report.failureCount,
                                report.skippedCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filter == ResultsFilter.ALL,
                        onClick = { viewModel.setFilter(ResultsFilter.ALL) },
                        label = { Text(stringResource(R.string.filter_results_all, report.totalCount)) }
                    )
                    FilterChip(
                        selected = uiState.filter == ResultsFilter.SUCCESS,
                        onClick = { viewModel.setFilter(ResultsFilter.SUCCESS) },
                        label = { Text(stringResource(R.string.filter_results_success, report.successCount)) }
                    )
                    if (report.failureCount > 0) {
                        FilterChip(
                            selected = uiState.filter == ResultsFilter.FAILED,
                            onClick = { viewModel.setFilter(ResultsFilter.FAILED) },
                            label = { Text(stringResource(R.string.filter_results_failed, report.failureCount)) }
                        )
                    }
                    if (report.skippedCount > 0) {
                        FilterChip(
                            selected = uiState.filter == ResultsFilter.SKIPPED,
                            onClick = { viewModel.setFilter(ResultsFilter.SKIPPED) },
                            label = { Text(stringResource(R.string.filter_results_skipped, report.skippedCount)) }
                        )
                    }
                }

                // Results list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredResults) { item ->
                        ResultItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultItemCard(
    item: SingleActionResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertEdge
        ) {
            val (statusColor, statusIcon) = when {
                item.isSkipped -> StatusYellow to Icons.Default.Warning
                item.isSuccess -> StatusGreen to Icons.Default.Check
                else -> StatusRed to Icons.Default.Close
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.message.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = if (item.isSuccess) MaterialTheme.colorScheme.primary else StatusRed
                    )
                }
            }
        }
    }
}
