package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.shizuku.SystemAccessInfo

@Composable
fun SystemAccessCard(
    accessInfo: SystemAccessInfo,
    onConnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_access_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF17181D)
        ),
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
                Text(
                    text = "SYSTEM ACCESS",
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
                            if (accessInfo.hasWriteSecureSettings) Color(0x2010B981) else Color(0x20F59E0B)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (accessInfo.hasWriteSecureSettings) "WRITE_SECURE GRANTED" else "SANDBOX RESTRICTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (accessInfo.hasWriteSecureSettings) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Access Rows
            AccessStatusRow(
                label = "ADB",
                statusText = accessInfo.adbStatusText,
                isPositive = accessInfo.hasWriteSecureSettings || accessInfo.isAdbEnabled,
                isWarning = !accessInfo.hasWriteSecureSettings && accessInfo.isAdbEnabled
            )

            AccessStatusRow(
                label = "Wireless Debugging",
                statusText = accessInfo.wirelessDebugStatusText,
                isPositive = accessInfo.isWirelessDebuggingEnabled,
                isWarning = false
            )

            AccessStatusRow(
                label = "Shizuku",
                statusText = accessInfo.shizukuStatusText,
                isPositive = accessInfo.isShizukuRunning,
                isWarning = accessInfo.isShizukuInstalled && !accessInfo.isShizukuRunning
            )

            AccessStatusRow(
                label = "Root",
                statusText = accessInfo.rootStatusText,
                isPositive = accessInfo.isRootDetected,
                isWarning = false
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Connect / Guide | Refresh Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("connect_access_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect", color = Color.White)
                }

                OutlinedButton(
                    onClick = onRefreshClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E4254)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("refresh_access_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh", color = Color(0xFFE2E8F0))
                }
            }
        }
    }
}

@Composable
private fun AccessStatusRow(
    label: String,
    statusText: String,
    isPositive: Boolean,
    isWarning: Boolean
) {
    val indicatorColor = when {
        isPositive -> Color(0xFF10B981)
        isWarning -> Color(0xFFF59E0B)
        else -> Color(0xFF64748B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFFE2E8F0)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = indicatorColor
            )
        }
    }
}
