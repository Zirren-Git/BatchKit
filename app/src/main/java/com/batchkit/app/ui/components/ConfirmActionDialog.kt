package com.batchkit.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.batchkit.app.R
import com.batchkit.app.data.model.BatchActionType
import com.batchkit.app.ui.theme.StatusRed

@Composable
fun ConfirmActionDialog(
    action: BatchActionType,
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionName = stringResource(action.titleRes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.confirm_action_title, actionName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.confirm_action_message, actionName, selectedCount),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (action.isDestructive) StatusRed else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.confirm_proceed),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.confirm_cancel))
            }
        }
    )
}
