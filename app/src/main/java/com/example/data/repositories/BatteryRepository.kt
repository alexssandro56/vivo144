package com.example.data.repositories

import com.example.data.models.BatteryInfo
import com.example.platform.battery.BatteryMonitor
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun observeBattery(): Flow<BatteryInfo>
    fun getCurrentBattery(): BatteryInfo
}

class BatteryRepositoryImpl(
    private val batteryMonitor: BatteryMonitor
) : BatteryRepository {
    override fun observeBattery(): Flow<BatteryInfo> = batteryMonitor.observeBattery()
    override fun getCurrentBattery(): BatteryInfo = batteryMonitor.getCurrentBattery()
}
