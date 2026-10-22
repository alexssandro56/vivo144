package com.example.ui.screens.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.BatteryInfo
import com.example.data.models.FpsMetrics
import com.example.data.models.OverlayPreferences
import com.example.data.models.PerformanceProfile
import com.example.data.models.SystemPerformanceMetrics
import com.example.data.models.ThermalInfo
import com.example.data.repositories.BatteryRepository
import com.example.data.repositories.FpsUnlockRepository
import com.example.data.repositories.PerformanceProfileRepository
import com.example.data.repositories.SystemPerformanceRepository
import com.example.data.repositories.ThermalRepository
import com.example.data.repositories.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PerformanceUiState(
    val fpsMetrics: FpsMetrics = FpsMetrics(),
    val systemMetrics: SystemPerformanceMetrics = SystemPerformanceMetrics(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val thermalInfo: ThermalInfo = ThermalInfo(),
    val activeProfile: PerformanceProfile = PerformanceProfile.ULTRA_PERFORMANCE,
    val overlayPreferences: OverlayPreferences = OverlayPreferences.DEFAULT,
    val isMonitoringActive: Boolean = true,
    val targetFps: Int = 144,
    val userNotification: String? = null
)

class PerformanceViewModel(
    private val systemPerformanceRepository: SystemPerformanceRepository,
    private val batteryRepository: BatteryRepository,
    private val thermalRepository: ThermalRepository,
    private val performanceProfileRepository: PerformanceProfileRepository,
    private val fpsUnlockRepository: FpsUnlockRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceUiState())
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    private var fpsMonitorJob: Job? = null
    private var sysSamplerJob: Job? = null

    init {
        observeBatteryAndThermal()
        observeProfileAndConfig()
        observeOverlayPreferences()
        startMonitoring()
        startSystemSampler()
    }

    private fun observeBatteryAndThermal() {
        viewModelScope.launch {
            batteryRepository.observeBattery().collect { b ->
                _uiState.update { it.copy(batteryInfo = b) }
            }
        }
        viewModelScope.launch {
            thermalRepository.observeThermal().collect { t ->
                _uiState.update { it.copy(thermalInfo = t) }
            }
        }
    }

    private fun observeProfileAndConfig() {
        viewModelScope.launch {
            performanceProfileRepository.activeProfile.collect { prof ->
                _uiState.update { it.copy(activeProfile = prof, targetFps = prof.targetFps) }
            }
        }
        viewModelScope.launch {
            fpsUnlockRepository.fpsConfig.collect { config ->
                _uiState.update { it.copy(targetFps = config.targetFps) }
            }
        }
    }

    private fun observeOverlayPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.overlayPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(overlayPreferences = prefs) }
            }
        }
    }

    fun updateOverlayPreferences(prefs: OverlayPreferences) {
        userPreferencesRepository.saveOverlayPreferences(prefs)
        _uiState.update {
            it.copy(
                overlayPreferences = prefs,
                userNotification = if (prefs.isOverlayEnabled) "Floating FPS overlay enabled" else "Floating overlay disabled"
            )
        }
    }

    fun startMonitoring() {
        fpsMonitorJob?.cancel()
        _uiState.update {
            it.copy(
                isMonitoringActive = true,
                userNotification = "Real-time FPS sampling active"
            )
        }
        fpsMonitorJob = viewModelScope.launch {
            systemPerformanceRepository.observeFpsMetrics(_uiState.value.targetFps.toFloat()).collect { metrics ->
                _uiState.update { it.copy(fpsMetrics = metrics) }
            }
        }
    }

    fun stopMonitoring() {
        fpsMonitorJob?.cancel()
        fpsMonitorJob = null
        _uiState.update {
            it.copy(
                isMonitoringActive = false,
                fpsMetrics = it.fpsMetrics.copy(isMonitoring = false),
                userNotification = "Live FPS monitoring paused"
            )
        }
    }

    fun toggleMonitoring() {
        if (_uiState.value.isMonitoringActive) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    fun toggleHighPerformanceMode(enabled: Boolean) {
        viewModelScope.launch {
            val profile = if (enabled) PerformanceProfile.ULTRA_PERFORMANCE else PerformanceProfile.BALANCED
            performanceProfileRepository.setActiveProfile(profile)
            _uiState.update {
                it.copy(
                    activeProfile = profile,
                    targetFps = profile.targetFps,
                    userNotification = if (enabled) {
                        "High Performance Mode active (144 FPS / 144 Hz)"
                    } else {
                        "High Performance Mode disabled (Balanced 120 FPS)"
                    }
                )
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    private fun startSystemSampler() {
        sysSamplerJob = viewModelScope.launch {
            while (isActive) {
                val metrics = systemPerformanceRepository.sampleSystemPerformance()
                _uiState.update { it.copy(systemMetrics = metrics) }
                delay(1500)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            val battery = batteryRepository.getCurrentBattery()
            val thermal = thermalRepository.getCurrentThermal()
            val metrics = systemPerformanceRepository.sampleSystemPerformance()
            _uiState.update {
                it.copy(
                    batteryInfo = battery,
                    thermalInfo = thermal,
                    systemMetrics = metrics
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fpsMonitorJob?.cancel()
        sysSamplerJob?.cancel()
    }

    companion object {
        fun provideFactory(
            systemPerformanceRepository: SystemPerformanceRepository,
            batteryRepository: BatteryRepository,
            thermalRepository: ThermalRepository,
            performanceProfileRepository: PerformanceProfileRepository,
            fpsUnlockRepository: FpsUnlockRepository,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PerformanceViewModel(
                    systemPerformanceRepository = systemPerformanceRepository,
                    batteryRepository = batteryRepository,
                    thermalRepository = thermalRepository,
                    performanceProfileRepository = performanceProfileRepository,
                    fpsUnlockRepository = fpsUnlockRepository,
                    userPreferencesRepository = userPreferencesRepository
                ) as T
            }
        }
    }
}
