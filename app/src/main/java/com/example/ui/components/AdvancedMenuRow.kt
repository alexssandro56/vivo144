package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.VioletPrimaryDark

@Composable
fun AdvancedMenuSection(
    onNavigateToAdb: () -> Unit,
    onNavigateToFpsConfig: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onRestoreDefaultsClick: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToPerformance: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.advanced_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Floating FPS Overlay HUD
                if (onNavigateToPerformance != null) {
                    AdvancedMenuItem(
                        icon = Icons.Filled.Layers,
                        title = "Floating FPS Counter HUD",
                        description = "Real-time in-game overlay with auto-launch",
                        onClick = onNavigateToPerformance,
                        tag = "menu_fps_overlay"
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Theme Customizer & Settings
                if (onNavigateToSettings != null) {
                    AdvancedMenuItem(
                        icon = Icons.Filled.Palette,
                        title = "Theme Customizer",
                        description = "Light, Dark, AMOLED & Custom Accent Colors",
                        onClick = onNavigateToSettings,
                        tag = "menu_theme_customizer"
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // ADB / Wireless Debugging
                AdvancedMenuItem(
                    icon = Icons.Filled.Terminal,
                    title = stringResource(R.string.adv_adb_title),
                    description = stringResource(R.string.adv_adb_desc),
                    onClick = onNavigateToAdb,
                    tag = "menu_adb"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // FPS Configuration
                AdvancedMenuItem(
                    icon = Icons.Filled.Tune,
                    title = stringResource(R.string.adv_fps_title),
                    description = stringResource(R.string.adv_fps_desc),
                    onClick = onNavigateToFpsConfig,
                    tag = "menu_fps_config"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Device Diagnostics
                AdvancedMenuItem(
                    icon = Icons.Filled.Speed,
                    title = stringResource(R.string.adv_diag_title),
                    description = stringResource(R.string.adv_diag_desc),
                    onClick = onNavigateToDiagnostics,
                    tag = "menu_diagnostics"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Restore Defaults
                AdvancedMenuItem(
                    icon = Icons.Filled.Refresh,
                    title = stringResource(R.string.adv_restore_title),
                    description = stringResource(R.string.adv_restore_desc),
                    onClick = onRestoreDefaultsClick,
                    tag = "menu_restore_defaults"
                )
            }
        }
    }
}

@Composable
fun AdvancedMenuItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = VioletPrimaryDark,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
