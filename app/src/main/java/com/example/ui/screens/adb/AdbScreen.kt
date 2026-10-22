package com.example.ui.screens.adb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.platform.adb.WirelessConnectResult
import com.example.ui.components.AdbCommandBox
import com.example.ui.components.VivoTopAppBar
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VioletPrimaryDark

@Composable
fun AdbScreen(
    viewModel: AdbViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            VivoTopAppBar(
                title = "ADB Setup",
                subtitle = "Vivo & iQOO Wireless / USB Authorization",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshStatus(showToastOnCheck = true) },
                        modifier = Modifier.testTag("refresh_adb_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Status",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (uiState.isRefreshing) Modifier.rotate(rotationAngle) else Modifier
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LIVE CONNECTION STATUS CARD
            AdbStatusCard(
                uiState = uiState,
                onRefreshClick = { viewModel.refreshStatus(showToastOnCheck = true) },
                onOpenDevSettings = { viewModel.openDeveloperSettings(context) }
            )

            // 2. TAB SELECTOR (Instructions, Wireless Connect, Commands)
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color(0xFF17181D),
                contentColor = Color(0xFFA855F7),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        color = Color(0xFF7C3AED),
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFF23252E))
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("adb_tabs_row")
            ) {
                Tab(
                    selected = uiState.selectedTab == AdbSetupTab.INSTRUCTIONS,
                    onClick = { viewModel.selectTab(AdbSetupTab.INSTRUCTIONS) },
                    text = {
                        Text(
                            text = "Step-by-Step",
                            fontWeight = if (uiState.selectedTab == AdbSetupTab.INSTRUCTIONS) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_instructions")
                )

                Tab(
                    selected = uiState.selectedTab == AdbSetupTab.WIRELESS_CONNECT,
                    onClick = { viewModel.selectTab(AdbSetupTab.WIRELESS_CONNECT) },
                    text = {
                        Text(
                            text = "Wireless Connect",
                            fontWeight = if (uiState.selectedTab == AdbSetupTab.WIRELESS_CONNECT) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_wireless_connect")
                )

                Tab(
                    selected = uiState.selectedTab == AdbSetupTab.COMMANDS,
                    onClick = { viewModel.selectTab(AdbSetupTab.COMMANDS) },
                    text = {
                        Text(
                            text = "Commands",
                            fontWeight = if (uiState.selectedTab == AdbSetupTab.COMMANDS) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_commands")
                )
            }

            // 3. TAB CONTENT
            when (uiState.selectedTab) {
                AdbSetupTab.INSTRUCTIONS -> {
                    StepByStepGuideSection(
                        onOpenDevSettings = { viewModel.openDeveloperSettings(context) },
                        onOpenWirelessSettings = { viewModel.openWirelessDebugSettings(context) },
                        onCopyCommand = { viewModel.copyCommand(it) },
                        command = uiState.adbStatus?.permissionCommand
                            ?: "adb shell pm grant com.aistudio.iqoounlock.vfps android.permission.WRITE_SECURE_SETTINGS"
                    )
                }

                AdbSetupTab.WIRELESS_CONNECT -> {
                    WirelessConnectSection(
                        uiState = uiState,
                        onPortChange = { viewModel.updateWirelessPort(it) },
                        onPairingCodeChange = { viewModel.updateWirelessPairingCode(it) },
                        onConnectClick = { viewModel.attemptWirelessConnect() },
                        onRefreshClick = { viewModel.refreshStatus(showToastOnCheck = true) },
                        onOpenWirelessSettings = { viewModel.openWirelessDebugSettings(context) }
                    )
                }

                AdbSetupTab.COMMANDS -> {
                    TerminalCommandsSection(
                        uiState = uiState,
                        onCopyCommand = { viewModel.copyCommand(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// 1. STATUS CARD
// -------------------------------------------------------------
@Composable
private fun AdbStatusCard(
    uiState: AdbUiState,
    onRefreshClick: () -> Unit,
    onOpenDevSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = uiState.adbStatus
    val isAuthorized = status?.hasWriteSecureSettings == true
    val isUsbEnabled = status?.isUsbDebuggingEnabled == true
    val isDevEnabled = status?.isDeveloperOptionsEnabled == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adb_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF17181D)
        ),
        border = BorderStroke(
            1.dp,
            if (isAuthorized) StatusSuccess.copy(alpha = 0.5f) else Color(0xFF2E313D)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ADB & SYSTEM PERMISSION STATUS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isAuthorized) Color(0x2010B981)
                            else if (isUsbEnabled) Color(0x20F59E0B)
                            else Color(0x20EF4444)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isAuthorized) "AUTHORIZED"
                        else if (isUsbEnabled) "ADB DETECTED"
                        else "ACTION REQUIRED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isAuthorized) Color(0xFF10B981)
                        else if (isUsbEnabled) Color(0xFFF59E0B)
                        else Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-status rows
            StatusCheckRow(
                label = "Developer Options",
                isActive = isDevEnabled,
                activeText = "Enabled",
                inactiveText = "Disabled"
            )

            StatusCheckRow(
                label = "USB / Wireless Debugging",
                isActive = isUsbEnabled,
                activeText = "Active (Connected)",
                inactiveText = "Turned Off"
            )

            StatusCheckRow(
                label = "WRITE_SECURE_SETTINGS",
                isActive = isAuthorized,
                activeText = "Granted (144 FPS Ready)",
                inactiveText = "Not Granted (Requires ADB)"
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Buttons: Refresh Status & Dev Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRefreshClick,
                    enabled = !uiState.isRefreshing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        disabledContainerColor = Color(0xFF4C1D95)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("refresh_adb_status_btn")
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isRefreshing) "Refreshing…" else "Refresh Status",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onOpenDevSettings,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E4254)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_dev_options_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dev Settings",
                        color = Color(0xFFE2E8F0),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCheckRow(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFFCBD5E1)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF10B981) else Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) activeText else inactiveText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. STEP-BY-STEP GUIDE WITH SIMPLIFIED DIAGRAMS
// -------------------------------------------------------------
@Composable
private fun StepByStepGuideSection(
    onOpenDevSettings: () -> Unit,
    onOpenWirelessSettings: () -> Unit,
    onCopyCommand: (String) -> Unit,
    command: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STEP 1: DEVELOPER OPTIONS
        StepCard(
            stepNumber = "1",
            title = "Enable Developer Options on Vivo/iQOO",
            description = "Unlock system developer controls by tapping the Build Number."
        ) {
            // Visual Diagram: Settings UI Flow
            DiagramContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagramBreadcrumb(path = listOf("Settings", "About Phone", "Software Info"))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF20222B))
                            .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Build number / Software version",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "PD2307_A_14.0.12.6.W10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF7C3AED))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.TouchApp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap 7x",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Toast will appear: 'You are now a developer!'",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenDevSettings,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF3E4254)),
                modifier = Modifier.fillMaxWidth().testTag("step1_open_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Developer Options")
            }
        }

        // STEP 2: USB DEBUGGING & VIVO SECURITY SETTING
        StepCard(
            stepNumber = "2",
            title = "Enable USB Debugging & Security Mode",
            description = "Vivo/iQOO OriginOS requires granting both USB Debugging and Security Settings."
        ) {
            // Visual Diagram: Developer Option Switches
            DiagramContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagramBreadcrumb(path = listOf("Developer options", "Debugging Section"))

                    // Switch 1: USB Debugging
                    DiagramToggleRow(
                        title = "USB debugging",
                        subtitle = "Debug mode when USB is connected",
                        isChecked = true
                    )

                    // Switch 2: Vivo Security Settings
                    DiagramToggleRow(
                        title = "USB debugging (Security settings)",
                        subtitle = "Allow granting permissions & simulating input",
                        isChecked = true,
                        isHighlight = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenDevSettings,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF3E4254)),
                modifier = Modifier.fillMaxWidth().testTag("step2_open_dev_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Developer Options & Toggle")
            }
        }

        // STEP 3: WIRELESS DEBUGGING (NO PC REQUIRED ON ANDROID 11+)
        StepCard(
            stepNumber = "3",
            title = "Wireless Debugging (Wi-Fi 2.4/5 GHz)",
            description = "Pair and connect wirelessly over your local Wi-Fi without needing a USB cable."
        ) {
            // Visual Diagram: Wireless Debugging IP/Port & Pairing
            DiagramContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagramBreadcrumb(path = listOf("Developer options", "Wireless debugging"))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF20222B))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "IP address & Port",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "192.168.1.105:39145",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF334155))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pair Code: 482910",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF1F5F9),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Tip: Keep Wi-Fi connected and screen awake during initial connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenWirelessSettings,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF3E4254)),
                modifier = Modifier.fillMaxWidth().testTag("step3_open_wireless_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Wireless Debugging Settings")
            }
        }

        // STEP 4: RUN PERMISSION GRANT COMMAND
        StepCard(
            stepNumber = "4",
            title = "Execute Authorization Command",
            description = "Run this one-time command via ADB terminal, Shizuku, or PC shell to unlock 144 FPS."
        ) {
            AdbCommandBox(
                command = command,
                onCopyClick = onCopyCommand
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
        border = BorderStroke(1.dp, Color(0xFF2E313D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DiagramContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1015))
            .border(1.dp, Color(0xFF23252E), RoundedCornerShape(10.dp))
    ) {
        content()
    }
}

@Composable
private fun DiagramBreadcrumb(path: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        path.forEachIndexed { index, seg ->
            Text(
                text = seg,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (index == path.size - 1) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (index == path.size - 1) Color(0xFFA855F7) else Color(0xFF64748B)
            )
            if (index < path.size - 1) {
                Text(
                    text = " > ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun DiagramToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) Color(0xFF7C3AED).copy(alpha = 0.15f) else Color(0xFF20222B))
            .border(
                1.dp,
                if (isHighlight) Color(0xFF7C3AED).copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isChecked) Color(0xFF10B981) else Color(0xFF475569))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isChecked) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

// -------------------------------------------------------------
// 3. WIRELESS CONNECT & PAIR SECTION
// -------------------------------------------------------------
@Composable
private fun WirelessConnectSection(
    uiState: AdbUiState,
    onPortChange: (String) -> Unit,
    onPairingCodeChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenWirelessSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val ip = uiState.adbStatus?.localIpAddress ?: "127.0.0.1"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
            border = BorderStroke(1.dp, Color(0xFF2E313D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WIRELESS DEBUGGING CONNECTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF94A3B8)
                    )

                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Establish an on-device ADB session using Android 11+ Wireless Debugging port.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                // Host & IP indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1015))
                        .border(1.dp, Color(0xFF23252E), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Device Wi-Fi Host IP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = ip,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenWirelessSettings,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF3E4254)),
                            modifier = Modifier.testTag("open_wireless_settings_shortcut")
                        ) {
                            Text(
                                text = "Check Port",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA855F7)
                            )
                        }
                    }
                }

                // Port Input
                OutlinedTextField(
                    value = uiState.wirelessPort,
                    onValueChange = onPortChange,
                    label = { Text("Wireless Debugging Port") },
                    placeholder = { Text("e.g. 5555 or 39145") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color(0xFF3E4254),
                        focusedLabelColor = Color(0xFFA855F7),
                        cursorColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wireless_port_input")
                )

                // Optional Pairing Code Input
                OutlinedTextField(
                    value = uiState.wirelessPairingCode,
                    onValueChange = onPairingCodeChange,
                    label = { Text("Pairing Code (Optional)") },
                    placeholder = { Text("6-digit code (e.g. 482910)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onConnectClick()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color(0xFF3E4254),
                        focusedLabelColor = Color(0xFFA855F7),
                        cursorColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wireless_pairing_code_input")
                )

                // Result Feedback Box
                uiState.lastConnectResult?.let { result ->
                    val isSuccess = result is WirelessConnectResult.Success
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSuccess) Color(0x2010B981) else Color(0x20EF4444))
                            .border(
                                1.dp,
                                if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isSuccess) "Connection Status" else "Connection Failed",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                                Text(
                                    text = when (result) {
                                        is WirelessConnectResult.Success -> result.message
                                        is WirelessConnectResult.Failure -> "${result.error} ${result.suggestion}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // Connect & Refresh Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onConnectClick()
                        },
                        enabled = !uiState.isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("connect_wireless_btn")
                    ) {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting…", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Connect",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF3E4254)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("verify_wireless_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refresh Status",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. TERMINAL COMMANDS SECTION
// -------------------------------------------------------------
@Composable
private fun TerminalCommandsSection(
    uiState: AdbUiState,
    onCopyCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
            border = BorderStroke(1.dp, Color(0xFF2E313D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PRIMARY PERMISSION COMMAND",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "Grants the application permission to adjust display refresh rate and peak compositor frequency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                val mainCmd = uiState.adbStatus?.permissionCommand
                    ?: "adb shell pm grant com.aistudio.iqoounlock.vfps android.permission.WRITE_SECURE_SETTINGS"

                AdbCommandBox(
                    command = mainCmd,
                    onCopyClick = onCopyCommand
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
            border = BorderStroke(1.dp, Color(0xFF2E313D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ORIGINOS / FUNTOUCHOS 144 HZ OVERRIDE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "Directly sets the Vivo display compositor mode 3 (144Hz) and locks the peak refresh rate globally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                val vivoCmd = uiState.adbStatus?.vivoSpecificCommand
                    ?: "adb shell settings put global vivo_screen_refresh_rate_mode 3 && adb shell settings put system peak_refresh_rate 144.0"

                AdbCommandBox(
                    command = vivoCmd,
                    onCopyClick = onCopyCommand
                )
            }
        }
    }
}
