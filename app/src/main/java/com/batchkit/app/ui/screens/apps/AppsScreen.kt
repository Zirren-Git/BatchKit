package com.batchkit.app.ui.screens.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.batchkit.app.R
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.ShizukuInfo
import com.batchkit.app.data.repository.SortOption
import com.batchkit.app.ui.components.AppListItem
import com.batchkit.app.ui.components.BatchActionBottomSheet
import com.batchkit.app.ui.components.ConfirmActionDialog
import com.batchkit.app.ui.components.ShizukuStatusChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    shizukuInfo: ShizukuInfo,
    onNavigateToShizuku: () -> Unit,
    onExecutionComplete: (BatchExecutionReport) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        viewModel.setSearchQuery("")
                                    } else {
                                        isSearchActive = false
                                    }
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Close search")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertEdge) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.selectedPackages.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${uiState.selectedPackages.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        ShizukuStatusChip(
                            shizukuInfo = shizukuInfo,
                            onClick = onNavigateToShizuku
                        )

                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_all)) },
                                onClick = {
                                    viewModel.selectAllFiltered()
                                    isMenuOpen = false
                                },
                                leadingIcon = { Icon(Icons.Default.SelectAll, null) }
                            )
                            if (uiState.selectedPackages.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.deselect_all)) },
                                    onClick = {
                                        viewModel.clearSelection()
                                        isMenuOpen = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Clear, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.invert_selection)) },
                                    onClick = {
                                        viewModel.invertSelection()
                                        isMenuOpen = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by)) },
                                onClick = {
                                    isMenuOpen = false
                                    isSortMenuOpen = true
                                },
                                leadingIcon = { Icon(Icons.Default.FilterList, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.btn_refresh_status)) },
                                onClick = {
                                    viewModel.loadApps()
                                    isMenuOpen = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_asc)) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_desc)) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_package)) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.PACKAGE)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_install_date)) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.INSTALL_DATE)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_last_update)) },
                                onClick = {
                                    viewModel.setSortOption(SortOption.LAST_UPDATE)
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedPackages.isNotEmpty() && !uiState.isExecuting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showActionSheet = true },
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    text = {
                        Text(
                            text = stringResource(R.string.apps_selected, uiState.selectedPackages.size),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterType == FilterType.ALL,
                    onClick = { viewModel.setFilterType(FilterType.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = uiState.filterType == FilterType.USER,
                    onClick = { viewModel.setFilterType(FilterType.USER) },
                    label = { Text(stringResource(R.string.filter_user)) }
                )
                FilterChip(
                    selected = uiState.filterType == FilterType.SYSTEM,
                    onClick = { viewModel.setFilterType(FilterType.SYSTEM) },
                    label = { Text(stringResource(R.string.filter_system)) }
                )
                FilterChip(
                    selected = uiState.filterType == FilterType.RUNNING,
                    onClick = { viewModel.setFilterType(FilterType.RUNNING) },
                    label = { Text(stringResource(R.string.filter_running)) }
                )
                FilterChip(
                    selected = uiState.filterType == FilterType.FROZEN,
                    onClick = { viewModel.setFilterType(FilterType.FROZEN) },
                    label = { Text(stringResource(R.string.filter_frozen)) }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_apps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            isSelected = uiState.selectedPackages.contains(app.packageName),
                            onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                            loadIcon = { viewModel.loadIcon(it) }
                        )
                    }
                }
            }
        }
    }

    // Batch Action Sheet
    if (showActionSheet) {
        BatchActionBottomSheet(
            selectedCount = uiState.selectedPackages.size,
            isSafeModeEnabled = uiState.isSafeModeEnabled,
            onActionSelected = { action ->
                coroutineScope.launch {
                    sheetState.hide()
                    showActionSheet = false
                    if (action.isDestructive) {
                        viewModel.onActionTrigger(action)
                    } else {
                        viewModel.executeBatch(action, onExecutionComplete)
                    }
                }
            },
            onDismiss = { showActionSheet = false },
            sheetState = sheetState
        )
    }

    // Confirmation dialog for destructive actions
    uiState.pendingConfirmAction?.let { action ->
        ConfirmActionDialog(
            action = action,
            selectedCount = uiState.selectedPackages.size,
            onConfirm = {
                viewModel.executeBatch(action, onExecutionComplete)
            },
            onDismiss = {
                viewModel.dismissConfirmDialog()
            }
        )
    }

    // Execution progress dialog
    if (uiState.isExecuting) {
        Dialog(onDismissRequest = { /* Prevent dismiss during execution */ }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.executing_batch),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val progress = uiState.executionProgress
                    if (progress != null && progress.totalCount > 0) {
                        LinearProgressIndicator(
                            progress = { progress.currentIndex.toFloat() / progress.totalCount },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${progress.currentIndex} / ${progress.totalCount}: ${progress.currentApp.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
