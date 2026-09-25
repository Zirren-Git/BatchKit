package com.batchkit.app.ui.screens.apps

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batchkit.app.R
import com.batchkit.app.core.model.AppSettings
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.di.AppContainer
import com.batchkit.app.ui.components.ActionPickerSheet
import com.batchkit.app.ui.components.AppListItem
import com.batchkit.app.ui.components.ConfirmDialog
import com.batchkit.app.ui.components.EmptyState
import com.batchkit.app.ui.components.FilterSortSheet
import com.batchkit.app.ui.components.ProtectedWarningDialog

@Composable
fun AppsScreen(
    container: AppContainer,
    viewModel: AppsViewModel,
    settings: AppSettings,
    onRunAction: (BatchAction, List<PrivilegedTarget>) -> Unit,
    onSaveProfile: (String) -> Unit,
    onOpenShizuku: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val sortDescending by viewModel.sortDescending.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val runningAvailable by viewModel.runningStateAvailable.collectAsStateWithLifecycle()
    val usageAccess by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<BatchAction?>(null) }
    var showSaveProfile by remember { mutableStateOf(false) }

    val noSelectionMessage = stringResource(R.string.msg_no_selection)
    val actions = remember(settings.safeMode, settings.advancedUnlocked) { settings.visibleActions() }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = viewModel::setQuery,
            singleLine = true,
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.cd_search))
            },
            label = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apps_count_filtered, apps.size, allApps.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.cd_refresh))
            }
            IconButton(onClick = { showFilters = true }) {
                Icon(painterResource(R.drawable.ic_filter), contentDescription = stringResource(R.string.cd_filter))
            }
            IconButton(onClick = { showFilters = true }) {
                Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.cd_sort))
            }
        }

        if (!onboardingCompleted) {
            OnboardingCard(
                onStart = {
                    viewModel.completeOnboarding()
                    onOpenShizuku()
                },
                onSkip = viewModel::completeOnboarding,
            )
        }

        if (!runningAvailable) {
            HintCard(text = stringResource(R.string.apps_running_unavailable))
        }
        if (!usageAccess) {
            HintCard(
                text = stringResource(R.string.apps_usage_access_body),
                actionLabel = stringResource(R.string.action_grant_usage_access),
                onAction = onOpenUsageAccess,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading -> LoadingState()

                apps.isEmpty() -> EmptyState(
                    iconRes = R.drawable.ic_search,
                    title = stringResource(R.string.apps_empty_title),
                    body = stringResource(R.string.apps_empty_body),
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = apps, key = { it.packageName }) { entry ->
                        AppListItem(
                            entry = entry,
                            selected = entry.packageName in selected,
                            iconLoader = container.appIconLoader,
                            onToggle = viewModel::toggleSelection,
                        )
                    }
                }
            }
        }

        BottomBar(
            selectedCount = selectedCount,
            defaultActionLabel = stringResource(settings.defaultAction.labelRes),
            onSelectAll = viewModel::selectAllVisible,
            onClear = viewModel::clearSelection,
            onPrimary = {
                if (selectedCount == 0) onMessage(noSelectionMessage) else showActionPicker = true
            },
            onSaveProfile = {
                if (selectedCount == 0) {
                    onMessage(noSelectionMessage)
                } else {
                    showSaveProfile = true
                }
            },
        )
    }

    if (showFilters) {
        FilterSortSheet(
            filters = filters,
            sortMode = sortMode,
            sortDescending = sortDescending,
            onApply = { newFilters, newSort, descending ->
                viewModel.applyFilters(newFilters, newSort, descending)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }

    if (showActionPicker) {
        ActionPickerSheet(
            actions = actions,
            onPick = { action ->
                showActionPicker = false
                pendingAction = action
            },
            onDismiss = { showActionPicker = false },
        )
    }

    pendingAction?.let { action ->
        val blockedLabels = viewModel.blockedLabels(action)
        val confirm = {
            val targets = viewModel.selectedTargets()
            pendingAction = null
            onRunAction(action, targets)
        }
        if (blockedLabels.isNotEmpty()) {
            ProtectedWarningDialog(
                title = stringResource(R.string.confirm_protected_title, blockedLabels.size),
                message = stringResource(
                    R.string.confirm_protected_message,
                    (selectedCount - blockedLabels.size).coerceAtLeast(0),
                ),
                skipped = blockedLabels,
                confirmLabel = stringResource(R.string.confirm_apply),
                dismissLabel = stringResource(R.string.confirm_cancel),
                onConfirm = confirm,
                onDismiss = { pendingAction = null },
            )
        } else {
            ConfirmDialog(
                title = stringResource(
                    R.string.confirm_destructive_title,
                    stringResource(action.labelRes),
                    selectedCount,
                ),
                message = stringResource(R.string.confirm_destructive_message, selectedCount),
                confirmLabel = stringResource(R.string.confirm_apply),
                dismissLabel = stringResource(R.string.confirm_cancel),
                onConfirm = confirm,
                onDismiss = { pendingAction = null },
            )
        }
    }

    if (showSaveProfile) {
        SaveProfileDialog(
            onSave = { name ->
                showSaveProfile = false
                onSaveProfile(name)
            },
            onDismiss = { showSaveProfile = false },
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.apps_loading))
        }
    }
}

@Composable
private fun OnboardingCard(onStart: () -> Unit, onSkip: () -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) { Text(stringResource(R.string.onboarding_start)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
            }
        }
    }
}

@Composable
private fun HintCard(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun BottomBar(
    selectedCount: Int,
    defaultActionLabel: String,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onPrimary: () -> Unit,
    onSaveProfile: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (selectedCount == 0) {
                        stringResource(R.string.selection_none)
                    } else {
                        stringResource(R.string.selection_count, selectedCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectAll) {
                    Icon(painterResource(R.drawable.ic_checklist), contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cd_select_all_filtered))
                }
                if (selectedCount > 0) {
                    IconButton(onClick = onClear) {
                        Icon(
                            painterResource(R.drawable.ic_clear),
                            contentDescription = stringResource(R.string.cd_clear_selection),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                    Text(defaultActionLabel)
                }
                FilledTonalButton(onClick = onSaveProfile) {
                    Icon(painterResource(R.drawable.ic_save), contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.profile_save_title))
                }
            }
        }
    }
}

@Composable
private fun SaveProfileDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val fallbackName = stringResource(R.string.profile_name_placeholder)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_save_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.profile_name_label)) },
                placeholder = { Text(fallbackName) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.ifBlank { fallbackName }) }) {
                Text(stringResource(R.string.profile_save_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm_cancel)) }
        },
    )
}
