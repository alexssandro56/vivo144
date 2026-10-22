package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.models.BatteryInfo
import com.example.data.models.TemperatureStatus
import com.example.data.models.ThermalInfo

@Composable
fun BatteryThermalCard(
    batteryInfo: BatteryInfo,
    thermalInfo: ThermalInfo,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tempColor = when (thermalInfo.thermalSeverity) {
        TemperatureStatus.OPTIMAL -> Color(0xFF10B981) // Green
        TemperatureStatus.WARM -> Color(0xFFF59E0B)    // Amber
        TemperatureStatus.HIGH -> Color(0xFFF97316)    // Orange
        TemperatureStatus.CRITICAL -> Color(0xFFEF4444)// Red
        TemperatureStatus.UNAVAILABLE -> Color(0xFF94A3B8)
    }

    val batteryColor = when {
        batteryInfo.percentage <= 20 -> Color(0xFFEF4444)
        batteryInfo.percentage <= 40 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_thermal_card")
            .then(
                if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
            ),
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
                    text = "BATTERY & THERMAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                // Performance status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tempColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(tempColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = thermalInfo.thermalSeverity.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = tempColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column layout: Battery | Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Battery Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (batteryInfo.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
                            contentDescription = "Battery",
                            tint = batteryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${batteryInfo.percentage}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (batteryInfo.isCharging) "Charging" else "On battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batteryInfo.isCharging) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(Color(0xFF2E313D))
                )

                // Temperature Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DeviceThermostat,
                            contentDescription = "Temperature",
                            tint = tempColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Temperature",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = batteryInfo.formattedTemperature,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (batteryInfo.temperatureCelsius != null) thermalInfo.thermalSeverity.label else "Sensor offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = tempColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Status",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tempColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (thermalInfo.isThrottling) "Throttling Active" else "Optimal Framerate",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (thermalInfo.isThrottling) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
