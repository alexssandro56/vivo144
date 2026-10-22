package com.example.data.repositories

import com.example.data.models.DeviceSpecs
import com.example.data.models.FpsConfig
import com.example.data.models.UnlockStatus
import com.example.platform.adb.AdbManager
import com.example.platform.device.DeviceInfoProvider
import com.example.platform.system.SettingsApplyResult
import com.example.platform.system.SystemSettingsController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class FpsUnlockRepositoryImpl(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val systemSettingsController: SystemSettingsController,
    private val adbManager: AdbManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : FpsUnlockRepository {

    override val fpsConfig: StateFlow<FpsConfig> = userPreferencesRepository.fpsConfigFlow

    override suspend fun checkCompatibility(): DeviceSpecs {
        val hasSecurePerm = systemSettingsController.hasWriteSecureSettings()
        val adbStatus = adbManager.getAdbStatus()
        return deviceInfoProvider.getDeviceSpecs(
            hasWritePermission = hasSecurePerm,
            isAdbConnected = adbStatus.isAdbAvailable
        )
    }

    override suspend fun getCurrentConfiguration(): FpsConfig {
        return userPreferencesRepository.fpsConfigFlow.value
    }

    override suspend fun applyFpsConfiguration(targetFps: Int, refreshRateHz: Int): Result<UnlockStatus> {
        val deviceSpecs = checkCompatibility()
        val targetRateFloat = refreshRateHz.toFloat()

        // 1. Attempt applying real system settings
        val applyResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            systemSettingsController.applyRefreshRate(
                targetHz = targetRateFloat,
                isVivoDevice = deviceSpecs.isVivoOrIqoo
            )
        }

        return when (applyResult) {
            is SettingsApplyResult.Success -> {
                userPreferencesRepository.saveConfig(
                    userPreferencesRepository.fpsConfigFlow.value.copy(
                        targetFps = targetFps,
                        refreshRateHz = refreshRateHz,
                        currentFps = targetFps,
                        isUnlocked = true
                    )
                )
                Result.success(UnlockStatus.Unlocked(targetFps = targetFps, refreshRateHz = refreshRateHz))
            }
            is SettingsApplyResult.PermissionDenied -> {
                // If not yet granted WRITE_SECURE_SETTINGS, report actionable instruction
                Result.failure(
                    AdbPermissionRequiredException(
                        message = "System permission WRITE_SECURE_SETTINGS required to modify display refresh rate.",
                        command = applyResult.adbGrantCommand
                    )
                )
            }
            is SettingsApplyResult.Failed -> {
                Result.failure(Exception(applyResult.error))
            }
        }
    }

    override suspend fun restoreDefaults(): Result<UnlockStatus> {
        val restoreResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            systemSettingsController.restoreDefaults()
        }
        return when (restoreResult) {
            is SettingsApplyResult.Success -> {
                userPreferencesRepository.resetToDefaults()
                Result.success(UnlockStatus.Restored)
            }
            is SettingsApplyResult.PermissionDenied -> {
                // Reset local configuration state even if system settings require adb
                userPreferencesRepository.resetToDefaults()
                Result.success(UnlockStatus.Restored)
            }
            is SettingsApplyResult.Failed -> {
                userPreferencesRepository.resetToDefaults()
                Result.success(UnlockStatus.Restored)
            }
        }
    }
}

class AdbPermissionRequiredException(
    override val message: String,
    val command: String
) : Exception(message)
