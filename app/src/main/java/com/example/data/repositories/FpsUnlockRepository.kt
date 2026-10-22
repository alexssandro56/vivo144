package com.example.data.repositories

import com.example.data.models.DeviceSpecs
import com.example.data.models.FpsConfig
import com.example.data.models.UnlockStatus
import kotlinx.coroutines.flow.StateFlow

interface FpsUnlockRepository {
    val fpsConfig: StateFlow<FpsConfig>
    suspend fun checkCompatibility(): DeviceSpecs
    suspend fun getCurrentConfiguration(): FpsConfig
    suspend fun applyFpsConfiguration(targetFps: Int, refreshRateHz: Int): Result<UnlockStatus>
    suspend fun restoreDefaults(): Result<UnlockStatus>
}
