package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.CompatibilityConfidence
import com.example.data.models.DeviceSpecs
import com.example.data.models.GroundedCompatibilityReport
import com.example.data.models.GroundedSource
import com.example.data.models.SupportedDeviceEntry

@Composable
fun CompatibilityCheckerDialog(
    isOpen: Boolean,
    deviceSpecs: DeviceSpecs?,
    report: GroundedCompatibilityReport?,
    isLoading: Boolean,
    onCheckCompatibility: (customQuery: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var customSearchText by remember(deviceSpecs?.model) {
        mutableStateOf(deviceSpecs?.model ?: "")
    }
    var isShowingSupportedList by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 700.dp)
                .testTag("compatibility_checker_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131419),
            border = BorderStroke(1.dp, Color(0xFF282A36)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TravelExplore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Compatibility Checker",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4285F4))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Google Search Grounding",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_compat_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search & Model Input Bar
                OutlinedTextField(
                    value = customSearchText,
                    onValueChange = { customSearchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compat_search_input"),
                    label = { Text("Device Model to Verify", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. iQOO 13, Vivo X100 Pro, iQOO Neo 9", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFFA855F7)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onCheckCompatibility(customSearchText) },
                            enabled = !isLoading,
                            modifier = Modifier.testTag("run_compat_search_btn")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFA855F7)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Verify",
                                    tint = Color(0xFFA855F7)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onCheckCompatibility(customSearchText) }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF2E313D),
                        focusedContainerColor = Color(0xFF181920),
                        unfocusedContainerColor = Color(0xFF181920),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoading) {
                        item {
                            LoadingGroundingCard()
                        }
                    } else if (report != null) {
                        // 1. Status Alert or Verified Banner
                        item {
                            if (report.isOfficiallyListed) {
                                VerifiedStatusCard(report = report)
                            } else {
                                UnlistedAlertCard(report = report, deviceSpecs = deviceSpecs)
                            }
                        }

                        // 2. Grounded Analysis Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23)),
                                border = BorderStroke(1.dp, Color(0xFF2E313D))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "HARDWARE & GAMING ANALYSIS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 1.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF94A3B8)
                                        )

                                        if (report.isLiveGrounded) {
                                            Text(
                                                text = "● Live Grounded",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = report.detailedAnalysis,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // 3. Search Queries & Grounded Sources
                        if (report.groundedSources.isNotEmpty() || report.searchQueriesUsed.isNotEmpty()) {
                            item {
                                GroundedSourcesCard(
                                    sources = report.groundedSources,
                                    queries = report.searchQueriesUsed
                                )
                            }
                        }

                        // 4. Toggleable Officially Supported Vivo/iQOO Catalog
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isShowingSupportedList = !isShowingSupportedList },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF17181F)),
                                border = BorderStroke(1.dp, Color(0xFF282A36))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Official 144Hz / 120Hz Models Catalog",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${report.officialSupportedModels.size} verified flagship & gaming devices",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Text(
                                        text = if (isShowingSupportedList) "Hide" else "View All",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFA855F7)
                                    )
                                }
                            }
                        }

                        if (isShowingSupportedList) {
                            items(report.officialSupportedModels) { entry ->
                                SupportedModelRow(entry = entry, onSelect = {
                                    customSearchText = entry.modelName
                                    onCheckCompatibility(entry.modelName)
                                })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dismiss_compat_dialog_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(1.dp, Color(0xFF2E313D))
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = { onCheckCompatibility(customSearchText) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("refresh_grounding_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isLoading) "Checking..." else "Live Check")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingGroundingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171821)),
        border = BorderStroke(1.dp, Color(0xFF2E313D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF4285F4),
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Querying Google Search Grounding...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verifying live Vivo/iQOO gaming hardware specifications & OriginOS/FuntouchOS display profiles",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun VerifiedStatusCard(report: GroundedCompatibilityReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compat_verified_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818)),
        border = BorderStroke(1.dp, Color(0xFF10B981))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "OFFICIALLY SUPPORTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF34D399)
                )
                Text(
                    text = report.summaryVerdict,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Target '${report.deviceModelChecked}' is confirmed compatible with 144Hz/120Hz Ultra Game Mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA7F3D0)
                )
            }
        }
    }
}

@Composable
private fun UnlistedAlertCard(
    report: GroundedCompatibilityReport,
    deviceSpecs: DeviceSpecs?
) {
    val isNonVivo = report.confidence == CompatibilityConfidence.NON_VIVO_GENERIC || (deviceSpecs?.isVivoOrIqoo == false)
    val alertTitle = if (isNonVivo) "Non-Vivo Device Detected" else "Device Not Officially Listed"
    val bannerBorderColor = if (isNonVivo) Color(0xFFEF4444) else Color(0xFFF59E0B)
    val bannerContainerColor = if (isNonVivo) Color(0xFF261214) else Color(0xFF2A1F0D)
    val textColor = if (isNonVivo) Color(0xFFFCA5A5) else Color(0xFFFDE68A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compat_unlisted_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerContainerColor),
        border = BorderStroke(1.dp, bannerBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bannerBorderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = bannerBorderColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "COMPATIBILITY ALERT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = bannerBorderColor
                    )
                    Text(
                        text = alertTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Device '${report.deviceModelChecked}' is not found in the verified Vivo / iQOO official gaming database.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isNonVivo) {
                    "• High refresh rate forced values (e.g., peak_refresh_rate) may still be applied via ADB Write Secure Settings, but OriginOS proprietary game acceleration and V1/V2 display chips are absent on non-Vivo hardware."
                } else {
                    "• This model variant may still support ADB / Write Secure Settings unlock, but official Ultra Game Mode 144Hz frame interpolation may vary depending on firmware release."
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = textColor
            )
        }
    }
}

@Composable
private fun GroundedSourcesCard(
    sources: List<GroundedSource>,
    queries: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15161C)),
        border = BorderStroke(1.dp, Color(0xFF262833))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "GOOGLE SEARCH GROUNDING SOURCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (queries.isNotEmpty()) {
                Text(
                    text = "Queries: ${queries.joinToString(" • ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60A5FA)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            sources.take(3).forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF93C5FD),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportedModelRow(
    entry: SupportedDeviceEntry,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B22)),
        border = BorderStroke(1.dp, Color(0xFF2A2D3A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.modelName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = "${entry.series} • ${entry.chipset}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${entry.maxRefreshRate} Hz",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}
