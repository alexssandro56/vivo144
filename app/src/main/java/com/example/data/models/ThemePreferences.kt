package com.example.data.models

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follows device dark/light system schedule"),
    DARK("Dark Theme", "AMOLED battery saving & low-light comfort"),
    LIGHT("Light Theme", "High daylight visibility & clean contrast")
}

data class AccentColorOption(
    val id: String,
    val name: String,
    val color: Color,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val darkContainer: Color,
    val lightContainer: Color
)

object AccentColorPalette {
    val ElectricViolet = AccentColorOption(
        id = "electric_violet",
        name = "Electric Violet",
        color = Color(0xFFA855F7),
        darkPrimary = Color(0xFFA855F7),
        lightPrimary = Color(0xFF7C3AED),
        darkContainer = Color(0xFF2E1065),
        lightContainer = Color(0xFFEDE9FE)
    )

    val CyberCyan = AccentColorOption(
        id = "cyber_cyan",
        name = "Cyber Cyan",
        color = Color(0xFF06B6D4),
        darkPrimary = Color(0xFF22D3EE),
        lightPrimary = Color(0xFF0891B2),
        darkContainer = Color(0xFF164E63),
        lightContainer = Color(0xFFCFFAFE)
    )

    val EmeraldSpeed = AccentColorOption(
        id = "emerald_speed",
        name = "Emerald Nitro",
        color = Color(0xFF10B981),
        darkPrimary = Color(0xFF34D399),
        lightPrimary = Color(0xFF059669),
        darkContainer = Color(0xFF064E3B),
        lightContainer = Color(0xFFD1FAE5)
    )

    val SunsetAmber = AccentColorOption(
        id = "sunset_amber",
        name = "Sunset Amber",
        color = Color(0xFFF59E0B),
        darkPrimary = Color(0xFFFBBF24),
        lightPrimary = Color(0xFFD97706),
        darkContainer = Color(0xFF78350F),
        lightContainer = Color(0xFFFEF3C7)
    )

    val CrimsonBlaze = AccentColorOption(
        id = "crimson_blaze",
        name = "Crimson Blaze",
        color = Color(0xFFEF4444),
        darkPrimary = Color(0xFFF87171),
        lightPrimary = Color(0xFFDC2626),
        darkContainer = Color(0xFF7F1D1D),
        lightContainer = Color(0xFFFEE2E2)
    )

    val NeonPink = AccentColorOption(
        id = "neon_pink",
        name = "Neon Magenta",
        color = Color(0xFFEC4899),
        darkPrimary = Color(0xFFF472B6),
        lightPrimary = Color(0xFFDB2777),
        darkContainer = Color(0xFF831843),
        lightContainer = Color(0xFFFCE7F3)
    )

    val SapphireBlue = AccentColorOption(
        id = "sapphire_blue",
        name = "Sapphire Pulse",
        color = Color(0xFF3B82F6),
        darkPrimary = Color(0xFF60A5FA),
        lightPrimary = Color(0xFF2563EB),
        darkContainer = Color(0xFF1E3A8A),
        lightContainer = Color(0xFFDBEAFE)
    )

    val ElectricLime = AccentColorOption(
        id = "electric_lime",
        name = "Electric Lime",
        color = Color(0xFF84CC16),
        darkPrimary = Color(0xFFA3E635),
        lightPrimary = Color(0xFF65A30D),
        darkContainer = Color(0xFF365314),
        lightContainer = Color(0xFFECFCCB)
    )

    val PRESETS = listOf(
        ElectricViolet,
        CyberCyan,
        EmeraldSpeed,
        SunsetAmber,
        CrimsonBlaze,
        NeonPink,
        SapphireBlue,
        ElectricLime
    )

    fun findPresetById(id: String): AccentColorOption {
        return PRESETS.firstOrNull { it.id == id } ?: ElectricViolet
    }

    fun findPresetByColor(color: Color): AccentColorOption? {
        return PRESETS.firstOrNull {
            it.color.value == color.value ||
            it.darkPrimary.value == color.value ||
            it.lightPrimary.value == color.value
        }
    }
}

data class ThemePreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColorId: String = "electric_violet",
    val customAccentColorHex: Long = 0xFFA855F7,
    val isCustomColor: Boolean = false,
    val isAmoledPureBlack: Boolean = true
) {
    val activeAccentColor: Color
        get() = if (isCustomColor) {
            Color(customAccentColorHex)
        } else {
            AccentColorPalette.findPresetById(accentColorId).color
        }

    val activeAccentOption: AccentColorOption
        get() = if (isCustomColor) {
            val baseColor = Color(customAccentColorHex)
            AccentColorOption(
                id = "custom",
                name = "Custom Color",
                color = baseColor,
                darkPrimary = baseColor,
                lightPrimary = baseColor,
                darkContainer = baseColor.copy(alpha = 0.25f),
                lightContainer = baseColor.copy(alpha = 0.15f)
            )
        } else {
            AccentColorPalette.findPresetById(accentColorId)
        }

    companion object {
        val DEFAULT = ThemePreferences()
    }
}
