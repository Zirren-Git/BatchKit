package com.batchkit.app.ui.screens.shizuku

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batchkit.app.BatchKitApp
import com.batchkit.app.R
import com.batchkit.app.data.model.ShizukuInfo
import com.batchkit.app.data.model.ShizukuStatus
import com.batchkit.app.domain.ShizukuManager
import com.batchkit.app.ui.theme.StatusGreen
import com.batchkit.app.ui.theme.StatusRed
import com.batchkit.app.ui.theme.StatusYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuScreen(
    shizukuInfo: ShizukuInfo,
    shizukuManager: ShizukuManager = BatchKitApp.instance.shizukuManager
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shizuku_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { shizukuManager.updateState() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Card
            LiveStatusCard(shizukuInfo = shizukuInfo)

            // Actions for Shizuku
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shizukuInfo.status == ShizukuStatus.PERMISSION_DENIED) {
                    Button(
                        onClick = { shizukuManager.requestPermission() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_request_permission))
                    }
                }

                OutlinedButton(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                            try {
                                context.startActivity(marketIntent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                                context.startActivity(webIntent)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_open_shizuku))
                }
            }

            // Wireless Debugging Guide
            Text(
                text = stringResource(R.string.guide_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            GuideStepCard(
                stepNumber = "1",
                title = stringResource(R.string.guide_step_1_title),
                description = stringResource(R.string.guide_step_1_desc)
            )

            GuideStepCard(
                stepNumber = "2",
                title = stringResource(R.string.guide_step_2_title),
                description = stringResource(R.string.guide_step_2_desc)
            )

            GuideStepCard(
                stepNumber = "3",
                title = stringResource(R.string.guide_step_3_title),
                description = stringResource(R.string.guide_step_3_desc)
            )

            GuideStepCard(
                stepNumber = "4",
                title = stringResource(R.string.guide_step_4_title),
                description = stringResource(R.string.guide_step_4_desc)
            )

            // OEM Quirks & Troubleshooting
            Text(
                text = stringResource(R.string.quirks_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            QuirkCard(
                title = stringResource(R.string.quirk_xiaomi_title),
                description = stringResource(R.string.quirk_xiaomi_desc)
            )

            QuirkCard(
                title = stringResource(R.string.quirk_oppo_title),
                description = stringResource(R.string.quirk_oppo_desc)
            )

            QuirkCard(
                title = stringResource(R.string.quirk_reboot_title),
                description = stringResource(R.string.quirk_reboot_desc)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LiveStatusCard(shizukuInfo: ShizukuInfo) {
    val (statusColor, statusIcon, statusText) = when (shizukuInfo.status) {
        ShizukuStatus.READY -> Triple(StatusGreen, Icons.Default.CheckCircle, stringResource(R.string.shizuku_ready))
        ShizukuStatus.PERMISSION_DENIED -> Triple(StatusYellow, Icons.Default.Warning, stringResource(R.string.shizuku_denied))
        ShizukuStatus.NOT_RUNNING -> Triple(StatusRed, Icons.Default.Error, stringResource(R.string.shizuku_not_running))
        ShizukuStatus.NOT_INSTALLED -> Triple(StatusRed, Icons.Default.Error, stringResource(R.string.shizuku_not_installed))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertEdge) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.shizuku_status_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            if (shizukuInfo.status == ShizukuStatus.READY) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_version_label, shizukuInfo.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val uidType = if (shizukuInfo.uid == 0) "Root" else "ADB Shell"
                    Text(
                        text = stringResource(R.string.shizuku_uid_label, shizukuInfo.uid, uidType),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuirkCard(
    title: String,
    description: String,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertEdge) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
