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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompatibilityResult

@Composable
fun CompatibilitySummaryCard(
    compatibilityResult: CompatibilityResult?,
    onViewDetailsClick: () -> Unit,
    onCheckGroundedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isReady = compatibilityResult?.isReadyFor144Fps == true
    val statusColor = if (isReady) Color(0xFF10B981) else Color(0xFFF59E0B)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewDetailsClick() }
            .testTag("compatibility_summary_card"),
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
                    text = "COMPATIBILITY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagnostics",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA855F7)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Details",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Key Checks Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isVivo = compatibilityResult?.isVivoOrIqoo == true
                val is144 = compatibilityResult?.is144HzDisplayAvailable == true
                val isAndroid = compatibilityResult?.isAndroidCompatible == true
                val isAccess = compatibilityResult?.hasWriteSecureSettings == true || compatibilityResult?.isAdbConnected == true

                QuickCheckItem(
                    title = "Device",
                    subtitle = if (isVivo) "Supported" else "Generic",
                    isPassed = isVivo,
                    modifier = Modifier.weight(1f)
                )
                QuickCheckItem(
                    title = "Display",
                    subtitle = if (is144) "144 Hz" else "${compatibilityResult?.maxRefreshRate?.toInt() ?: 60} Hz",
                    isPassed = is144,
                    modifier = Modifier.weight(1f)
                )
                QuickCheckItem(
                    title = "Android",
                    subtitle = if (isAndroid) "Compatible" else "Unsupported",
                    isPassed = isAndroid,
                    modifier = Modifier.weight(1f)
                )
                QuickCheckItem(
                    title = "Access",
                    subtitle = if (compatibilityResult?.hasWriteSecureSettings == true) "Authorized" else if (compatibilityResult?.isAdbConnected == true) "Connected" else "Required",
                    isPassed = isAccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF23252E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Overall Score & Readiness Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Filled.Check else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isReady) "READY FOR 144 FPS" else "CONFIGURATION PENDING",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                        if (!isReady && compatibilityResult != null && compatibilityResult.missingRequirements.isNotEmpty()) {
                            Text(
                                text = compatibilityResult.missingRequirements.first(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(
                    text = "${compatibilityResult?.passedChecksCount ?: 0} / ${compatibilityResult?.totalRequiredChecks ?: 5}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            // Live Grounded Checker Button
            if (onCheckGroundedClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCheckGroundedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_grounded_checker_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1E202B),
                        contentColor = Color(0xFF93C5FD)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.TravelExplore,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify Online (Google Search Grounding)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCheckItem(
    title: String,
    subtitle: String,
    isPassed: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isPassed) Color(0xFF10B981) else Color(0xFFF59E0B)

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPassed) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF94A3B8)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}
