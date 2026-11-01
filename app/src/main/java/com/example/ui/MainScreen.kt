package com.example.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CounterTabContent
import com.example.ui.components.HistoryScreen
import com.example.ui.components.PresetsScreen
import com.example.ui.components.SettingsScreen
import com.example.ui.components.TargetReachedDialog
import com.example.ui.theme.FlameOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: CookerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Keep screen awake while cooking if setting is enabled
    DisposableEffect(uiState.isListening, uiState.settings.keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (window != null && uiState.isListening && uiState.settings.keepScreenAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
        if (isGranted) {
            viewModel.startListeningAndCooking()
        }
    }

    // Optional notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    DisposableEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onDispose { }
    }

    // Back handler: pop back to counter if in secondary tab
    if (uiState.selectedTab != CookerTab.COUNTER) {
        BackHandler {
            viewModel.selectTab(CookerTab.COUNTER)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.selectedTab) {
                            CookerTab.COUNTER -> "Whistle Counter"
                            CookerTab.PRESETS -> "Recipe Presets"
                            CookerTab.HISTORY -> "Cooking History"
                            CookerTab.SETTINGS -> "Settings & Tuning"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.25).sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == CookerTab.COUNTER,
                    onClick = { viewModel.selectTab(CookerTab.COUNTER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == CookerTab.COUNTER) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
                            contentDescription = "Counter"
                        )
                    },
                    label = { Text("Counter") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_counter")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == CookerTab.PRESETS,
                    onClick = { viewModel.selectTab(CookerTab.PRESETS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == CookerTab.PRESETS) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                            contentDescription = "Presets"
                        )
                    },
                    label = { Text("Presets") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_presets")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == CookerTab.HISTORY,
                    onClick = { viewModel.selectTab(CookerTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == CookerTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == CookerTab.SETTINGS,
                    onClick = { viewModel.selectTab(CookerTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.selectedTab == CookerTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                CookerTab.COUNTER -> {
                    CounterTabContent(
                        uiState = uiState,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onStartListening = {
                            if (uiState.hasRecordPermission) {
                                viewModel.startListeningAndCooking()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onPauseListening = { viewModel.pauseListening() },
                        onResetSession = { viewModel.resetSession() },
                        onIncrementTarget = { viewModel.incrementTarget() },
                        onDecrementTarget = { viewModel.decrementTarget() },
                        onManualAddWhistle = { viewModel.manualAddWhistle() },
                        onManualSubtractWhistle = { viewModel.manualSubtractWhistle() },
                        onSimulateWhistle = { viewModel.simulateCookerWhistle() },
                        onNavigateToPresets = { viewModel.selectTab(CookerTab.PRESETS) }
                    )
                }

                CookerTab.PRESETS -> {
                    PresetsScreen(
                        uiState = uiState,
                        onSelectPreset = { preset -> viewModel.applyPreset(preset) },
                        onAddCustomPreset = { name, whistles, category, tips ->
                            viewModel.addCustomPreset(name, whistles, category, tips)
                        }
                    )
                }

                CookerTab.HISTORY -> {
                    HistoryScreen(
                        uiState = uiState,
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }

                CookerTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) }
                    )
                }
            }
        }

        // Target Reached Alert Dialog
        TargetReachedDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissAlarm() }
        )
    }
}
