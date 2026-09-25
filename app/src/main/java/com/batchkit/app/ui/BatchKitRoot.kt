package com.batchkit.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batchkit.app.R
import com.batchkit.app.core.model.AppSettings
import com.batchkit.app.core.model.BatchAction
import com.batchkit.app.core.model.Profile
import com.batchkit.app.core.model.PrivilegedTarget
import com.batchkit.app.di.AppContainer
import com.batchkit.app.engine.RunState
import com.batchkit.app.ui.components.ShizukuStatusChip
import com.batchkit.app.ui.screens.apps.AppsScreen
import com.batchkit.app.ui.screens.apps.AppsViewModel
import com.batchkit.app.ui.screens.onboarding.OnboardingScreen
import com.batchkit.app.ui.screens.profiles.ProfilesScreen
import com.batchkit.app.ui.screens.profiles.ProfilesViewModel
import com.batchkit.app.ui.screens.results.ResultsScreen
import com.batchkit.app.ui.screens.settings.SettingsScreen
import com.batchkit.app.ui.screens.shizuku.ShizukuScreen
import com.batchkit.app.ui.screens.shizuku.openUsageAccessSettings
import kotlinx.coroutines.launch

/**
 * The screen the bottom bar switches between. Kept as plain state instead of a
 * navigation graph: five destinations with no deep links do not need one, and
 * the state survives configuration changes and process death.
 */
private enum class Destination(
    @DrawableRes val iconRes: Int,
    val labelRes: Int,
) {
    APPS(R.drawable.ic_apps, R.string.screen_apps),
    RESULTS(R.drawable.ic_play, R.string.screen_results),
    PROFILES(R.drawable.ic_save, R.string.screen_profiles),
    SETTINGS(R.drawable.ic_settings, R.string.screen_settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchKitRoot(container: AppContainer) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val status by container.shizukuStatusProvider.status.collectAsStateWithLifecycle()
    val runState by container.runCoordinator.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val appsViewModel: AppsViewModel = viewModel(factory = AppsViewModel.Factory)
    val profilesViewModel: ProfilesViewModel = viewModel(factory = ProfilesViewModel.Factory)

    val protectedCount = remember(container) { container.protectedPackageResolver.resolve().size }
    var destination by rememberSaveable { mutableStateOf(Destination.APPS) }
    var showShizukuHelp by rememberSaveable { mutableStateOf(false) }
    val running = runState is RunState.Running

    // Announce the end of a run wherever the user happens to be.
    LaunchedEffect(runState) {
        val finished = runState as? RunState.Finished ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            context.getString(
                R.string.msg_run_finished,
                finished.summary.succeeded,
                finished.summary.total,
            ),
        )
    }

    val startRun: (BatchAction, List<PrivilegedTarget>) -> Unit = { action, targets ->
        when {
            targets.isEmpty() -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_no_selection))
            }

            running -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_run_started))
            }

            !status.ready -> {
                showShizukuHelp = true
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_shizuku_required))
                }
            }

            else -> {
                container.runCoordinator.start(action, targets)
                destination = Destination.RESULTS
            }
        }
    }

    val retryFailed: (List<String>) -> Unit = { packages ->
        scope.launch {
            val installed = container.appRepository.loadApps().associateBy { it.packageName }
            val action = (container.runCoordinator.state.value as? RunState.Finished)?.summary?.action
                ?: BatchAction.FORCE_STOP
            val targets = packages.mapNotNull { packageName ->
                installed[packageName]?.let { PrivilegedTarget.of(it) }
            }
            if (targets.isEmpty()) {
                snackbarHostState.showSnackbar(context.getString(R.string.results_nothing_to_retry))
            } else {
                container.runCoordinator.start(action, targets)
            }
        }
    }

    if (!settings.onboardingCompleted) {
        OnboardingScreen(
            container = container,
            onFinish = { appsViewModel.completeOnboarding() },
            onOpenShizuku = { showShizukuHelp = true },
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (showShizukuHelp) {
                            stringResource(R.string.shizuku_title)
                        } else {
                            stringResource(R.string.app_name)
                        },
                    )
                },
                navigationIcon = {
                    if (showShizukuHelp) {
                        IconButton(onClick = { showShizukuHelp = false }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    }
                },
                actions = {
                    ShizukuStatusChip(
                        status = status,
                        onClick = { showShizukuHelp = true },
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showShizukuHelp) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            showShizukuHelp -> ShizukuScreen(container = container, modifier = contentModifier)

            destination == Destination.APPS -> AppsScreen(
                container = container,
                viewModel = appsViewModel,
                settings = settings,
                modifier = contentModifier,
                onRunAction = startRun,
                onSaveProfile = { name ->
                    scope.launch {
                        container.profileRepository.save(
                            Profile(
                                name = name,
                                packages = appsViewModel.selected.value.toList(),
                                actions = listOf(settings.defaultAction),
                            ),
                        )
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.profile_saved_message),
                        )
                    }
                },
                onOpenShizuku = { showShizukuHelp = true },
                onOpenUsageAccess = { openUsageAccessSettings(context) },
                onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )

            destination == Destination.RESULTS -> ResultsScreen(
                container = container,
                modifier = contentModifier,
                onRetryFailed = retryFailed,
            )

            destination == Destination.PROFILES -> ProfilesScreen(
                modifier = contentModifier,
                viewModel = profilesViewModel,
                onApply = { profile ->
                    scope.launch {
                        if (!status.ready) {
                            showShizukuHelp = true
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.msg_profile_needs_shizuku),
                            )
                        } else {
                            val targets = profilesViewModel.resolveTargets(profile)
                            if (targets.isEmpty()) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.profile_missing_apps),
                                )
                            } else {
                                container.runCoordinator.applySequentially(profile.actions, targets)
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.msg_profile_applied, profile.name),
                                )
                                destination = Destination.RESULTS
                            }
                        }
                    }
                },
                onMessage = { message ->
                    if (message.isNotEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                },
            )

            else -> SettingsScreen(
                modifier = contentModifier,
                container = container,
                settings = settings,
                protectedCount = protectedCount,
                onSafeModeChange = { enabled ->
                    scope.launch { container.settingsRepository.setSafeMode(enabled) }
                },
                onAdvancedUnlockChange = { unlocked ->
                    scope.launch { container.settingsRepository.setAdvancedUnlocked(unlocked) }
                },
                onThemeChange = { mode -> scope.launch { container.settingsRepository.setThemeMode(mode) } },
                onDefaultActionChange = { action ->
                    scope.launch { container.settingsRepository.setDefaultAction(action) }
                },
            )
        }
    }
}
