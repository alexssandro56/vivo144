package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repositories.BatteryRepository
import com.example.data.repositories.BatteryRepositoryImpl
import com.example.data.repositories.CompatibilityRepository
import com.example.data.repositories.CompatibilityRepositoryImpl
import com.example.data.repositories.DeviceDiagnosticsRepository
import com.example.data.repositories.DeviceDiagnosticsRepositoryImpl
import com.example.data.repositories.FpsUnlockRepository
import com.example.data.repositories.FpsUnlockRepositoryImpl
import com.example.data.repositories.GameProfileRepository
import com.example.data.repositories.GameProfileRepositoryImpl
import com.example.data.repositories.GroundedCompatibilityRepository
import com.example.data.repositories.PerformanceProfileRepository
import com.example.data.repositories.PerformanceProfileRepositoryImpl
import com.example.data.repositories.SystemPerformanceRepository
import com.example.data.repositories.SystemPerformanceRepositoryImpl
import com.example.data.repositories.ThermalRepository
import com.example.data.repositories.ThermalRepositoryImpl
import com.example.data.repositories.UserPreferencesRepository
import com.example.domain.usecases.CheckAdbConnectionUseCase
import com.example.domain.usecases.GetDeviceDiagnosticsUseCase
import com.example.domain.usecases.RestoreDefaultsUseCase
import com.example.domain.usecases.UnlockFpsUseCase
import com.example.platform.adb.AdbManager
import com.example.platform.battery.BatteryMonitor
import com.example.platform.device.DeviceInfoProvider
import com.example.platform.fps.RealtimeFpsSampler
import com.example.platform.shizuku.ShizukuDetector
import com.example.platform.system.SystemPerformanceSampler
import com.example.platform.system.SystemSettingsController
import com.example.platform.thermal.ThermalMonitor

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val systemSettingsController: SystemSettingsController by lazy {
        SystemSettingsController(context)
    }

    val deviceInfoProvider: DeviceInfoProvider by lazy {
        DeviceInfoProvider(context)
    }

    val adbManager: AdbManager by lazy {
        AdbManager(context, systemSettingsController)
    }

    val shizukuDetector: ShizukuDetector by lazy {
        ShizukuDetector(context)
    }

    val batteryMonitor: BatteryMonitor by lazy {
        BatteryMonitor(context)
    }

    val thermalMonitor: ThermalMonitor by lazy {
        ThermalMonitor(context, batteryMonitor)
    }

    val realtimeFpsSampler: RealtimeFpsSampler by lazy {
        RealtimeFpsSampler(context)
    }

    val systemPerformanceSampler: SystemPerformanceSampler by lazy {
        SystemPerformanceSampler(context)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val batteryRepository: BatteryRepository by lazy {
        BatteryRepositoryImpl(batteryMonitor)
    }

    val thermalRepository: ThermalRepository by lazy {
        ThermalRepositoryImpl(thermalMonitor)
    }

    val performanceProfileRepository: PerformanceProfileRepository by lazy {
        PerformanceProfileRepositoryImpl(userPreferencesRepository)
    }

    val gameProfileRepository: GameProfileRepository by lazy {
        GameProfileRepositoryImpl(database.gameProfileDao())
    }

    val compatibilityRepository: CompatibilityRepository by lazy {
        CompatibilityRepositoryImpl(
            deviceInfoProvider = deviceInfoProvider,
            systemSettingsController = systemSettingsController,
            adbManager = adbManager,
            shizukuDetector = shizukuDetector
        )
    }

    val systemPerformanceRepository: SystemPerformanceRepository by lazy {
        SystemPerformanceRepositoryImpl(
            realtimeFpsSampler = realtimeFpsSampler,
            systemPerformanceSampler = systemPerformanceSampler
        )
    }

    val fpsUnlockRepository: FpsUnlockRepository by lazy {
        FpsUnlockRepositoryImpl(
            deviceInfoProvider = deviceInfoProvider,
            systemSettingsController = systemSettingsController,
            adbManager = adbManager,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    val deviceDiagnosticsRepository: DeviceDiagnosticsRepository by lazy {
        DeviceDiagnosticsRepositoryImpl(
            deviceInfoProvider = deviceInfoProvider,
            systemSettingsController = systemSettingsController,
            adbManager = adbManager
        )
    }

    val groundedCompatibilityRepository: GroundedCompatibilityRepository by lazy {
        GroundedCompatibilityRepository()
    }

    val unlockFpsUseCase: UnlockFpsUseCase by lazy {
        UnlockFpsUseCase(fpsUnlockRepository)
    }

    val restoreDefaultsUseCase: RestoreDefaultsUseCase by lazy {
        RestoreDefaultsUseCase(fpsUnlockRepository)
    }

    val getDeviceDiagnosticsUseCase: GetDeviceDiagnosticsUseCase by lazy {
        GetDeviceDiagnosticsUseCase(deviceDiagnosticsRepository)
    }

    val checkAdbConnectionUseCase: CheckAdbConnectionUseCase by lazy {
        CheckAdbConnectionUseCase(adbManager)
    }
}
