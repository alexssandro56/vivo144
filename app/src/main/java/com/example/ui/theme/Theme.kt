package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.models.AppThemeMode
import com.example.data.models.ThemePreferences

val LocalThemePreferences = compositionLocalOf { ThemePreferences.DEFAULT }

fun createDynamicDarkColorScheme(
    primaryColor: Color,
    darkContainerColor: Color? = null,
    isPureAmoledBlack: Boolean = true
): ColorScheme {
    val bg = if (isPureAmoledBlack) Color(0xFF000000) else Color(0xFF0D0E12)
    val surf = if (isPureAmoledBlack) Color(0xFF0F1015) else Color(0xFF17181D)
    val elevated = if (isPureAmoledBlack) Color(0xFF181920) else Color(0xFF202128)
    val border = if (isPureAmoledBlack) Color(0xFF282A36) else Color(0xFF2E313D)
    val container = darkContainerColor ?: primaryColor.copy(alpha = 0.22f)

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = Color(0xFFF3E8FF),
        secondary = Color(0xFF94A3B8),
        onSecondary = bg,
        secondaryContainer = elevated,
        onSecondaryContainer = Color(0xFFF1F5F9),
        tertiary = primaryColor.copy(alpha = 0.85f),
        onTertiary = Color.White,
        background = bg,
        onBackground = Color(0xFFF8FAFC),
        surface = surf,
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = elevated,
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = border,
        outlineVariant = Color(0xFF232530),
        error = StatusError,
        onError = Color.White,
        errorContainer = StatusErrorContainer,
        onErrorContainer = Color(0xFFFCA5A5)
    )
}

fun createDynamicLightColorScheme(
    primaryColor: Color,
    lightContainerColor: Color? = null
): ColorScheme {
    val container = lightContainerColor ?: primaryColor.copy(alpha = 0.12f)

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = Color(0xFF1E1B4B),
        secondary = Color(0xFF64748B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF0F172A),
        tertiary = primaryColor.copy(alpha = 0.85f),
        onTertiary = Color.White,
        background = Color(0xFFF8F9FC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF64748B),
        outline = Color(0xFFE2E8F0),
        outlineVariant = Color(0xFFCBD5E1),
        error = StatusError,
        onError = Color.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = StatusErrorContainer
    )
}

@Composable
fun MyApplicationTheme(
    themePreferences: ThemePreferences = ThemePreferences.DEFAULT,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themePreferences.themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val option = themePreferences.activeAccentOption
    val primaryColor = if (isDark) option.darkPrimary else option.lightPrimary
    val containerColor = if (isDark) option.darkContainer else option.lightContainer

    val colorScheme = if (isDark) {
        createDynamicDarkColorScheme(
            primaryColor = primaryColor,
            darkContainerColor = containerColor,
            isPureAmoledBlack = themePreferences.isAmoledPureBlack
        )
    } else {
        createDynamicLightColorScheme(
            primaryColor = primaryColor,
            lightContainerColor = containerColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalThemePreferences provides themePreferences) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
