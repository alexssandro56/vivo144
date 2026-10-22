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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.GameProfile
import com.example.data.models.PerformanceProfile

@Composable
fun AddEditGameDialog(
    gameProfile: GameProfile? = null,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (GameProfile) -> Unit
) {
    if (!isOpen) return

    var gameName by remember(gameProfile) { mutableStateOf(gameProfile?.gameName ?: "") }
    var packageName by remember(gameProfile) { mutableStateOf(gameProfile?.packageName ?: "") }
    var targetFps by remember(gameProfile) { mutableIntStateOf(gameProfile?.targetFps ?: 144) }
    var refreshRate by remember(gameProfile) { mutableIntStateOf(gameProfile?.refreshRate ?: 144) }
    var selectedProfileId by remember(gameProfile) {
        mutableStateOf(gameProfile?.performanceProfile?.id ?: PerformanceProfile.ULTRA_PERFORMANCE.id)
    }

    val presets = listOf(
        Pair("PUBG Mobile", "com.tencent.ig"),
        Pair("COD Mobile", "com.activision.callofduty.shooter"),
        Pair("Genshin Impact", "com.miHoYo.GenshinImpact"),
        Pair("Free Fire MAX", "com.dts.freefiremax"),
        Pair("Honor of Kings", "com.levelinfinite.sgameGlobal"),
        Pair("Asphalt 9", "com.gameloft.android.ANMP.GloftA9HM")
    )

    val fpsOptions = listOf(60, 90, 120, 144)
    val profiles = PerformanceProfile.ALL_PROFILES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (gameProfile == null) "Add Game Profile" else "Edit Game Profile",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Presets
                if (gameProfile == null) {
                    Text(
                        text = "POPULAR PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF94A3B8)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF20222B))
                                    .clickable {
                                        gameName = preset.first
                                        packageName = preset.second
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA855F7)
                                )
                            }
                        }
                    }
                }

                // Game Name Field
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Game Name") },
                    placeholder = { Text("e.g. PUBG Mobile") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF3E4254),
                        focusedLabelColor = Color(0xFFA855F7),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("game_name_input")
                )

                // Package Name Field
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    placeholder = { Text("e.g. com.tencent.ig") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF3E4254),
                        focusedLabelColor = Color(0xFFA855F7),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("package_name_input")
                )

                // Target FPS Selector
                Text(
                    text = "TARGET FPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fpsOptions.forEach { fps ->
                        val isSelected = targetFps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF7C3AED) else Color(0xFF20222B)
                                )
                                .clickable {
                                    targetFps = fps
                                    refreshRate = fps
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$fps",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Performance Profile Selector
                Text(
                    text = "PERFORMANCE PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profiles.forEach { profile ->
                        val isSelected = selectedProfileId == profile.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF2E244D) else Color(0xFF1E2029)
                                )
                                .clickable {
                                    selectedProfileId = profile.id
                                    targetFps = profile.targetFps
                                    refreshRate = profile.refreshRate
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "${profile.targetFps} FPS • ${profile.refreshRate} Hz",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color(0xFFA855F7) else Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFA855F7)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (gameName.isNotBlank() && packageName.isNotBlank()) {
                        val profile = PerformanceProfile.getById(selectedProfileId)
                        val updated = (gameProfile ?: GameProfile(
                            gameName = gameName.trim(),
                            packageName = packageName.trim()
                        )).copy(
                            gameName = gameName.trim(),
                            packageName = packageName.trim(),
                            targetFps = targetFps,
                            refreshRate = refreshRate,
                            performanceProfile = profile
                        )
                        onSave(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                ),
                enabled = gameName.isNotBlank() && packageName.isNotBlank(),
                modifier = Modifier.testTag("save_game_btn")
            ) {
                Text("Save Profile", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color(0xFF3E4254))
            ) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF17181D),
        shape = RoundedCornerShape(20.dp)
    )
}
