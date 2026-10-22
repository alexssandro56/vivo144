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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.models.PerformanceProfile

@Composable
fun ActiveProfileCard(
    activeProfile: PerformanceProfile,
    onConfigureClick: () -> Unit,
    onToggleHighPerformance: ((Boolean) -> Unit)? = null,
    onSelectProfile: ((PerformanceProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isHighPerformanceActive = activeProfile.id == PerformanceProfile.ULTRA_PERFORMANCE.id

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_profile_card"),
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
                    text = "PERFORMANCE MODE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onConfigureClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "All Profiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA855F7)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Configure Profiles",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Title & Specs + Quick Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isHighPerformanceActive) Color(0xFF7C3AED).copy(alpha = 0.25f)
                            else Color(0xFF334155).copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (isHighPerformanceActive) Color(0xFFA855F7) else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHighPerformanceActive) "High Performance (Ultra)" else activeProfile.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${activeProfile.targetFps} FPS • ${activeProfile.refreshRate} Hz • ${activeProfile.powerMode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHighPerformanceActive) Color(0xFFA855F7) else Color(0xFF94A3B8)
                    )
                }

                if (onToggleHighPerformance != null) {
                    Switch(
                        checked = isHighPerformanceActive,
                        onCheckedChange = { onToggleHighPerformance(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7C3AED),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF2A2B36)
                        ),
                        modifier = Modifier.testTag("high_performance_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = activeProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            if (onSelectProfile != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceProfile.ALL_PROFILES.forEach { prof ->
                        val isSelected = activeProfile.id == prof.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF7C3AED) else Color(0xFF20222B))
                                .clickable { onSelectProfile(prof) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (prof.id) {
                                    PerformanceProfile.ULTRA_PERFORMANCE.id -> "Ultra 144Hz"
                                    PerformanceProfile.BALANCED.id -> "Balanced"
                                    else -> "Battery"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

