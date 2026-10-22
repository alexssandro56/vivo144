package com.example.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R
import com.example.VivoFpsApplication
import com.example.data.AppContainer
import com.example.data.models.FpsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings Tile Service that allows users to toggle
 * "High Performance Mode" (144 FPS / 144 Hz) directly from the Android System Shade.
 */
class HighPerformanceTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    private val appContainer: AppContainer
        get() = (application as? VivoFpsApplication)?.appContainer
            ?: AppContainer(applicationContext)

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            appContainer.userPreferencesRepository.userPreferencesFlow.collectLatest { config ->
                updateTileState(config)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        observeJob?.cancel()
        observeJob = null
    }

    override fun onClick() {
        super.onClick()
        val currentConfig = appContainer.userPreferencesRepository.userPreferencesFlow.value
        val isCurrentlyHighPerf = currentConfig.isUnlocked && currentConfig.targetFps >= 120

        serviceScope.launch {
            if (isCurrentlyHighPerf) {
                // Disable High Performance Mode -> revert to Standard 60 FPS
                withContext(Dispatchers.IO) {
                    appContainer.fpsUnlockRepository.restoreDefaults()
                    appContainer.userPreferencesRepository.saveConfig(
                        currentConfig.copy(
                            targetFps = 60,
                            refreshRateHz = 60,
                            currentFps = 60,
                            isUnlocked = false
                        )
                    )
                }
                Toast.makeText(
                    applicationContext,
                    getString(R.string.tile_toast_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Enable High Performance Mode -> apply 144 FPS / 144 Hz
                withContext(Dispatchers.IO) {
                    appContainer.fpsUnlockRepository.applyFpsConfiguration(
                        targetFps = 144,
                        refreshRateHz = 144
                    )
                    appContainer.userPreferencesRepository.saveConfig(
                        currentConfig.copy(
                            targetFps = 144,
                            refreshRateHz = 144,
                            currentFps = 144,
                            isUnlocked = true,
                            isMultiTurboGameModeEnabled = true,
                            isPeakRateForced = true
                        )
                    )
                }
                Toast.makeText(
                    applicationContext,
                    getString(R.string.tile_toast_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Immediately refresh the tile
            val newConfig = appContainer.userPreferencesRepository.userPreferencesFlow.value
            updateTileState(newConfig)
        }
    }

    private fun updateTileState(config: FpsConfig) {
        val tile = qsTile ?: return
        val isHighPerf = config.isUnlocked && config.targetFps >= 120

        tile.state = if (isHighPerf) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_high_performance_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_high_performance)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isHighPerf) {
                "${config.targetFps} FPS • ${config.refreshRateHz}Hz"
            } else {
                getString(R.string.tile_high_performance_sub_inactive)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = if (isHighPerf) {
                "Active - ${config.targetFps} FPS"
            } else {
                "Inactive - 60 FPS"
            }
        }

        tile.contentDescription = getString(R.string.tile_high_performance_desc)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
