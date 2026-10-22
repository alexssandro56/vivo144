package com.example.data.models

data class PerformanceProfile(
    val id: String,
    val name: String,
    val targetFps: Int,
    val refreshRate: Int,
    val description: String,
    val powerMode: String,
    val recommendedFor: String = ""
) {
    companion object {
        val ULTRA_PERFORMANCE = PerformanceProfile(
            id = "ultra_performance",
            name = "Ultra Performance",
            targetFps = 144,
            refreshRate = 144,
            description = "Maximum frame pacing and peak refresh rate for competitive esports titles.",
            powerMode = "Performance Mode / Multi-Turbo",
            recommendedFor = "Competitive Gaming (PUBG, COD, Free Fire)"
        )

        val BALANCED = PerformanceProfile(
            id = "balanced",
            name = "Balanced",
            targetFps = 120,
            refreshRate = 120,
            description = "Balanced performance, silky fluid animations and moderate battery consumption.",
            powerMode = "Balanced Mode",
            recommendedFor = "Daily Gaming & UI Navigation"
        )

        val BATTERY_SAVER = PerformanceProfile(
            id = "battery_saver",
            name = "Battery Saver",
            targetFps = 60,
            refreshRate = 60,
            description = "Lower power consumption and reduced thermal output for extended screen-on time.",
            powerMode = "Standard / Power Saver",
            recommendedFor = "Long Gaming Sessions / Low Battery"
        )

        val ALL_PROFILES = listOf(ULTRA_PERFORMANCE, BALANCED, BATTERY_SAVER)

        fun getById(id: String): PerformanceProfile {
            return ALL_PROFILES.find { it.id == id } ?: BALANCED
        }
    }
}
