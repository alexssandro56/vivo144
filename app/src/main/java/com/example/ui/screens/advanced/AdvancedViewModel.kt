package com.example.ui.screens.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.DeviceSpecs
import com.example.data.models.FpsConfig
import com.example.data.models.PerformanceProfile
import com.example.data.repositories.AdbPermissionRequiredException
import com.example.data.repositories.FpsUnlockRepository
import com.example.data.repositories.PerformanceProfileRepository
import com.example.domain.usecases.CheckAdbConnectionUseCase
import com.example.domain.usecases.RestoreDefaultsUseCase
import com.example.domain.usecases.UnlockFpsUseCase
import com.example.platform.shizuku.ShizukuDetector
import com.example.platform.shizuku.SystemAccessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedUiState(
    val deviceSpecs: DeviceSpecs? = null,
    val fpsConfig: FpsConfig = FpsConfig.DEFAULT,
    val selectedRefreshRate: Int = 144,
    val selectedProfile: PerformanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
    val systemAccessInfo: SystemAccessInfo = SystemAccessInfo(
        isAdbEnabled = false,
        isWirelessDebuggingEnabled = false,
        hasWriteSecureSettings = false,
        isShizukuInstalled = false,
        isShizukuRunning = false,
        isRootDetected = false,
        adbStatusText = "Checking…",
        wirelessDebugStatusText = "Disabled",
        shizukuStatusText = "Not installed",
        rootStatusText = "Not detected"
    ),
    val isApplying: Boolean = false,
    val isRestoreDialogOpen: Boolean = false,
    val isMultiTurboOptimized: Boolean = true,
    val userNotification: String? = null
)

class AdvancedViewModel(
    private val fpsUnlockRepository: FpsUnlockRepository,
    private val unlockFpsUseCase: UnlockFpsUseCase,
    private val restoreDefaultsUseCase: RestoreDefaultsUseCase,
    private val performanceProfileRepository: PerformanceProfileRepository,
    private val shizukuDetector: ShizukuDetector,
    private val checkAdbConnectionUseCase: CheckAdbConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedUiState())
    val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val specs = fpsUnlockRepository.checkCompatibility()
                val config = fpsUnlockRepository.getCurrentConfiguration()
                val activeProf = performanceProfileRepository.getActiveProfile()
                val adbStatus = checkAdbConnectionUseCase()

                val accessInfo = shizukuDetector.getSystemAccessInfo(
                    isAdbConnected = adbStatus.isUsbDebuggingEnabled || adbStatus.isAdbAvailable,
                    isWirelessDebugging = adbStatus.isWirelessDebuggingEnabled,
                    hasWriteSecureSettings = adbStatus.hasWriteSecureSettings
                )

                _uiState.update {
                    it.copy(
                        deviceSpecs = specs,
                        fpsConfig = config,
                        selectedRefreshRate = config.refreshRateHz.toInt().coerceIn(60, 144),
                        selectedProfile = activeProf,
                        systemAccessInfo = accessInfo
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotification = "Failed to load settings: ${e.localizedMessage}") }
            }
        }
    }

    fun selectRefreshRate(rate: Int) {
        _uiState.update { it.copy(selectedRefreshRate = rate) }
    }

    fun selectProfile(profile: PerformanceProfile) {
        _uiState.update {
            it.copy(
                selectedProfile = profile,
                selectedRefreshRate = profile.refreshRate
            )
        }
    }

    fun applyRefreshRate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val rate = _uiState.value.selectedRefreshRate
            val result = unlockFpsUseCase(targetFps = rate, refreshRateHz = rate)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userNotification = "Applied $rate Hz display refresh rate"
                    )
                }
                loadData()
            }.onFailure { err ->
                val msg = if (err is AdbPermissionRequiredException) {
                    "ADB authorization required to modify display rate"
                } else {
                    err.localizedMessage ?: "Unable to change refresh rate"
                }
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userNotification = msg
                    )
                }
            }
        }
    }

    fun applyProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val profile = _uiState.value.selectedProfile
            performanceProfileRepository.setActiveProfile(profile)
            val result = unlockFpsUseCase(targetFps = profile.targetFps, refreshRateHz = profile.refreshRate)
            val isHighPerf = profile.id == PerformanceProfile.ULTRA_PERFORMANCE.id
            val successMsg = if (isHighPerf) {
                "High Performance Mode enabled (144 FPS & Multi-Turbo Active)"
            } else {
                "Profile applied: ${profile.name} (${profile.targetFps} FPS / ${profile.refreshRate} Hz)"
            }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userNotification = successMsg
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userNotification = "Could not apply ${profile.name}: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun toggleMultiTurbo(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isMultiTurboOptimized = enabled,
                userNotification = if (enabled) "Vivo Multi-Turbo priority enabled" else "Vivo Multi-Turbo reset to system auto"
            )
        }
    }

    fun toggleHighPerformanceDirectly(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                selectProfile(PerformanceProfile.ULTRA_PERFORMANCE)
                applyProfile()
            } else {
                selectProfile(PerformanceProfile.BALANCED)
                applyProfile()
            }
        }
    }

    fun showRestoreDialog(show: Boolean) {
        _uiState.update { it.copy(isRestoreDialogOpen = show) }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val result = restoreDefaultsUseCase()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isRestoreDialogOpen = false,
                        userNotification = "System display settings restored to manufacturer default"
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isRestoreDialogOpen = false,
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
            performanceProfileRepository: PerformanceProfileRepository,
            shizukuDetector: ShizukuDetector,
            checkAdbConnectionUseCase: CheckAdbConnectionUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdvancedViewModel(
                    fpsUnlockRepository = fpsUnlockRepository,
                    unlockFpsUseCase = unlockFpsUseCase,
                    restoreDefaultsUseCase = restoreDefaultsUseCase,
                    performanceProfileRepository = performanceProfileRepository,
                    shizukuDetector = shizukuDetector,
                    checkAdbConnectionUseCase = checkAdbConnectionUseCase
                ) as T
            }
        }
    }
}
