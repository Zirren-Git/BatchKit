package com.batchkit.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.batchkit.app.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector? = null
) {
    object Apps : Screen("apps", R.string.nav_apps, Icons.Default.Apps)
    object Profiles : Screen("profiles", R.string.nav_profiles, Icons.Default.Bookmarks)
    object Shizuku : Screen("shizuku", R.string.nav_shizuku, Icons.Default.Security)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Results : Screen("results", R.string.results_title)

    companion object {
        val bottomNavItems = listOf(Apps, Profiles, Shizuku, Settings)
    }
}
