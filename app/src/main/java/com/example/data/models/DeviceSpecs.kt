package com.example.data.models

data class DeviceSpecs(
    val model: String,
    val manufacturer: String,
    val brand: String = "",
    val deviceName: String = "",
    val androidVersion: String,
    val sdkInt: Int,
    val buildNumber: String,
    val currentRefreshRate: Float,
    val supportedRefreshRates: List<Float>,
    val isVivoOrIqoo: Boolean,
    val is144HzSupported: Boolean,
    val hasWriteSecureSettingsPermission: Boolean,
    val isAdbConnected: Boolean,
    val customOsBrand: String = "OriginOS / FuntouchOS"
) {
    val maxSupportedRefreshRate: Float
        get() = supportedRefreshRates.maxOrNull() ?: currentRefreshRate

    val displaySummary: String
        get() {
            val maxRate = maxSupportedRefreshRate.toInt()
            return "$maxRate Hz Display"
        }
}

