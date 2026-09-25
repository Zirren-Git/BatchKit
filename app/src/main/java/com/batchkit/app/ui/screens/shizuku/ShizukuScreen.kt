package com.batchkit.app.ui.screens.shizuku

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batchkit.app.R
import com.batchkit.app.core.model.ShizukuPhase
import com.batchkit.app.core.model.ShizukuStatus
import com.batchkit.app.di.AppContainer
import com.batchkit.app.ui.components.SectionHeader

/**
 * Shizuku setup and troubleshooting.
 *
 * The screen always shows what the current phase is, what the user has to do
 * about it, and how to fix the known OEM quirks, so a failed connection never
 * ends in a dead end.
 */
@Composable
fun ShizukuScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val status by container.shizukuStatusProvider.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.shizuku_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        PhaseCard(
            status = status,
            onPrimary = {
                when (status.phase) {
                    ShizukuPhase.NOT_INSTALLED -> openUrl(context, SHIZUKU_PLAY_URL)
                    ShizukuPhase.NOT_RUNNING -> openShizukuApp(context)
                    ShizukuPhase.PERMISSION_REQUIRED -> container.shizukuStatusProvider.requestPermission()
                    ShizukuPhase.READY -> container.shizukuStatusProvider.refresh()
                    ShizukuPhase.CHECKING -> container.shizukuStatusProvider.refresh()
                }
            },
        )

        SectionHeader(stringResource(R.string.shizuku_steps_title))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Step(1, stringResource(R.string.shizuku_step_1))
                Step(2, stringResource(R.string.shizuku_step_2))
                Step(3, stringResource(R.string.shizuku_step_3))
                Step(4, stringResource(R.string.shizuku_step_4))
                Step(5, stringResource(R.string.shizuku_step_5))
            }
        }

        SectionHeader(stringResource(R.string.shizuku_troubleshooting_title))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                TroubleText(R.string.shizuku_trouble_generic)
                Spacer(Modifier.height(8.dp))
                TroubleText(R.string.shizuku_trouble_miui)
                Spacer(Modifier.height(8.dp))
                TroubleText(R.string.shizuku_trouble_realme)
                Spacer(Modifier.height(8.dp))
                TroubleText(R.string.shizuku_trouble_usb)
                Spacer(Modifier.height(8.dp))
                TroubleText(R.string.shizuku_help_note)
                Spacer(Modifier.height(8.dp))
                TroubleText(R.string.shizuku_note_reboot)
            }
        }

        SectionHeader(stringResource(R.string.settings_about_title))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (status.version > 0) {
                        stringResource(R.string.shizuku_info_version, status.version, status.uid)
                    } else {
                        stringResource(R.string.shizuku_uid_unknown)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PhaseCard(status: ShizukuStatus, onPrimary: () -> Unit) {
    val context = LocalContext.current
    val ready = status.phase == ShizukuPhase.READY
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = if (ready) painterResource(R.drawable.ic_check_circle) else painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    tint = if (ready) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(status.statusLabelRes),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when (status.phase) {
                        ShizukuPhase.CHECKING -> R.string.shizuku_phase_checking
                        ShizukuPhase.NOT_INSTALLED -> R.string.shizuku_phase_not_installed_body
                        ShizukuPhase.NOT_RUNNING -> R.string.shizuku_phase_not_running_body
                        ShizukuPhase.PERMISSION_REQUIRED -> R.string.shizuku_phase_permission_body
                        ShizukuPhase.READY -> R.string.shizuku_phase_ready_body
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary) {
                    Text(
                        stringResource(
                            when (status.phase) {
                                ShizukuPhase.NOT_INSTALLED -> R.string.shizuku_action_install
                                ShizukuPhase.NOT_RUNNING -> R.string.shizuku_action_start
                                ShizukuPhase.PERMISSION_REQUIRED -> R.string.shizuku_action_grant
                                else -> R.string.shizuku_action_retry
                            },
                        ),
                    )
                }
                if (status.installed) {
                    OutlinedButton(onClick = { openShizukuApp(context) }) {
                        Text(stringResource(R.string.shizuku_action_open))
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TroubleText(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SHIZUKU_PLAY_URL = "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openShizukuApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(ShizukuStatus.SHIZUKU_PACKAGE)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    } else {
        openUrl(context, "package:" + ShizukuStatus.SHIZUKU_PACKAGE)
    }
}

/** Opens the system usage access screen; used by the app list hint. */
fun openUsageAccessSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
