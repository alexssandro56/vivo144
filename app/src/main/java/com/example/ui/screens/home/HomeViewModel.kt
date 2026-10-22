package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.BatteryInfo
import com.example.data.models.CompatibilityResult
import com.example.data.models.DeviceSpecs
import com.example.data.models.FpsConfig
import com.example.data.models.FpsMetrics
import com.example.data.models.GroundedCompatibilityReport
import com.example.data.models.PerformanceProfile
import com.example.data.models.SystemPerformanceMetrics
import com.example.data.models.ThermalInfo
import com.example.data.models.UnlockStatus
import com.example.data.repositories.AdbPermissionRequiredException
import com.example.data.repositories.BatteryRepository
import com.example.data.repositories.CompatibilityRepository
import com.example.data.repositories.FpsUnlockRepository
import com.example.data.repositories.GroundedCompatibilityRepository
import com.example.data.repositories.PerformanceProfileRepository
import com.example.data.repositories.SystemPerformanceRepository
import com.example.data.repositories.ThermalRepository
import com.example.domain.usecases.RestoreDefaultsUseCase
import com.example.domain.usecases.UnlockFpsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val deviceSpecs: DeviceSpecs? = null,
    val fpsConfig: FpsConfig = FpsConfig.DEFAULT,
    val unlockStatus: UnlockStatus = UnlockStatus.Ready,
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val thermalInfo: ThermalInfo = ThermalInfo(),
    val compatibilityResult: CompatibilityResult? = null,
    val fpsMetrics: FpsMetrics = FpsMetrics(),
    val systemMetrics: SystemPerformanceMetrics = SystemPerformanceMetrics(),
    val activeProfile: PerformanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
    val isRestoreDialogOpen: Boolean = false,
    val isCompatibilityDialogOpen: Boolean = false,
    val groundedReport: GroundedCompatibilityReport? = null,
    val isCheckingGroundedCompatibility: Boolean = false,
    val userNotification: String? = null
)

class HomeViewModel(
    private val fpsUnlockRepository: FpsUnlockRepository,
    private val unlockFpsUseCase: UnlockFpsUseCase,
    private val restoreDefaultsUseCase: RestoreDefaultsUseCase,
    private val batteryRepository: BatteryRepository,
    private val thermalRepository: ThermalRepository,
    private val compatibilityRepository: CompatibilityRepository,
    private val systemPerformanceRepository: SystemPerformanceRepository,
    private val performanceProfileRepository: PerformanceProfileRepository,
    private val groundedCompatibilityRepository: GroundedCompatibilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeBattery()
        observeThermal()
        observeFpsConfig()
        observeActiveProfile()
        samplePerformance()
    }

    private fun observeBattery() {
        viewModelScope.launch {
            batteryRepository.observeBattery().collect { battery ->
                _uiState.update { it.copy(batteryInfo = battery) }
            }
        }
    }

    private fun observeThermal() {
        viewModelScope.launch {
            thermalRepository.observeThermal().collect { thermal ->
                _uiState.update { it.copy(thermalInfo = thermal) }
            }
        }
    }

    private fun observeFpsConfig() {
        viewModelScope.launch {
            fpsUnlockRepository.fpsConfig.collect { config ->
                _uiState.update { it.copy(fpsConfig = config) }
            }
        }
    }

    private fun observeActiveProfile() {
        viewModelScope.launch {
            performanceProfileRepository.activeProfile.collect { profile ->
                _uiState.update { it.copy(activeProfile = profile) }
            }
        }
    }

    private fun samplePerformance() {
        viewModelScope.launch {
            val metrics = systemPerformanceRepository.sampleSystemPerformance()
            _uiState.update {
                it.copy(
                    systemMetrics = metrics,
                    fpsMetrics = FpsMetrics(
                        currentFps = _uiState.value.fpsConfig.targetFps,
                        refreshRateHz = _uiState.value.fpsConfig.refreshRateHz.toFloat()
                    )
                )
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val specs = fpsUnlockRepository.checkCompatibility()
                val currentConfig = fpsUnlockRepository.getCurrentConfiguration()
                val compatResult = compatibilityRepository.evaluateCompatibility(currentConfig.targetFps)
                val battery = batteryRepository.getCurrentBattery()
                val thermal = thermalRepository.getCurrentThermal()
                val activeProf = performanceProfileRepository.getActiveProfile()
                val sysMetrics = systemPerformanceRepository.sampleSystemPerformance()

                _uiState.update {
                    it.copy(
                        deviceSpecs = specs,
                        fpsConfig = currentConfig,
                        compatibilityResult = compatResult,
                        batteryInfo = battery,
                        thermalInfo = thermal,
                        activeProfile = activeProf,
                        systemMetrics = sysMetrics,
                        fpsMetrics = FpsMetrics(
                            currentFps = currentConfig.targetFps,
                            refreshRateHz = currentConfig.refreshRateHz.toFloat()
                        ),
                        unlockStatus = if (currentConfig.isUnlocked) {
                            UnlockStatus.Unlocked(currentConfig.targetFps, currentConfig.refreshRateHz)
                        } else {
                            UnlockStatus.Ready
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userNotification = "Failed to refresh device telemetry: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun unlock144Fps() {
        viewModelScope.launch {
            val is144 = _uiState.value.deviceSpecs?.is144HzSupported ?: true
            val maxRate = _uiState.value.deviceSpecs?.maxSupportedRefreshRate?.toInt() ?: 144
            val targetFps = if (is144) 144 else maxRate

            _uiState.update {
                it.copy(
                    unlockStatus = UnlockStatus.Applying(
                        progress = 0.5f,
                        stepDescription = "Applying recommended $targetFps FPS / $targetFps Hz display configuration…"
                    )
                )
            }

            delay(350)

            val result = unlockFpsUseCase(targetFps = targetFps, refreshRateHz = targetFps)
            result.onSuccess { status ->
                performanceProfileRepository.setActiveProfile(PerformanceProfile.ULTRA_PERFORMANCE)
                val successMessage = if (is144) {
                    "144 FPS unlocked successfully! Ultra Performance active."
                } else {
                    "$targetFps FPS unlocked successfully (Device Panel Maximum)."
                }
                _uiState.update {
                    it.copy(
                        unlockStatus = status,
                        userNotification = successMessage
                    )
                }
                loadData()
            }.onFailure { err ->
                if (err is AdbPermissionRequiredException) {
                    _uiState.update {
                        it.copy(
                            unlockStatus = UnlockStatus.Error(
                                message = "ADB authorization required (WRITE_SECURE_SETTINGS)",
                                requiresAdbPermission = true,
                                commandToRun = err.command
                            ),
                            userNotification = "ADB authorization required to unlock $targetFps FPS."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            unlockStatus = UnlockStatus.Error(
                                message = err.localizedMessage ?: "Unable to apply configuration"
                            ),
                            userNotification = "Unable to apply configuration: ${err.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun toggleHighPerformanceMode(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val profile = PerformanceProfile.ULTRA_PERFORMANCE
                performanceProfileRepository.setActiveProfile(profile)
                val is144 = _uiState.value.deviceSpecs?.is144HzSupported ?: true
                val targetRate = if (is144) 144 else (_uiState.value.deviceSpecs?.maxSupportedRefreshRate?.toInt() ?: 120)
                val result = unlockFpsUseCase(targetFps = targetRate, refreshRateHz = targetRate)
                result.onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            unlockStatus = status,
                            activeProfile = profile,
                            userNotification = "High Performance Mode enabled ($targetRate FPS & Multi-Turbo Active)"
                        )
                    }
                    loadData()
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            activeProfile = profile,
                            userNotification = "High Performance Mode enabled (Ultra Profile)"
                        )
                    }
                    loadData()
                }
            } else {
                val profile = PerformanceProfile.BALANCED
                performanceProfileRepository.setActiveProfile(profile)
                val targetRate = 120
                val result = unlockFpsUseCase(targetFps = targetRate, refreshRateHz = targetRate)
                result.onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            unlockStatus = status,
                            activeProfile = profile,
                            userNotification = "High Performance Mode disabled (Switched to Balanced 120 FPS)"
                        )
                    }
                    loadData()
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            activeProfile = profile,
                            userNotification = "High Performance Mode disabled (Balanced Profile Active)"
                        )
                    }
                    loadData()
                }
            }
        }
    }

    fun selectProfile(profile: PerformanceProfile) {
        viewModelScope.launch {
            performanceProfileRepository.setActiveProfile(profile)
            val result = unlockFpsUseCase(targetFps = profile.targetFps, refreshRateHz = profile.refreshRate)
            val message = "Applied ${profile.name} Mode (${profile.targetFps} FPS / ${profile.refreshRate} Hz)"
            result.onSuccess { status ->
                _uiState.update {
                    it.copy(
                        unlockStatus = status,
                        activeProfile = profile,
                        userNotification = message
                    )
                }
                loadData()
            }.onFailure {
                _uiState.update {
                    it.copy(
                        activeProfile = profile,
                        userNotification = message
                    )
                }
                loadData()
            }
        }
    }

    fun showRestoreDialog(show: Boolean) {
        _uiState.update { it.copy(isRestoreDialogOpen = show) }
    }

    fun showCompatibilityDialog(show: Boolean) {
        _uiState.update { it.copy(isCompatibilityDialogOpen = show) }
        if (show && _uiState.value.groundedReport == null) {
            checkGroundedCompatibility()
        }
    }

    fun checkGroundedCompatibility(customQuery: String? = null) {
        val specs = _uiState.value.deviceSpecs ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingGroundedCompatibility = true) }
            val report = groundedCompatibilityRepository.checkGroundedCompatibility(specs, customQuery)
            _uiState.update {
                it.copy(
                    groundedReport = report,
                    isCheckingGroundedCompatibility = false,
                    userNotification = if (report.isOfficiallyListed) {
                        "Device verified: ${report.deviceModelChecked}"
                    } else {
                        "Alert: ${report.deviceModelChecked} not in official Vivo/iQOO catalog"
                    }
                )
            }
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val result = restoreDefaultsUseCase()
            result.onSuccess { status ->
                _uiState.update {
                    it.copy(
                        unlockStatus = status,
                        userNotification = "Default display settings restored"
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        userNotification = "Restore error: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    companion object {
        fun provideFactory(
            fpsUnlockRepository: FpsUnlockRepository,
            unlockFpsUseCase: UnlockFpsUseCase,
            restoreDefaultsUseCase: RestoreDefaultsUseCase,
            batteryRepository: BatteryRepository,
            thermalRepository: ThermalRepository,
            compatibilityRepository: CompatibilityRepository,
            systemPerformanceRepository: SystemPerformanceRepository,
            performanceProfileRepository: PerformanceProfileRepository,
            groundedCompatibilityRepository: GroundedCompatibilityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    fpsUnlockRepository = fpsUnlockRepository,
                    unlockFpsUseCase = unlockFpsUseCase,
                    restoreDefaultsUseCase = restoreDefaultsUseCase,
                    batteryRepository = batteryRepository,
                    thermalRepository = thermalRepository,
                    compatibilityRepository = compatibilityRepository,
                    systemPerformanceRepository = systemPerformanceRepository,
                    performanceProfileRepository = performanceProfileRepository,
                    groundedCompatibilityRepository = groundedCompatibilityRepository
                ) as T
            }
        }
    }
}
