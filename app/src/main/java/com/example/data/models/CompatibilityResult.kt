package com.example.data.models

enum class CheckStatus {
    PASSED,
    WARNING,
    FAILED,
    UNAVAILABLE
}

data class CompatibilityCheckItem(
    val title: String,
    val description: String,
    val status: CheckStatus,
    val details: String,
    val isOptional: Boolean = false
)

data class CompatibilityResult(
    val manufacturer: String,
    val model: String,
    val brand: String = "",
    val deviceName: String = "",
    val androidVersion: String,
    val maxRefreshRate: Float,
    val currentRefreshRate: Float,
    val isVivoOrIqoo: Boolean,
    val is144HzDisplayAvailable: Boolean,
    val isAndroidCompatible: Boolean,
    val isAdbConnected: Boolean,
    val isShizukuAvailable: Boolean,
    val hasWriteSecureSettings: Boolean,
    val isRootDetected: Boolean,
    val items: List<CompatibilityCheckItem>,
    val passedChecksCount: Int,
    val totalRequiredChecks: Int,
    val isReadyFor144Fps: Boolean,
    val readinessSummary: String,
    val missingRequirements: List<String>
)
