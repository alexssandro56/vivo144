package com.example.data.repositories

import com.example.data.models.ThermalInfo
import com.example.platform.thermal.ThermalMonitor
import kotlinx.coroutines.flow.Flow

interface ThermalRepository {
    fun observeThermal(): Flow<ThermalInfo>
    fun getCurrentThermal(): ThermalInfo
}

class ThermalRepositoryImpl(
    private val thermalMonitor: ThermalMonitor
) : ThermalRepository {
    override fun observeThermal(): Flow<ThermalInfo> = thermalMonitor.observeThermal()
    override fun getCurrentThermal(): ThermalInfo = thermalMonitor.getCurrentThermal()
}
