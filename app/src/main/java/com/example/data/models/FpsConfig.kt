package com.example.data.models

data class FpsConfig(
    val targetFps: Int = 144,
    val refreshRateHz: Int = 144,
    val isMultiTurboGameModeEnabled: Boolean = true,
    val isPeakRateForced: Boolean = true,
    val currentFps: Int = 60,
    val isUnlocked: Boolean = false
) {
    companion object {
        val AVAILABLE_FPS_TARGETS = listOf(60, 90, 120, 144)
        val AVAILABLE_REFRESH_RATES = listOf(60, 90, 120, 144)
        val DEFAULT = FpsConfig(
            targetFps = 144,
            refreshRateHz = 144,
            isMultiTurboGameModeEnabled = true,
            isPeakRateForced = true,
            currentFps = 60,
            isUnlocked = false
        )
    }
}
