package com.batchkit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batchkit.app.R
import com.batchkit.app.core.model.AppFilters
import com.batchkit.app.core.model.SortMode

/** Filter and sort sheet for the app list. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSortSheet(
    filters: AppFilters,
    sortMode: SortMode,
    sortDescending: Boolean,
    onApply: (AppFilters, SortMode, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filters) }
    var sort by remember { mutableStateOf(sortMode) }
    var descending by remember { mutableStateOf(sortDescending) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SectionHeader(stringResource(R.string.filter_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.showUserApps,
                    onClick = { draft = draft.copy(showUserApps = !draft.showUserApps) },
                    label = { Text(stringResource(R.string.filter_user)) },
                )
                FilterChip(
                    selected = draft.showSystemApps,
                    onClick = { draft = draft.copy(showSystemApps = !draft.showSystemApps) },
                    label = { Text(stringResource(R.string.filter_system)) },
                )
                FilterChip(
                    selected = draft.runningOnly,
                    onClick = { draft = draft.copy(runningOnly = !draft.runningOnly) },
                    label = { Text(stringResource(R.string.filter_running)) },
                )
                FilterChip(
                    selected = draft.frozenOnly,
                    onClick = { draft = draft.copy(frozenOnly = !draft.frozenOnly) },
                    label = { Text(stringResource(R.string.filter_frozen)) },
                )
            }

            SectionHeader(stringResource(R.string.sort_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sort == mode,
                        onClick = { sort = mode },
                        label = { Text(stringResource(sortModeLabelRes(mode))) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = descending,
                    onClick = { descending = !descending },
                    label = {
                        Text(
                            stringResource(
                                if (descending) R.string.sort_descending else R.string.sort_ascending,
                            ),
                        )
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val safeFilters = if (draft.showUserApps || draft.showSystemApps) {
                            draft
                        } else {
                            draft.copy(showUserApps = true)
                        }
                        onApply(safeFilters, sort, descending)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.filter_apply))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

fun sortModeLabelRes(mode: SortMode): Int = when (mode) {
    SortMode.NAME -> R.string.sort_name
    SortMode.LAST_USED -> R.string.sort_last_used
    SortMode.INSTALL_DATE -> R.string.sort_install_date
}
