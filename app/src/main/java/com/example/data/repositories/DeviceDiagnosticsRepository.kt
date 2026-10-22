package com.example.data.repositories

import com.example.data.models.AdbStatus
import com.example.data.models.DeviceSpecs
import com.example.data.models.DiagnosticItem

interface DeviceDiagnosticsRepository {
    suspend fun getDeviceSpecs(): DeviceSpecs
    suspend fun getAdbStatus(): AdbStatus
    suspend fun getDiagnosticItems(): List<DiagnosticItem>
}
