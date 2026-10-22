package com.example.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.AppThemeMode
import com.example.data.models.FpsConfig
import com.example.data.models.OverlayHudStyle
import com.example.data.models.OverlayPreferences
import com.example.data.models.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("iqoo_fps_unlocker_prefs", Context.MODE_PRIVATE)

    private val _fpsConfigFlow = MutableStateFlow(loadConfig())
    val userPreferencesFlow: StateFlow<FpsConfig> = _fpsConfigFlow.asStateFlow()
    val fpsConfigFlow: StateFlow<FpsConfig> get() = userPreferencesFlow

    private val _themePreferencesFlow = MutableStateFlow(loadThemePreferences())
    val themePreferencesFlow: StateFlow<ThemePreferences> = _themePreferencesFlow.asStateFlow()

    private val _overlayPreferencesFlow = MutableStateFlow(loadOverlayPreferences())
    val overlayPreferencesFlow: StateFlow<OverlayPreferences> = _overlayPreferencesFlow.asStateFlow()

    fun getTargetFps(): Int = _fpsConfigFlow.value.targetFps

    fun saveFpsConfig(targetFps: Int, refreshRate: Float, isUnlocked: Boolean) {
        val updated = _fpsConfigFlow.value.copy(
            targetFps = targetFps,
            refreshRateHz = refreshRate.toInt(),
            isUnlocked = isUnlocked,
            currentFps = targetFps
        )
        saveConfig(updated)
    }

    private fun loadConfig(): FpsConfig {
        return FpsConfig(
            targetFps = prefs.getInt(KEY_TARGET_FPS, 144),
            refreshRateHz = prefs.getInt(KEY_REFRESH_RATE, 144),
            isMultiTurboGameModeEnabled = prefs.getBoolean(KEY_GAME_MODE, true),
            isPeakRateForced = prefs.getBoolean(KEY_PEAK_FORCED, true),
            currentFps = prefs.getInt(KEY_CURRENT_FPS, 60),
            isUnlocked = prefs.getBoolean(KEY_IS_UNLOCKED, false)
        )
    }

    fun saveConfig(config: FpsConfig) {
        prefs.edit()
            .putInt(KEY_TARGET_FPS, config.targetFps)
            .putInt(KEY_REFRESH_RATE, config.refreshRateHz)
            .putBoolean(KEY_GAME_MODE, config.isMultiTurboGameModeEnabled)
            .putBoolean(KEY_PEAK_FORCED, config.isPeakRateForced)
            .putInt(KEY_CURRENT_FPS, config.currentFps)
            .putBoolean(KEY_IS_UNLOCKED, config.isUnlocked)
            .apply()
        _fpsConfigFlow.value = config
    }

    fun setUnlocked(isUnlocked: Boolean, currentFps: Int) {
        val updated = _fpsConfigFlow.value.copy(isUnlocked = isUnlocked, currentFps = currentFps)
        saveConfig(updated)
    }

    fun resetToDefaults() {
        val defaultConfig = FpsConfig.DEFAULT
        saveConfig(defaultConfig)
    }

    // --- THEME PREFERENCES ---
    private fun loadThemePreferences(): ThemePreferences {
        val themeModeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeModeName)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }

        return ThemePreferences(
            themeMode = themeMode,
            accentColorId = prefs.getString(KEY_ACCENT_ID, "electric_violet") ?: "electric_violet",
            customAccentColorHex = prefs.getLong(KEY_CUSTOM_ACCENT_HEX, 0xFFA855F7),
            isCustomColor = prefs.getBoolean(KEY_IS_CUSTOM_ACCENT, false),
            isAmoledPureBlack = prefs.getBoolean(KEY_AMOLED_BLACK, true)
        )
    }

    fun saveThemePreferences(themePreferences: ThemePreferences) {
        prefs.edit()
            .putString(KEY_THEME_MODE, themePreferences.themeMode.name)
            .putString(KEY_ACCENT_ID, themePreferences.accentColorId)
            .putLong(KEY_CUSTOM_ACCENT_HEX, themePreferences.customAccentColorHex)
            .putBoolean(KEY_IS_CUSTOM_ACCENT, themePreferences.isCustomColor)
            .putBoolean(KEY_AMOLED_BLACK, themePreferences.isAmoledPureBlack)
            .apply()
        _themePreferencesFlow.value = themePreferences
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        val updated = _themePreferencesFlow.value.copy(themeMode = themeMode)
        saveThemePreferences(updated)
    }

    fun setPresetAccentColor(accentId: String) {
        val updated = _themePreferencesFlow.value.copy(
            accentColorId = accentId,
            isCustomColor = false
        )
        saveThemePreferences(updated)
    }

    fun setCustomAccentColor(colorHex: Long) {
        val updated = _themePreferencesFlow.value.copy(
            customAccentColorHex = colorHex,
            isCustomColor = true
        )
        saveThemePreferences(updated)
    }

    fun setAmoledBlack(enabled: Boolean) {
        val updated = _themePreferencesFlow.value.copy(isAmoledPureBlack = enabled)
        saveThemePreferences(updated)
    }

    fun resetThemePreferences() {
        val defaultTheme = ThemePreferences.DEFAULT
        saveThemePreferences(defaultTheme)
    }

    // --- OVERLAY PREFERENCES ---
    private fun loadOverlayPreferences(): OverlayPreferences {
        val styleName = prefs.getString(KEY_OVERLAY_HUD_STYLE, OverlayHudStyle.COMPACT_PILL.name)
            ?: OverlayHudStyle.COMPACT_PILL.name
        val hudStyle = try {
            OverlayHudStyle.valueOf(styleName)
        } catch (_: Exception) {
            OverlayHudStyle.COMPACT_PILL
        }

        return OverlayPreferences(
            isOverlayEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, false),
            hudStyle = hudStyle,
            showFrameTime = prefs.getBoolean(KEY_OVERLAY_SHOW_FRAME_TIME, true),
            showMinFps = prefs.getBoolean(KEY_OVERLAY_SHOW_MIN_FPS, true),
            showRefreshRate = prefs.getBoolean(KEY_OVERLAY_SHOW_REFRESH_RATE, true),
            showBatteryTemp = prefs.getBoolean(KEY_OVERLAY_SHOW_BATTERY_TEMP, true),
            showCpuUsage = prefs.getBoolean(KEY_OVERLAY_SHOW_CPU, true),
            opacityPercent = prefs.getInt(KEY_OVERLAY_OPACITY, 90),
            autoLaunchOnGames = prefs.getBoolean(KEY_OVERLAY_AUTO_LAUNCH, true),
            snapToEdges = prefs.getBoolean(KEY_OVERLAY_SNAP_EDGES, true),
            accentColorHex = prefs.getLong(KEY_OVERLAY_ACCENT_HEX, 0xFFA855F7),
            posX = prefs.getInt(KEY_OVERLAY_POS_X, 40),
            posY = prefs.getInt(KEY_OVERLAY_POS_Y, 180)
        )
    }

    fun saveOverlayPreferences(overlayPrefs: OverlayPreferences) {
        prefs.edit()
            .putBoolean(KEY_OVERLAY_ENABLED, overlayPrefs.isOverlayEnabled)
            .putString(KEY_OVERLAY_HUD_STYLE, overlayPrefs.hudStyle.name)
            .putBoolean(KEY_OVERLAY_SHOW_FRAME_TIME, overlayPrefs.showFrameTime)
            .putBoolean(KEY_OVERLAY_SHOW_MIN_FPS, overlayPrefs.showMinFps)
            .putBoolean(KEY_OVERLAY_SHOW_REFRESH_RATE, overlayPrefs.showRefreshRate)
            .putBoolean(KEY_OVERLAY_SHOW_BATTERY_TEMP, overlayPrefs.showBatteryTemp)
            .putBoolean(KEY_OVERLAY_SHOW_CPU, overlayPrefs.showCpuUsage)
            .putInt(KEY_OVERLAY_OPACITY, overlayPrefs.opacityPercent)
            .putBoolean(KEY_OVERLAY_AUTO_LAUNCH, overlayPrefs.autoLaunchOnGames)
            .putBoolean(KEY_OVERLAY_SNAP_EDGES, overlayPrefs.snapToEdges)
            .putLong(KEY_OVERLAY_ACCENT_HEX, overlayPrefs.accentColorHex)
            .putInt(KEY_OVERLAY_POS_X, overlayPrefs.posX)
            .putInt(KEY_OVERLAY_POS_Y, overlayPrefs.posY)
            .apply()
        _overlayPreferencesFlow.value = overlayPrefs
    }

    fun setOverlayEnabled(enabled: Boolean) {
        val updated = _overlayPreferencesFlow.value.copy(isOverlayEnabled = enabled)
        saveOverlayPreferences(updated)
    }

    fun setOverlayHudStyle(style: OverlayHudStyle) {
        val updated = _overlayPreferencesFlow.value.copy(hudStyle = style)
        saveOverlayPreferences(updated)
    }

    fun setOverlayPosition(x: Int, y: Int) {
        val updated = _overlayPreferencesFlow.value.copy(posX = x, posY = y)
        saveOverlayPreferences(updated)
    }

    fun setOverlayOpacity(opacityPercent: Int) {
        val updated = _overlayPreferencesFlow.value.copy(opacityPercent = opacityPercent.coerceIn(20, 100))
        saveOverlayPreferences(updated)
    }

    fun setAutoLaunchOnGames(enabled: Boolean) {
        val updated = _overlayPreferencesFlow.value.copy(autoLaunchOnGames = enabled)
        saveOverlayPreferences(updated)
    }

    companion object {
        private const val KEY_TARGET_FPS = "key_target_fps"
        private const val KEY_REFRESH_RATE = "key_refresh_rate"
        private const val KEY_GAME_MODE = "key_game_mode"
        private const val KEY_PEAK_FORCED = "key_peak_forced"
        private const val KEY_CURRENT_FPS = "key_current_fps"
        private const val KEY_IS_UNLOCKED = "key_is_unlocked"

        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ACCENT_ID = "key_accent_id"
        private const val KEY_CUSTOM_ACCENT_HEX = "key_custom_accent_hex"
        private const val KEY_IS_CUSTOM_ACCENT = "key_is_custom_accent"
        private const val KEY_AMOLED_BLACK = "key_amoled_black"

        private const val KEY_OVERLAY_ENABLED = "key_overlay_enabled"
        private const val KEY_OVERLAY_HUD_STYLE = "key_overlay_hud_style"
        private const val KEY_OVERLAY_SHOW_FRAME_TIME = "key_overlay_show_frame_time"
        private const val KEY_OVERLAY_SHOW_MIN_FPS = "key_overlay_show_min_fps"
        private const val KEY_OVERLAY_SHOW_REFRESH_RATE = "key_overlay_show_refresh_rate"
        private const val KEY_OVERLAY_SHOW_BATTERY_TEMP = "key_overlay_show_battery_temp"
        private const val KEY_OVERLAY_SHOW_CPU = "key_overlay_show_cpu"
        private const val KEY_OVERLAY_OPACITY = "key_overlay_opacity"
        private const val KEY_OVERLAY_AUTO_LAUNCH = "key_overlay_auto_launch"
        private const val KEY_OVERLAY_SNAP_EDGES = "key_overlay_snap_edges"
        private const val KEY_OVERLAY_ACCENT_HEX = "key_overlay_accent_hex"
        private const val KEY_OVERLAY_POS_X = "key_overlay_pos_x"
        private const val KEY_OVERLAY_POS_Y = "key_overlay_pos_y"
    }
}
