package com.example.domain.usecases

import com.example.data.models.AdbStatus
import com.example.data.models.DeviceSpecs
import com.example.data.models.DiagnosticItem
import com.example.data.repositories.DeviceDiagnosticsRepository

class GetDeviceDiagnosticsUseCase(private val repository: DeviceDiagnosticsRepository) {
    suspend fun getDiagnosticItems(): List<DiagnosticItem> = repository.getDiagnosticItems()
    suspend fun getDeviceSpecs(): DeviceSpecs = repository.getDeviceSpecs()
    suspend fun getAdbStatus(): AdbStatus = repository.getAdbStatus()
}
