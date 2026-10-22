package com.vivo144.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Neon = Color(0xFF3DFFB0)
val Cyan = Color(0xFF4FD8FF)
val Amber = Color(0xFFFFC266)
val Danger = Color(0xFFFF6B7A)
val Bg = Color(0xFF080B10)
val Surface1 = Color(0xFF10151D)
val Surface2 = Color(0xFF161D28)
val Outline = Color(0xFF243042)
val TextDim = Color(0xFF8A97A8)

private val scheme = darkColorScheme(
    primary = Neon,
    onPrimary = Color(0xFF00281A),
    primaryContainer = Color(0xFF0F3A2B),
    onPrimaryContainer = Neon,
    secondary = Cyan,
    onSecondary = Color(0xFF002233),
    secondaryContainer = Color(0xFF0E2A3A),
    onSecondaryContainer = Cyan,
    tertiary = Amber,
    background = Bg,
    onBackground = Color(0xFFE8EDF4),
    surface = Bg,
    onSurface = Color(0xFFE8EDF4),
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Color(0xFF1C2532),
    outline = Outline,
    outlineVariant = Color(0xFF1A2230),
    error = Danger,
)

private val type = Typography().let { t ->
    t.copy(
        displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 64.sp, letterSpacing = (-2).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun VivoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
