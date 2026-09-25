package com.batchkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batchkit.app.core.model.ShizukuPhase
import com.batchkit.app.core.model.ShizukuStatus

/**
 * Always visible Shizuku indicator: green when the connection is ready, red when
 * it is not. Tapping it opens the setup and troubleshooting screen.
 */
@Composable
fun ShizukuStatusChip(
    status: ShizukuStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = status.phase == ShizukuPhase.READY
    val dotColor = when (status.phase) {
        ShizukuPhase.READY -> Color(0xFF2E9E5B)
        ShizukuPhase.CHECKING -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }
    val containerColor = if (ready) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (ready) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(status.statusLabelRes),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
