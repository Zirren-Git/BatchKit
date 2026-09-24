package com.batchkit.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.batchkit.app.R
import com.batchkit.app.data.model.AppInfo
import com.batchkit.app.ui.theme.StatusBlue
import com.batchkit.app.ui.theme.StatusGreen
import com.batchkit.app.ui.theme.StatusPurple
import com.batchkit.app.ui.theme.StatusRed
import com.batchkit.app.ui.theme.StatusYellow

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    loadIcon: suspend (String) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconDrawable by remember(app.packageName) { mutableStateOf<Drawable?>(app.icon) }

    LaunchedEffect(app.packageName) {
        if (iconDrawable == null) {
            iconDrawable = loadIcon(app.packageName)
        }
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertEdge,
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onToggleSelect(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect(it) },
            modifier = Modifier.padding(end = 8.dp)
        )

        // App Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val d = iconDrawable
            if (d != null) {
                val bitmap = remember(d) {
                    try {
                        d.toBitmap(width = 96, height = 96)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Label, package name, badges
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertEdge) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (app.isRunning) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ItemBadge(text = stringResource(R.string.badge_running), color = StatusGreen)
                }
            }

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Badges row
            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (app.isProtected) {
                    ItemBadge(text = stringResource(R.string.badge_protected), color = StatusPurple)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (app.isDeviceAdmin) {
                    ItemBadge(text = stringResource(R.string.badge_admin), color = StatusRed)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (app.isFrozen) {
                    ItemBadge(text = stringResource(R.string.badge_frozen), color = StatusYellow)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (app.isSystemApp) {
                    ItemBadge(text = stringResource(R.string.badge_system), color = StatusBlue)
                }
            }
        }
    }
}

@Composable
fun ItemBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
