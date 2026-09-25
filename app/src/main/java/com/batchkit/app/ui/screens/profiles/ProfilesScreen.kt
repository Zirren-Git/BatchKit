package com.batchkit.app.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.batchkit.app.core.model.Profile
import com.batchkit.app.ui.components.EmptyState
import com.batchkit.app.ui.components.StatusPill

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onApply: (Profile) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val pinnedId by viewModel.pinnedId.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.profiles_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (profiles.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_save,
                title = stringResource(R.string.profiles_empty_title),
                body = stringResource(R.string.profiles_empty_body),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        pinned = profile.id == pinnedId,
                        onApply = { onApply(profile) },
                        onPin = { pinned -> viewModel.pin(profile, pinned) },
                        onDelete = { pendingDelete = profile },
                        onScheduleChange = { enabled, hour, minute ->
                            viewModel.setSchedule(profile, enabled, hour, minute)
                        },
                    )
                }
            }
        }
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_message, profile.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(profile)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.profile_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    pinned: Boolean,
    onApply: () -> Unit,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onScheduleChange: (Boolean, Int, Int) -> Unit,
) {
    var scheduleEnabled by remember(profile.id, profile.scheduleEnabled) {
        mutableStateOf(profile.scheduleEnabled)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onPin(!pinned) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = stringResource(
                            if (pinned) R.string.profile_unpin else R.string.profile_pin,
                        ),
                        tint = if (pinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.profile_delete),
                    )
                }
            }

            Text(
                text = stringResource(R.string.profile_apps_count, profile.packages.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_actions_count, profile.actions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile.actions.take(3).forEach { action ->
                    StatusPill(
                        text = stringResource(action.labelRes),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onApply) { Text(stringResource(R.string.profile_apply)) }
                if (pinned) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_pinned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_schedule),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_schedule_time,
                            profile.scheduleHour,
                            profile.scheduleMinute,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = scheduleEnabled,
                    onCheckedChange = { checked ->
                        scheduleEnabled = checked
                        onScheduleChange(checked, profile.scheduleHour, profile.scheduleMinute)
                    },
                )
            }
            Text(
                text = stringResource(R.string.profile_schedule_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
