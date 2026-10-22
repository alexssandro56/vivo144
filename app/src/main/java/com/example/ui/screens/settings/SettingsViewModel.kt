package com.example.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.AccentColorPalette
import com.example.data.models.AppThemeMode
import com.example.data.models.ThemePreferences
import com.example.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomColorState(
    val red: Int = 168,
    val green: Int = 85,
    val blue: Int = 247,
    val hexInput: String = "A855F7",
    val isValidHex: Boolean = true,
    val isStudioExpanded: Boolean = false
) {
    val asColor: Color
        get() = Color(red, green, blue)

    val asHexLong: Long
        get() = (0xFFL shl 24) or ((red.toLong() and 0xFF) shl 16) or ((green.toLong() and 0xFF) shl 8) or (blue.toLong() and 0xFF)
}

data class SettingsUiState(
    val themePreferences: ThemePreferences = ThemePreferences.DEFAULT,
    val customColorState: CustomColorState = CustomColorState(),
    val userNotification: String? = null
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.themePreferencesFlow.collect { prefs ->
                val customColor = Color(prefs.customAccentColorHex)
                val r = (customColor.red * 255).toInt()
                val g = (customColor.green * 255).toInt()
                val b = (customColor.blue * 255).toInt()
                val hex = String.format("%02X%02X%02X", r, g, b)

                _uiState.update { current ->
                    current.copy(
                        themePreferences = prefs,
                        customColorState = current.customColorState.copy(
                            red = r,
                            green = g,
                            blue = b,
                            hexInput = hex,
                            isValidHex = true
                        )
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        userPreferencesRepository.setThemeMode(mode)
        _uiState.update {
            it.copy(userNotification = "Theme mode set to ${mode.displayName}")
        }
    }

    fun setPresetAccent(accentId: String) {
        userPreferencesRepository.setPresetAccentColor(accentId)
        val option = AccentColorPalette.findPresetById(accentId)
        _uiState.update {
            it.copy(userNotification = "Accent color set to ${option.name}")
        }
    }

    fun setAmoledBlack(enabled: Boolean) {
        userPreferencesRepository.setAmoledBlack(enabled)
        _uiState.update {
            it.copy(userNotification = if (enabled) "Pure AMOLED Black enabled" else "Standard dark theme enabled")
        }
    }

    fun toggleStudioExpanded() {
        _uiState.update {
            it.copy(customColorState = it.customColorState.copy(isStudioExpanded = !it.customColorState.isStudioExpanded))
        }
    }

    fun updateCustomChannel(r: Int? = null, g: Int? = null, b: Int? = null) {
        val current = _uiState.value.customColorState
        val newR = (r ?: current.red).coerceIn(0, 255)
        val newG = (g ?: current.green).coerceIn(0, 255)
        val newB = (b ?: current.blue).coerceIn(0, 255)
        val hex = String.format("%02X%02X%02X", newR, newG, newB)

        val updatedCustom = current.copy(
            red = newR,
            green = newG,
            blue = newB,
            hexInput = hex,
            isValidHex = true
        )

        _uiState.update { it.copy(customColorState = updatedCustom) }
        userPreferencesRepository.setCustomAccentColor(updatedCustom.asHexLong)
    }

    fun updateHexInput(rawHex: String) {
        val cleanHex = rawHex.trim().removePrefix("#").take(6)
        val isValid = cleanHex.length == 6 && cleanHex.all { it in "0123456789ABCDEFabcdef" }

        _uiState.update {
            it.copy(
                customColorState = it.customColorState.copy(
                    hexInput = cleanHex,
                    isValidHex = isValid || cleanHex.isEmpty()
                )
            )
        }

        if (isValid) {
            try {
                val colorInt = cleanHex.toLong(16)
                val r = ((colorInt shr 16) and 0xFF).toInt()
                val g = ((colorInt shr 8) and 0xFF).toInt()
                val b = (colorInt and 0xFF).toInt()

                val updatedCustom = _uiState.value.customColorState.copy(
                    red = r,
                    green = g,
                    blue = b,
                    hexInput = cleanHex,
                    isValidHex = true
                )

                _uiState.update { it.copy(customColorState = updatedCustom) }
                userPreferencesRepository.setCustomAccentColor(updatedCustom.asHexLong)
            } catch (_: Exception) {
                // Ignore parse errors while typing
            }
        }
    }

    fun applyQuickCustomColor(color: Color) {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        updateCustomChannel(r, g, b)
    }

    fun resetThemeToDefaults() {
        userPreferencesRepository.resetThemePreferences()
        _uiState.update {
            it.copy(userNotification = "Theme restored to system defaults")
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    companion object {
        fun provideFactory(
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(userPreferencesRepository) as T
            }
        }
    }
}
