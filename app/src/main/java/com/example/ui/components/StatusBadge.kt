package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DiagnosticStatusType
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.VioletContainerDark
import com.example.ui.theme.VioletPrimaryDark

@Composable
fun StatusBadge(
    statusType: DiagnosticStatusType,
    label: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (statusType) {
        DiagnosticStatusType.SUPPORTED,
        DiagnosticStatusType.CONNECTED -> Triple(
            StatusSuccessContainer.copy(alpha = 0.35f),
            StatusSuccess,
            StatusSuccess
        )
        DiagnosticStatusType.AVAILABLE_144 -> Triple(
            VioletContainerDark.copy(alpha = 0.6f),
            VioletPrimaryDark,
            VioletPrimaryDark
        )
        DiagnosticStatusType.WARNING_REQ_ADB -> Triple(
            StatusWarningContainer.copy(alpha = 0.4f),
            StatusWarning,
            StatusWarning
        )
        DiagnosticStatusType.GENERIC_FALLBACK -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        DiagnosticStatusType.INFO -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.primary
        )
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 6.dp)
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp
        )
    }
}
