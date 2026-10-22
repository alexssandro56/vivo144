package com.example.ui.screens.adb

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.AdbStatus
import com.example.domain.usecases.CheckAdbConnectionUseCase
import com.example.platform.adb.AdbManager
import com.example.platform.adb.WirelessConnectResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdbSetupTab {
    INSTRUCTIONS,
    WIRELESS_CONNECT,
    COMMANDS
}

data class AdbUiState(
    val adbStatus: AdbStatus? = null,
    val selectedTab: AdbSetupTab = AdbSetupTab.INSTRUCTIONS,
    val isRefreshing: Boolean = false,
    val isConnecting: Boolean = false,
    val wirelessPort: String = "5555",
    val wirelessPairingCode: String = "",
    val wirelessHost: String = "127.0.0.1",
    val lastConnectResult: WirelessConnectResult? = null,
    val toastMessage: String? = null
)

class AdbViewModel(
    private val checkAdbConnectionUseCase: CheckAdbConnectionUseCase,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdbUiState())
    val uiState: StateFlow<AdbUiState> = _uiState.asStateFlow()

    init {
        refreshStatus(showToastOnCheck = false)
    }

    fun selectTab(tab: AdbSetupTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateWirelessPort(port: String) {
        _uiState.update { it.copy(wirelessPort = port) }
    }

    fun updateWirelessPairingCode(code: String) {
        _uiState.update { it.copy(wirelessPairingCode = code) }
    }

    fun refreshStatus(showToastOnCheck: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(250) // brief smoothing animation
            val status = checkAdbConnectionUseCase()

            val feedback = if (showToastOnCheck) {
                when {
                    status.hasWriteSecureSettings -> "Status verified: WRITE_SECURE_SETTINGS is active & authorized!"
                    status.isUsbDebuggingEnabled -> "ADB debugging is active, but WRITE_SECURE_SETTINGS permission is pending."
                    status.isDeveloperOptionsEnabled -> "Developer Options enabled. Toggle USB or Wireless Debugging to connect."
                    else -> "Connection status: Not connected. Enable Developer Options & USB Debugging below."
                }
            } else null

            _uiState.update {
                it.copy(
                    adbStatus = status,
                    isRefreshing = false,
                    toastMessage = feedback
                )
            }
        }
    }

    fun attemptWirelessConnect() {
        val currentState = _uiState.value
        val port = currentState.wirelessPort
        val pairingCode = currentState.wirelessPairingCode.takeIf { it.isNotBlank() }
        val host = currentState.wirelessHost

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, lastConnectResult = null) }
            delay(400) // simulation feedback delay

            val result = adbManager.attemptWirelessConnect(
                host = host,
                portString = port,
                pairingCode = pairingCode
            )

            val status = checkAdbConnectionUseCase()

            val toast = when (result) {
                is WirelessConnectResult.Success -> result.message
                is WirelessConnectResult.Failure -> "${result.error} ${result.suggestion}"
            }

            _uiState.update {
                it.copy(
                    isConnecting = false,
                    lastConnectResult = result,
                    adbStatus = status,
                    toastMessage = toast
                )
            }
        }
    }

    fun copyCommand(command: String) {
        val success = adbManager.copyToClipboard(command)
        if (success) {
            _uiState.update { it.copy(toastMessage = "Command copied to clipboard") }
        }
    }

    fun openDeveloperSettings(context: Context) {
        try {
            val intent = adbManager.createOpenDevSettingsIntent()
            context.startActivity(intent)
        } catch (_: Exception) {
            _uiState.update { it.copy(toastMessage = "Could not open Developer Options automatically. Open Settings > System > Developer Options.") }
        }
    }

    fun openWirelessDebugSettings(context: Context) {
        try {
            val intent = adbManager.createOpenWirelessDebugSettingsIntent()
            context.startActivity(intent)
        } catch (_: Exception) {
            openDeveloperSettings(context)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    companion object {
        fun provideFactory(
            checkAdbConnectionUseCase: CheckAdbConnectionUseCase,
            adbManager: AdbManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdbViewModel(checkAdbConnectionUseCase, adbManager) as T
            }
        }
    }
}

