package com.example.data.models

data class GameProfile(
    val id: Long = 0,
    val gameName: String,
    val packageName: String,
    val targetFps: Int = 144,
    val refreshRate: Int = 144,
    val performanceProfile: PerformanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
    val enabled: Boolean = true
) {
    companion object {
        val DEFAULT_GAMES = listOf(
            GameProfile(
                id = 1,
                gameName = "PUBG Mobile",
                packageName = "com.tencent.ig",
                targetFps = 144,
                refreshRate = 144,
                performanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
                enabled = true
            ),
            GameProfile(
                id = 2,
                gameName = "Call of Duty: Mobile",
                packageName = "com.activision.callofduty.shooter",
                targetFps = 144,
                refreshRate = 144,
                performanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
                enabled = true
            ),
            GameProfile(
                id = 3,
                gameName = "Genshin Impact",
                packageName = "com.miHoYo.GenshinImpact",
                targetFps = 120,
                refreshRate = 120,
                performanceProfile = PerformanceProfile.BALANCED,
                enabled = true
            ),
            GameProfile(
                id = 4,
                gameName = "Free Fire MAX",
                packageName = "com.dts.freefiremax",
                targetFps = 144,
                refreshRate = 144,
                performanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
                enabled = true
            ),
            GameProfile(
                id = 5,
                gameName = "Honor of Kings",
                packageName = "com.levelinfinite.sgameGlobal",
                targetFps = 120,
                refreshRate = 120,
                performanceProfile = PerformanceProfile.BALANCED,
                enabled = true
            )
        )
    }
}
