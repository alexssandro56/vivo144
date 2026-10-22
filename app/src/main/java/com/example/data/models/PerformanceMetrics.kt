package com.example.data.models

data class FpsMetrics(
    val currentFps: Int = 144,
    val averageFps: Int = 142,
    val minFps: Int = 120,
    val maxFps: Int = 144,
    val frameTimeMs: Float = 6.9f,
    val refreshRateHz: Float = 144.0f,
    val isStable: Boolean = true,
    val stabilityStatus: String = "Stable",
    val fpsHistory: List<Int> = listOf(144, 144, 143, 144, 142, 144, 144, 143, 144, 144),
    val isMonitoring: Boolean = false,
    val accessNote: String? = null
)

data class SystemPerformanceMetrics(
    val cpuUsagePercent: Int? = null,
    val gpuUsagePercent: Int? = null,
    val gpuRendererName: String? = null,
    val ramUsagePercent: Int = 0,
    val usedRamGb: Float = 0.0f,
    val totalRamGb: Float = 0.0f,
    val availableRamGb: Float = 0.0f,
    val performanceHealth: String = "Optimal",
    val activeThreadsCount: Int = 0,
    val cpuCoresCount: Int = Runtime.getRuntime().availableProcessors()
) {
    val formattedCpuUsage: String
        get() = cpuUsagePercent?.let { "$it%" } ?: "Unavailable"

    val formattedGpuUsage: String
        get() = gpuUsagePercent?.let { "$it%" } ?: "Unavailable"

    val formattedRamSummary: String
        get() = String.format("%.1f GB / %.1f GB", usedRamGb, totalRamGb)
}
