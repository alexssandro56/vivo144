package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.UnlockStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.VioletPrimaryDark

@Composable
fun PrimaryActionButton(
    unlockStatus: UnlockStatus,
    targetFps: Int = 144,
    is144HzSupported: Boolean = true,
    maxSupportedFps: Int = 144,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isApplying = unlockStatus.isApplying
        val isUnlocked = unlockStatus.isUnlocked
        val isError = unlockStatus is UnlockStatus.Error

        val containerColor = when {
            isUnlocked -> StatusSuccess
            isError -> StatusError
            !is144HzSupported -> Color(0xFF7C3AED)
            else -> VioletPrimaryDark
        }

        Button(
            onClick = onClick,
            enabled = !isApplying,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("unlock_144_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = Color.White,
                disabledContainerColor = containerColor.copy(alpha = 0.6f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 1.dp
            )
        ) {
            AnimatedContent(
                targetState = unlockStatus,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "buttonState"
            ) { status ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when {
                        status.isApplying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.status_applying),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        status.isUnlocked -> {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Unlocked",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val displayUnlockedRate = if (is144HzSupported) targetFps else maxSupportedFps
                            Text(
                                text = "$displayUnlockedRate FPS UNLOCKED",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        status is UnlockStatus.Error -> {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Error",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RETRY CONFIGURATION",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        !is144HzSupported -> {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Unlock Max FPS",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UNLOCK $maxSupportedFps FPS (PANEL MAX)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Unlock",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UNLOCK $targetFps FPS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle below button
        val subText = when {
            isError -> (unlockStatus as UnlockStatus.Error).message
            isUnlocked && !is144HzSupported -> "$maxSupportedFps Hz mode active (144 Hz locked by display hardware)"
            isUnlocked -> "144 Hz display refresh rate mode active"
            isApplying -> "Syncing system window compositor refresh rates…"
            !is144HzSupported -> "144 Hz is locked for this panel. Tap to unlock maximum supported $maxSupportedFps FPS."
            else -> stringResource(R.string.btn_unlock_sub)
        }

        Text(
            text = subText,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) StatusError else MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp
        )
    }
}

