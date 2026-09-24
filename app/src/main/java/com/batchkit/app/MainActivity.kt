package com.batchkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.batchkit.app.data.repository.ThemeMode
import com.batchkit.app.ui.navigation.Screen
import com.batchkit.app.ui.screens.apps.AppsScreen
import com.batchkit.app.ui.screens.apps.AppsViewModel
import com.batchkit.app.ui.screens.profiles.ProfilesScreen
import com.batchkit.app.ui.screens.profiles.ProfilesViewModel
import com.batchkit.app.ui.screens.results.ResultsScreen
import com.batchkit.app.ui.screens.results.ResultsViewModel
import com.batchkit.app.ui.screens.settings.SettingsScreen
import com.batchkit.app.ui.screens.settings.SettingsViewModel
import com.batchkit.app.ui.screens.shizuku.ShizukuScreen
import com.batchkit.app.ui.theme.BatchKitTheme

class MainActivity : ComponentActivity() {

    private val appsViewModel: AppsViewModel by viewModels()
    private val resultsViewModel: ResultsViewModel by viewModels()
    private val profilesViewModel: ProfilesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userSettings by settingsViewModel.userSettings.collectAsState()
            val shizukuInfo by BatchKitApp.instance.shizukuManager.shizukuInfo.collectAsState()

            val isDarkTheme = when (userSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            BatchKitTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(
                        appsViewModel = appsViewModel,
                        resultsViewModel = resultsViewModel,
                        profilesViewModel = profilesViewModel,
                        settingsViewModel = settingsViewModel,
                        shizukuInfo = shizukuInfo
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BatchKitApp.instance.shizukuManager.updateState()
    }
}

@Composable
fun MainAppScaffold(
    appsViewModel: AppsViewModel,
    resultsViewModel: ResultsViewModel,
    profilesViewModel: ProfilesViewModel,
    settingsViewModel: SettingsViewModel,
    shizukuInfo: com.batchkit.app.data.model.ShizukuInfo
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on results screen
            if (currentRoute != Screen.Results.route) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = stringResource(screen.titleRes)
                                    )
                                }
                            },
                            label = { Text(stringResource(screen.titleRes)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Apps.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Apps.route) {
                AppsScreen(
                    viewModel = appsViewModel,
                    shizukuInfo = shizukuInfo,
                    onNavigateToShizuku = { navController.navigate(Screen.Shizuku.route) },
                    onExecutionComplete = { report ->
                        resultsViewModel.setReport(report)
                        navController.navigate(Screen.Results.route)
                    }
                )
            }

            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    viewModel = profilesViewModel,
                    shizukuInfo = shizukuInfo,
                    onNavigateToShizuku = { navController.navigate(Screen.Shizuku.route) },
                    onExecutionComplete = { report ->
                        resultsViewModel.setReport(report)
                        navController.navigate(Screen.Results.route)
                    }
                )
            }

            composable(Screen.Shizuku.route) {
                ShizukuScreen(shizukuInfo = shizukuInfo)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    shizukuInfo = shizukuInfo,
                    onNavigateToShizuku = { navController.navigate(Screen.Shizuku.route) }
                )
            }

            composable(Screen.Results.route) {
                ResultsScreen(
                    viewModel = resultsViewModel,
                    onDone = {
                        navController.navigate(Screen.Apps.route) {
                            popUpTo(Screen.Apps.route) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
