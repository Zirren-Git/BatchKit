package com.batchkit.app.ui.screens.profiles

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batchkit.app.R
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.data.model.BatchExecutionReport
import com.batchkit.app.data.model.Profile
import com.batchkit.app.data.model.ShizukuInfo
import com.batchkit.app.ui.components.ShizukuStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    shizukuInfo: ShizukuInfo,
    onNavigateToShizuku: () -> Unit,
    onExecutionComplete: (BatchExecutionReport) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profiles_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    ShizukuStatusChip(
                        shizukuInfo = shizukuInfo,
                        onClick = onNavigateToShizuku
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profiles_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onRun = { viewModel.runProfile(profile, onExecutionComplete) },
                        onDelete = { profileToDelete = profile }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(stringResource(R.string.profile_delete)) },
            text = { Text(stringResource(R.string.profile_delete_confirm, profile.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile)
                        profileToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm_proceed), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(stringResource(R.string.confirm_cancel))
                }
            }
        )
    }

    // Create Profile Dialog
    if (uiState.showCreateDialog) {
        CreateProfileDialog(
            uiState = uiState,
            onNameChange = { viewModel.updateNewProfileName(it) },
            onDescChange = { viewModel.updateNewProfileDesc(it) },
            onTogglePackage = { viewModel.toggleNewProfilePackage(it) },
            onToggleAction = { viewModel.toggleNewProfileAction(it) },
            onSave = { viewModel.saveNewProfile() },
            onDismiss = { viewModel.dismissCreateDialog() }
        )
    }
}

@Composable
fun ProfileCard(
    profile: Profile,
    onRun: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertEdge
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete profile",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profile.actions.forEach { action ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(action.titleRes), fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertEdge
            ) {
                Text(
                    text = "${profile.targetPackageNames.size} apps configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onRun) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.profile_run))
                }
            }
        }
    }
}

@Composable
fun CreateProfileDialog(
    uiState: ProfilesUiState,
    onNameChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
    onToggleAction: (BatchActionType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_profile),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newProfileName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.profile_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.newProfileDesc,
                    onValueChange = onDescChange,
                    label = { Text(stringResource(R.string.profile_desc_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.profile_select_actions, uiState.newProfileSelectedActions.size),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        BatchActionType.FORCE_STOP,
                        BatchActionType.BLOCK_BACKGROUND,
                        BatchActionType.RESTORE_BACKGROUND,
                        BatchActionType.BATTERY_OPTIMIZATION_RESTRICT
                    ).forEach { action ->
                        val isSelected = uiState.newProfileSelectedActions.contains(action)
                        SuggestionChip(
                            onClick = { onToggleAction(action) },
                            label = { Text(stringResource(action.titleRes), fontSize = 11.sp) },
                            icon = if (isSelected) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.profile_select_apps, uiState.newProfileSelectedPackages.size),
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    items(uiState.installedApps, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertEdge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = uiState.newProfileSelectedPackages.contains(app.packageName),
                                onCheckedChange = { onTogglePackage(app.packageName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = uiState.newProfileName.isNotBlank() && uiState.newProfileSelectedPackages.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm_cancel))
            }
        }
    )
}
