package com.example.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.CompatibilityResult
import com.example.data.models.DeviceSpecs
import com.example.data.models.DiagnosticItem
import com.example.data.models.GroundedCompatibilityReport
import com.example.data.repositories.CompatibilityRepository
import com.example.data.repositories.GroundedCompatibilityRepository
import com.example.domain.usecases.GetDeviceDiagnosticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val deviceSpecs: DeviceSpecs? = null,
    val diagnosticItems: List<DiagnosticItem> = emptyList(),
    val compatibilityResult: CompatibilityResult? = null,
    val isCompatibilityDialogOpen: Boolean = false,
    val groundedReport: GroundedCompatibilityReport? = null,
    val isCheckingGroundedCompatibility: Boolean = false,
    val isLoading: Boolean = true,
    val userNotification: String? = null
)

class DiagnosticsViewModel(
    private val getDeviceDiagnosticsUseCase: GetDeviceDiagnosticsUseCase,
    private val compatibilityRepository: CompatibilityRepository,
    private val groundedCompatibilityRepository: GroundedCompatibilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        loadDiagnostics()
    }

    fun loadDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val specs = getDeviceDiagnosticsUseCase.getDeviceSpecs()
                val items = getDeviceDiagnosticsUseCase.getDiagnosticItems()
                val compat = compatibilityRepository.evaluateCompatibility(144)
                _uiState.update {
                    it.copy(
                        deviceSpecs = specs,
                        diagnosticItems = items,
                        compatibilityResult = compat,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNotification = "Failed to load diagnostics: ${e.localizedMessage}"
                    )
                }
            }
        }
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

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    companion object {
        fun provideFactory(
            getDeviceDiagnosticsUseCase: GetDeviceDiagnosticsUseCase,
            compatibilityRepository: CompatibilityRepository,
            groundedCompatibilityRepository: GroundedCompatibilityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DiagnosticsViewModel(
                    getDeviceDiagnosticsUseCase = getDeviceDiagnosticsUseCase,
                    compatibilityRepository = compatibilityRepository,
                    groundedCompatibilityRepository = groundedCompatibilityRepository
                ) as T
            }
        }
    }
}
