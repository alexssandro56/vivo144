package com.example.ui.screens.diagnostics

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.CheckStatus
import com.example.data.models.CompatibilityCheckItem
import com.example.data.models.CompatibilityResult
import com.example.data.models.DeviceSpecs
import com.example.ui.components.CompatibilityCheckerDialog
import com.example.ui.components.VivoTopAppBar

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: (() -> Unit)? = null,
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
                title = "Compatibility & Diagnostics",
                subtitle = "Hardware Verification & Capabilities",
                canNavigateBack = onNavigateBack != null,
                onNavigateBack = { onNavigateBack?.invoke() },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDiagnostics() },
                        modifier = Modifier.testTag("refresh_diag_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFA855F7))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. LIVE GROUNDED COMPATIBILITY BANNER
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grounded_compat_diag_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151926)),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TravelExplore,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LIVE MODEL CHECKER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF93C5FD)
                                )
                                Text(
                                    text = "Google Search Grounding",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.showCompatibilityDialog(true) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("diag_open_grounded_btn")
                        ) {
                            Text("Verify Model", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 1. COMPATIBILITY CHECKER SUMMARY & BREAKDOWN
                uiState.compatibilityResult?.let { compat ->
                    CompatibilityDetailCard(compat = compat)
                }

                // 2. HARDWARE SPECIFICATIONS CARD
                uiState.deviceSpecs?.let { specs ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hardware_specs_card"),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Analytics,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HARDWARE SPECIFICATIONS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            DiagRow(label = "Device Brand", value = specs.brand.ifBlank { specs.manufacturer })
                            DiagRow(label = "Model", value = specs.model)
                            DiagRow(label = "Device Code / Product", value = specs.deviceName)
                            DiagRow(label = "Manufacturer OEM", value = specs.manufacturer)
                            DiagRow(label = "Android Version", value = "${specs.androidVersion} (API ${specs.sdkInt})")
                            DiagRow(label = "OS Flavor", value = specs.customOsBrand)
                            DiagRow(label = "Build Number", value = specs.buildNumber)
                            DiagRow(
                                label = "144 Hz Display Support",
                                value = if (specs.is144HzSupported) "Supported (Ready)" else "Locked (Panel Max ${specs.maxSupportedRefreshRate.toInt()} Hz)"
                            )
                            DiagRow(label = "WRITE_SECURE_SETTINGS", value = if (specs.hasWriteSecureSettingsPermission) "Granted" else "Required via ADB")
                        }
                    }

                    // 3. DISPLAY MODES & CAPABILITIES
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_modes_card"),
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
                                Icon(
                                    imageVector = Icons.Filled.ScreenSearchDesktop,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SUPPORTED REFRESH RATES",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val rates = if (specs.supportedRefreshRates.isNotEmpty()) {
                                    specs.supportedRefreshRates.sortedDescending()
                                } else {
                                    listOf(144f, 120f, 60f)
                                }

                                rates.forEach { rate ->
                                    val isCurrent = rate == specs.currentRefreshRate
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrent) Color(0xFF7C3AED) else Color(0xFF20222B))
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${rate.toInt()}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isCurrent) Color.White else Color(0xFFE2E8F0)
                                            )
                                            Text(
                                                text = if (isCurrent) "Active" else "Supported",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrent) Color(0xFFE9D5FF) else Color(0xFF64748B),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Live Grounded Compatibility Checker Dialog
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
private fun CompatibilityDetailCard(compat: CompatibilityResult) {
    val isReady = compat.isReadyFor144Fps
    val statusColor = if (isReady) Color(0xFF10B981) else Color(0xFFF59E0B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compatibility_detail_card"),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPATIBILITY CHECKER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${compat.passedChecksCount} / ${compat.totalRequiredChecks} CHECKS PASSED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isReady) Color(0xFF0D2818) else Color(0xFF2A1C08))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Filled.Check else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isReady) "5 / 5 READY FOR 144 FPS" else "COMPATIBILITY ACTION REQUIRED",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isReady) "Your device meets all requirements for high-framerate unlocking."
                            else "Certain system permissions or hardware limits need attention.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                compat.items.forEach { item ->
                    CheckItemRow(item = item)
                }
            }

            if (compat.missingRequirements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MISSING REQUIREMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(4.dp))
                compat.missingRequirements.forEach { req ->
                    Text(
                        text = "• $req",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444).copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckItemRow(item: CompatibilityCheckItem) {
    val icon = when (item.status) {
        CheckStatus.PASSED -> Icons.Filled.Check
        CheckStatus.WARNING -> Icons.Filled.Warning
        CheckStatus.FAILED -> Icons.Filled.Close
        CheckStatus.UNAVAILABLE -> Icons.Filled.Info
    }

    val iconColor = when (item.status) {
        CheckStatus.PASSED -> Color(0xFF10B981)
        CheckStatus.WARNING -> Color(0xFFF59E0B)
        CheckStatus.FAILED -> Color(0xFFEF4444)
        CheckStatus.UNAVAILABLE -> Color(0xFF94A3B8)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1015))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                if (item.isOptional) {
                    Text(
                        text = "Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = iconColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.details,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}
