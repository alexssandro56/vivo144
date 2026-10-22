package com.example.data.models

enum class TemperatureStatus(val label: String) {
    OPTIMAL("Optimal"),
    WARM("Warm"),
    HIGH("High"),
    CRITICAL("Critical"),
    UNAVAILABLE("Unavailable")
}

data class ThermalInfo(
    val temperatureCelsius: Float? = null,
    val thermalStatus: String = "Normal",
    val thermalStatusCode: Int = 0,
    val isThrottling: Boolean = false,
    val coolingRecommended: Boolean = false,
    val thermalSeverity: TemperatureStatus = TemperatureStatus.OPTIMAL
) {
    val formattedTemperature: String
        get() = temperatureCelsius?.let { String.format("%.1f°C", it) } ?: "Temperature unavailable"

    companion object {
        fun fromCelsius(temp: Float?, androidThermalCode: Int = 0): ThermalInfo {
            val status = when {
                temp == null -> TemperatureStatus.UNAVAILABLE
                temp < 40f -> TemperatureStatus.OPTIMAL
                temp < 45f -> TemperatureStatus.WARM
                temp < 50f -> TemperatureStatus.HIGH
                else -> TemperatureStatus.CRITICAL
            }

            val statusDescription = when (androidThermalCode) {
                0 -> "None (Operating nominally)"
                1 -> "Light (Minimal cooling needed)"
                2 -> "Moderate (Framerate stable)"
                3 -> "Severe (System throttling active)"
                4 -> "Critical (Heavy hardware throttling)"
                5 -> "Emergency (Display throttling)"
                6 -> "Shutdown (Extreme thermal overload)"
                else -> if (temp != null && temp >= 45f) "Throttling risk" else "Nominal"
            }

            return ThermalInfo(
                temperatureCelsius = temp,
                thermalStatus = statusDescription,
                thermalStatusCode = androidThermalCode,
                isThrottling = androidThermalCode >= 3 || (temp != null && temp >= 48f),
                coolingRecommended = androidThermalCode >= 2 || (temp != null && temp >= 44f),
                thermalSeverity = status
            )
        }
    }
}
