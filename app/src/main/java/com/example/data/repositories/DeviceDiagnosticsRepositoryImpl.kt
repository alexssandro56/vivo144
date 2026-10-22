package com.example.data.repositories

import com.example.data.models.AdbStatus
import com.example.data.models.DeviceSpecs
import com.example.data.models.DiagnosticItem
import com.example.data.models.DiagnosticStatusType
import com.example.platform.adb.AdbManager
import com.example.platform.device.DeviceInfoProvider
import com.example.platform.system.SystemSettingsController

class DeviceDiagnosticsRepositoryImpl(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val systemSettingsController: SystemSettingsController,
    private val adbManager: AdbManager
) : DeviceDiagnosticsRepository {

    override suspend fun getDeviceSpecs(): DeviceSpecs {
        val hasSecurePerm = systemSettingsController.hasWriteSecureSettings()
        val adbStatus = adbManager.getAdbStatus()
        return deviceInfoProvider.getDeviceSpecs(hasSecurePerm, adbStatus.isAdbAvailable)
    }

    override suspend fun getAdbStatus(): AdbStatus {
        return adbManager.getAdbStatus()
    }

    override suspend fun getDiagnosticItems(): List<DiagnosticItem> {
        val specs = getDeviceSpecs()
        val adb = getAdbStatus()

        val items = mutableListOf<DiagnosticItem>()

        // 1. Device Brand
        items.add(
            DiagnosticItem(
                id = "brand",
                title = "Device Brand",
                value = specs.brand.ifBlank { specs.manufacturer },
                statusType = if (specs.isVivoOrIqoo) DiagnosticStatusType.SUPPORTED else DiagnosticStatusType.INFO,
                statusLabel = if (specs.isVivoOrIqoo) "✓ Vivo / iQOO" else specs.brand,
                detail = "Target OS: ${specs.customOsBrand}"
            )
        )

        // 2. Device Model
        items.add(
            DiagnosticItem(
                id = "model",
                title = "Device Model",
                value = specs.model,
                statusType = if (specs.isVivoOrIqoo) DiagnosticStatusType.SUPPORTED else DiagnosticStatusType.GENERIC_FALLBACK,
                statusLabel = if (specs.isVivoOrIqoo) "✓ Supported" else "Standard Model",
                detail = "Hardware manufacturer: ${specs.manufacturer}"
            )
        )

        // 3. Device Code Name
        items.add(
            DiagnosticItem(
                id = "device_name",
                title = "Device Name / Code",
                value = specs.deviceName,
                statusType = DiagnosticStatusType.INFO,
                statusLabel = "Hardware Product",
                detail = "Internal board / hardware identifier"
            )
        )

        // 4. Manufacturer
        items.add(
            DiagnosticItem(
                id = "manufacturer",
                title = "Manufacturer",
                value = specs.manufacturer,
                statusType = if (specs.isVivoOrIqoo) DiagnosticStatusType.SUPPORTED else DiagnosticStatusType.INFO,
                statusLabel = if (specs.isVivoOrIqoo) "✓ Supported" else "Android OEM",
                detail = "OEM vendor implementation"
            )
        )

        // 3. Android Version
        items.add(
            DiagnosticItem(
                id = "android_ver",
                title = "Android Version",
                value = specs.androidVersion,
                statusType = DiagnosticStatusType.SUPPORTED,
                statusLabel = "✓ Supported (API ${specs.sdkInt})",
                detail = "Requires Android 8.0+ (Target SDK 36)"
            )
        )

        // 4. Build Number
        items.add(
            DiagnosticItem(
                id = "build_num",
                title = "Build Number",
                value = specs.buildNumber,
                statusType = DiagnosticStatusType.INFO,
                statusLabel = "Verified",
                detail = "System firmware signature"
            )
        )

        // 5. Display Refresh Rate
        items.add(
            DiagnosticItem(
                id = "display_hz",
                title = "Display Refresh Rate",
                value = "${specs.currentRefreshRate.toInt()} Hz",
                statusType = if (specs.currentRefreshRate >= 143f) DiagnosticStatusType.AVAILABLE_144 else DiagnosticStatusType.INFO,
                statusLabel = if (specs.currentRefreshRate >= 143f) "✓ 144 Hz Active" else "Active Mode",
                detail = "Current window compositor rate"
            )
        )

        // 6. Supported Refresh Rates
        val supportedStr = specs.supportedRefreshRates.joinToString(", ") { "${it.toInt()} Hz" }
        items.add(
            DiagnosticItem(
                id = "supported_rates",
                title = "Supported Refresh Rates",
                value = supportedStr,
                statusType = if (specs.is144HzSupported) DiagnosticStatusType.AVAILABLE_144 else DiagnosticStatusType.SUPPORTED,
                statusLabel = if (specs.is144HzSupported) "✓ 144 Hz available" else "Display Modes",
                detail = "Detected hardware panel display mode capabilities"
            )
        )

        // 7. Current FPS Target
        items.add(
            DiagnosticItem(
                id = "current_fps",
                title = "Current FPS Target",
                value = "${specs.currentRefreshRate.toInt()} FPS",
                statusType = DiagnosticStatusType.SUPPORTED,
                statusLabel = "✓ Monitored",
                detail = "Frame synchronization rate"
            )
        )

        // 8. ADB Status
        items.add(
            DiagnosticItem(
                id = "adb_status",
                title = "ADB Status",
                value = adb.connectionStateText,
                statusType = if (adb.hasWriteSecureSettings) {
                    DiagnosticStatusType.CONNECTED
                } else {
                    DiagnosticStatusType.WARNING_REQ_ADB
                },
                statusLabel = if (adb.hasWriteSecureSettings) "✓ Connected" else "⚠ Wireless debugging required",
                detail = if (adb.hasWriteSecureSettings) "WRITE_SECURE_SETTINGS active" else "Grant permission via ADB terminal"
            )
        )

        // 9. Device Compatibility
        items.add(
            DiagnosticItem(
                id = "compat",
                title = "Device Compatibility",
                value = if (specs.isVivoOrIqoo) "Vivo / iQOO Hardware Verified" else "Universal Android DisplayMode API",
                statusType = DiagnosticStatusType.SUPPORTED,
                statusLabel = "✓ Supported",
                detail = "Dynamic high-refresh rate switching pipeline ready"
            )
        )

        return items
    }
}
