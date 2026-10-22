package com.example.ui.screens.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ActiveProfileCard
import com.example.ui.components.AdvancedMenuSection
import com.example.ui.components.BatteryThermalCard
import com.example.ui.components.CompatibilityCheckerDialog
import com.example.ui.components.CompatibilitySummaryCard
import com.example.ui.components.DeviceCard
import com.example.ui.components.FpsCard
import com.example.ui.components.PerformanceOverviewCard
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.RestoreConfirmDialog
import com.example.ui.components.VivoTopAppBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAdb: () -> Unit,
    onNavigateToFpsConfig: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToGameProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            VivoTopAppBar(
                title = "Vivo iQOO FPS Unlocker",
                subtitle = "Performance Center",
                canNavigateBack = false,
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("theme_settings_nav_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Theme Customizer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadData() },
                        modifier = Modifier.testTag("refresh_home_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("about_nav_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Device Hardware Header Card
            DeviceCard(deviceSpecs = uiState.deviceSpecs)

            // 2. MAIN FPS CARD (144 FPS / Target frame rate / Ready)
            FpsCard(
                fpsConfig = uiState.fpsConfig,
                unlockStatus = uiState.unlockStatus,
                deviceSpecs = uiState.deviceSpecs
            )

            // 3. Primary Action Button: [ UNLOCK 144 FPS / UNLOCK MAX FPS ]
            PrimaryActionButton(
                unlockStatus = uiState.unlockStatus,
                targetFps = uiState.fpsConfig.targetFps,
                is144HzSupported = uiState.deviceSpecs?.is144HzSupported ?: true,
                maxSupportedFps = uiState.deviceSpecs?.maxSupportedRefreshRate?.toInt() ?: 144,
                onClick = { viewModel.unlock144Fps() }
            )

            // 4. BATTERY & THERMAL CARD (Battery % • Temperature °C • Status)
            BatteryThermalCard(
                batteryInfo = uiState.batteryInfo,
                thermalInfo = uiState.thermalInfo,
                onCardClick = onNavigateToPerformance
            )

            // 5. COMPATIBILITY CHECKER SUMMARY CARD
            CompatibilitySummaryCard(
                compatibilityResult = uiState.compatibilityResult,
                onViewDetailsClick = onNavigateToDiagnostics,
                onCheckGroundedClick = { viewModel.showCompatibilityDialog(true) }
            )

            // 5.1 ADB SETUP DASHBOARD CARD
            if (uiState.deviceSpecs?.hasWriteSecureSettingsPermission != true) {
                AdbSetupDashboardCard(
                    onNavigateToAdb = onNavigateToAdb
                )
            }

            // 6. PERFORMANCE OVERVIEW (FPS • Refresh Rate • CPU • RAM)
            PerformanceOverviewCard(
                fpsMetrics = uiState.fpsMetrics,
                systemMetrics = uiState.systemMetrics,
                onViewPerformanceClick = onNavigateToPerformance
            )

            // 7. ACTIVE PROFILE CARD & HIGH PERFORMANCE TOGGLE
            ActiveProfileCard(
                activeProfile = uiState.activeProfile,
                onConfigureClick = onNavigateToFpsConfig,
                onToggleHighPerformance = { viewModel.toggleHighPerformanceMode(it) },
                onSelectProfile = { viewModel.selectProfile(it) }
            )

            // 8. ADVANCED NAVIGATION SECTION
            AdvancedMenuSection(
                onNavigateToAdb = onNavigateToAdb,
                onNavigateToFpsConfig = onNavigateToFpsConfig,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onRestoreDefaultsClick = { viewModel.showRestoreDialog(true) },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPerformance = onNavigateToPerformance
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Restore Confirmation Dialog
        RestoreConfirmDialog(
            isOpen = uiState.isRestoreDialogOpen,
            onConfirm = { viewModel.restoreDefaults() },
            onDismiss = { viewModel.showRestoreDialog(false) }
        )

        // Compatibility Checker Dialog (Google Search Grounding)
        CompatibilityCheckerDialog(
            isOpen = uiState.isCompatibilityDialogOpen,
            deviceSpecs = uiState.deviceSpecs,
            report = uiState.groundedReport,
            isLoading = uiState.isCheckingGroundedCompatibility,
            onCheckCompatibility = { customQuery -> viewModel.checkGroundedCompatibility(customQuery) },
            onDismiss = { viewModel.showCompatibilityDialog(false) }
        )
    }
}

@Composable
private fun AdbSetupDashboardCard(
    onNavigateToAdb: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToAdb)
            .testTag("dashboard_adb_setup_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F182A)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ADB Setup Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x30F59E0B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "1-Tap Guide",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Enable Wireless Debugging or USB Debugging on Vivo/iQOO to unlock 144 FPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate to ADB Setup",
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

