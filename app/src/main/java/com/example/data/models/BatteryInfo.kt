package com.example.data.models

enum class ChargingState(val displayName: String) {
    CHARGING_AC("Charging (AC Fast Charger)"),
    CHARGING_USB("Charging (USB)"),
    CHARGING_WIRELESS("Charging (Wireless)"),
    CHARGING("Charging"),
    DISCHARGING("Not charging (On battery)"),
    NOT_CHARGING("Connected, not charging"),
    FULL("Battery full (100%)"),
    UNKNOWN("Unknown")
}

data class BatteryInfo(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val chargingState: ChargingState = ChargingState.DISCHARGING,
    val temperatureCelsius: Float? = null,
    val voltageMv: Int? = null,
    val health: String = "Good",
    val technology: String = "Li-ion"
) {
    val formattedTemperature: String
        get() = temperatureCelsius?.let { String.format("%.1f°C", it) } ?: "Temperature unavailable"

    val thermalSeverity: TemperatureStatus
        get() = when {
            temperatureCelsius == null -> TemperatureStatus.UNAVAILABLE
            temperatureCelsius < 40f -> TemperatureStatus.OPTIMAL
            temperatureCelsius < 45f -> TemperatureStatus.WARM
            temperatureCelsius < 50f -> TemperatureStatus.HIGH
            else -> TemperatureStatus.CRITICAL
        }
}
