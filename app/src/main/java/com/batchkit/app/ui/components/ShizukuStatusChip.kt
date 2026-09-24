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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batchkit.app.R
import com.batchkit.app.data.model.ShizukuInfo
import com.batchkit.app.data.model.ShizukuStatus
import com.batchkit.app.ui.theme.StatusGreen
import com.batchkit.app.ui.theme.StatusRed
import com.batchkit.app.ui.theme.StatusYellow

@Composable
fun ShizukuStatusChip(
    shizukuInfo: ShizukuInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (shizukuInfo.status) {
        ShizukuStatus.READY -> StatusGreen to stringResource(R.string.shizuku_ready)
        ShizukuStatus.PERMISSION_DENIED -> StatusYellow to stringResource(R.string.shizuku_denied)
        ShizukuStatus.NOT_RUNNING -> StatusRed to stringResource(R.string.shizuku_not_running)
        ShizukuStatus.NOT_INSTALLED -> StatusRed to stringResource(R.string.shizuku_not_installed)
    }

    Row(
        verticalAlignment = Alignment.CenterVertEdge,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = statusColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
