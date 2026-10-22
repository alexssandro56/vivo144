package com.example.platform.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.data.models.ThermalInfo
import com.example.platform.battery.BatteryMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ThermalMonitor(
    private val context: Context,
    private val batteryMonitor: BatteryMonitor
) {

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun observeThermal(): Flow<ThermalInfo> = callbackFlow {
        // Battery temperature is universally available across all Android versions
        val initialBattery = batteryMonitor.getCurrentBattery()
        val initialThermalStatus = getSystemThermalStatus()
        trySend(ThermalInfo.fromCelsius(initialBattery.temperatureCelsius, initialThermalStatus))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                val currentBattery = batteryMonitor.getCurrentBattery()
                trySend(ThermalInfo.fromCelsius(currentBattery.temperatureCelsius, status))
            }
            try {
                powerManager.addThermalStatusListener(listener)
            } catch (_: Exception) { }

            awaitClose {
                try {
                    powerManager.removeThermalStatusListener(listener)
                } catch (_: Exception) { }
            }
        } else {
            awaitClose { }
        }
    }

    fun getCurrentThermal(): ThermalInfo {
        val battery = batteryMonitor.getCurrentBattery()
        val thermalStatusCode = getSystemThermalStatus()
        return ThermalInfo.fromCelsius(battery.temperatureCelsius, thermalStatusCode)
    }

    private fun getSystemThermalStatus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                powerManager.currentThermalStatus
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }
    }
}
