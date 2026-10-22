package com.example.ui.screens.performance

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.TemperatureStatus
import com.example.ui.components.FpsLiveChart
import com.example.ui.components.FpsOverlayCard
import com.example.ui.components.QuickSettingsTileCard
import com.example.ui.components.VivoTopAppBar

@Composable
fun PerformanceScreen(
    viewModel: PerformanceViewModel,
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

    val tempColor = when (uiState.thermalInfo.thermalSeverity) {
        TemperatureStatus.OPTIMAL -> Color(0xFF10B981)
        TemperatureStatus.WARM -> Color(0xFFF59E0B)
        TemperatureStatus.HIGH -> Color(0xFFF97316)
        TemperatureStatus.CRITICAL -> Color(0xFFEF4444)
        TemperatureStatus.UNAVAILABLE -> Color(0xFF94A3B8)
    }

    val stabilityColor = if (uiState.fpsMetrics.isStable) Color(0xFF10B981) else Color(0xFFF59E0B)

    Scaffold(
        topBar = {
            VivoTopAppBar(
                title = "Performance Monitor",
                subtitle = "Real-time System Telemetry",
                canNavigateBack = onNavigateBack != null,
                onNavigateBack = { onNavigateBack?.invoke() },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_perf_btn")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. REAL-TIME FPS MONITOR CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fps_monitor_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
                border = BorderStroke(1.dp, Color(0xFF2E313D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FPS MONITOR",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Stability Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(stabilityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(stabilityColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.fpsMetrics.stabilityStatus,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = stabilityColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Live Value Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "REAL-TIME FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${uiState.fpsMetrics.currentFps}",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FPS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFA855F7),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Frame time",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${uiState.fpsMetrics.frameTimeMs} ms",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Refresh: ${uiState.fpsMetrics.refreshRateHz.toInt()} Hz",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA855F7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Line Chart Canvas
                    FpsLiveChart(
                        fpsHistory = uiState.fpsMetrics.fpsHistory,
                        targetFps = uiState.targetFps
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Metric Sub-bar: Avg | Min | Max
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1015))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricSubCol(label = "Average FPS", value = "${uiState.fpsMetrics.averageFps}")
                        MetricSubCol(label = "Minimum FPS", value = "${uiState.fpsMetrics.minFps}")
                        MetricSubCol(label = "Maximum FPS", value = "${uiState.fpsMetrics.maxFps}")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Start / Stop Monitoring Button
                    Button(
                        onClick = { viewModel.toggleMonitoring() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isMonitoringActive) Color(0xFF2A2B36) else Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_fps_monitor_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isMonitoringActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isMonitoringActive) "Stop Monitoring" else "Start Live Monitoring",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 1.5 REAL-TIME FLOATING FPS OVERLAY HUD
            FpsOverlayCard(
                overlayPreferences = uiState.overlayPreferences,
                onUpdatePreferences = { prefs ->
                    viewModel.updateOverlayPreferences(prefs)
                },
                targetFps = uiState.targetFps
            )

            // 1.6 QUICK SETTINGS TILE SHORTCUT & PREVIEW
            QuickSettingsTileCard(
                isHighPerformanceActive = uiState.targetFps >= 144,
                onToggleHighPerformance = { isEnabled ->
                    viewModel.toggleHighPerformanceMode(isEnabled)
                }
            )

            // 2. SYSTEM PERFORMANCE CARD (CPU / GPU / RAM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_perf_card"),
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
                                imageVector = Icons.Filled.Memory,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM PERFORMANCE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Text(
                            text = uiState.systemMetrics.performanceHealth,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CPU Metric Bar
                    PerfMetricRow(
                        title = "CPU Usage",
                        value = uiState.systemMetrics.formattedCpuUsage,
                        progress = (uiState.systemMetrics.cpuUsagePercent ?: 0) / 100f,
                        note = "${uiState.systemMetrics.cpuCoresCount} System Cores (${uiState.systemMetrics.activeThreadsCount} Threads)"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // RAM Metric Bar
                    PerfMetricRow(
                        title = "RAM Memory",
                        value = "${uiState.systemMetrics.ramUsagePercent}%",
                        progress = uiState.systemMetrics.ramUsagePercent / 100f,
                        note = "${uiState.systemMetrics.formattedRamSummary} (Avail: ${String.format("%.1f GB", uiState.systemMetrics.availableRamGb)})"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // GPU Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1015))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GPU Graphics Hardware", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(uiState.systemMetrics.gpuRendererName ?: "Adreno / Mali GPU", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GPU Load", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text("Unavailable", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            // 3. BATTERY & THERMAL TELEMETRY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thermal_telemetry_card"),
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
                                imageVector = Icons.Filled.DeviceThermostat,
                                contentDescription = null,
                                tint = tempColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BATTERY & THERMAL TELEMETRY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Level", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text("${uiState.batteryInfo.percentage}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(uiState.batteryInfo.chargingState.displayName, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA855F7))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Thermal State", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(uiState.batteryInfo.formattedTemperature, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(uiState.thermalInfo.thermalStatus, style = MaterialTheme.typography.bodySmall, color = tempColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Health: ${uiState.batteryInfo.health} (${uiState.batteryInfo.technology})", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        Text(
                            text = if (uiState.thermalInfo.isThrottling) "System Throttling Active" else "Thermal Headroom OK",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (uiState.thermalInfo.isThrottling) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricSubCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
private fun PerfMetricRow(
    title: String,
    value: String,
    progress: Float,
    note: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE2E8F0))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFA855F7),
            trackColor = Color(0xFF2A2B36)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(note, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
    }
}
