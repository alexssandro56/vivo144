package com.example.data.repositories

import com.example.data.models.CheckStatus
import com.example.data.models.CompatibilityCheckItem
import com.example.data.models.CompatibilityResult
import com.example.platform.adb.AdbManager
import com.example.platform.device.DeviceInfoProvider
import com.example.platform.shizuku.ShizukuDetector
import com.example.platform.system.SystemSettingsController

interface CompatibilityRepository {
    suspend fun evaluateCompatibility(targetFps: Int = 144): CompatibilityResult
}

class CompatibilityRepositoryImpl(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val systemSettingsController: SystemSettingsController,
    private val adbManager: AdbManager,
    private val shizukuDetector: ShizukuDetector
) : CompatibilityRepository {

    override suspend fun evaluateCompatibility(targetFps: Int): CompatibilityResult {
        val hasSecureSettings = systemSettingsController.hasWriteSecureSettings()
        val isAdbDebugging = systemSettingsController.isAdbDebuggingEnabled()
        val specs = deviceInfoProvider.getDeviceSpecs(hasSecureSettings, isAdbDebugging)
        val shizukuInstalled = shizukuDetector.checkShizukuInstalled()
        val shizukuRunning = shizukuDetector.checkShizukuRunning()
        val isRoot = shizukuDetector.checkRoot()

        val checkItems = mutableListOf<CompatibilityCheckItem>()
        val missingRequirements = mutableListOf<String>()

        // 1. Device Manufacturer Check
        if (specs.isVivoOrIqoo) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Device Manufacturer",
                    description = "${specs.manufacturer} ${specs.model} (${specs.customOsBrand})",
                    status = CheckStatus.PASSED,
                    details = "Native Vivo/iQOO OriginOS & FuntouchOS display compositor commands supported."
                )
            )
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Device Manufacturer",
                    description = "${specs.manufacturer} ${specs.model}",
                    status = CheckStatus.WARNING,
                    details = "Non-Vivo device. Standard Android DisplayMode & Peak Refresh Rate keys will be used as fallback."
                )
            )
        }

        // 2. Display Refresh Rate Check
        val maxRate = specs.supportedRefreshRates.maxOrNull() ?: specs.currentRefreshRate
        val is144HzAvailable = specs.is144HzSupported
        if (is144HzAvailable) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Display Hardware (144 Hz)",
                    description = "144 Hz Available (Current: ${specs.currentRefreshRate.toInt()} Hz)",
                    status = CheckStatus.PASSED,
                    details = "Hardware panel reports high-refresh rate capability matching $targetFps FPS."
                )
            )
        } else if (maxRate >= 119.0f) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Display Hardware (144 Hz Locked)",
                    description = "${maxRate.toInt()} Hz Peak Display (Current: ${specs.currentRefreshRate.toInt()} Hz)",
                    status = CheckStatus.WARNING,
                    details = "144 Hz is locked: Hardware display panel maxes out at ${maxRate.toInt()} Hz. Frame rates up to ${maxRate.toInt()} FPS and all other utility features remain active."
                )
            )
            missingRequirements.add("144 Hz locked: Panel hardware max is ${maxRate.toInt()} Hz")
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Display Hardware (144 Hz Locked)",
                    description = "${maxRate.toInt()} Hz Standard Display",
                    status = CheckStatus.FAILED,
                    details = "144 Hz is locked: Display panel is limited to ${maxRate.toInt()} Hz hardware refresh rate. Standard refresh rates & all monitoring tools remain active."
                )
            )
            missingRequirements.add("144 Hz locked: Display panel is limited to ${maxRate.toInt()} Hz")
        }

        // 3. Android OS Version
        val isAndroidCompatible = specs.sdkInt >= 26
        if (isAndroidCompatible) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Android Compatibility",
                    description = specs.androidVersion,
                    status = CheckStatus.PASSED,
                    details = "Android framework display rate and power manager APIs are fully compatible."
                )
            )
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Android Compatibility",
                    description = specs.androidVersion,
                    status = CheckStatus.FAILED,
                    details = "Requires Android 8.0 (API 26) or higher."
                )
            )
            missingRequirements.add("Android version is below required API 26")
        }

        // 4. System Access / ADB
        if (hasSecureSettings) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "System Access",
                    description = "WRITE_SECURE_SETTINGS Granted",
                    status = CheckStatus.PASSED,
                    details = "Direct system-level display refresh rate configuration is authorized."
                )
            )
        } else if (isAdbDebugging) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "System Access",
                    description = "ADB Connected (Authorization Required)",
                    status = CheckStatus.WARNING,
                    details = "ADB debugging is active. Grant WRITE_SECURE_SETTINGS to apply display overrides without root."
                )
            )
            missingRequirements.add("WRITE_SECURE_SETTINGS permission required via ADB")
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "System Access",
                    description = "Standard Sandbox Mode",
                    status = CheckStatus.WARNING,
                    details = "ADB authorization or Shizuku is required to modify system refresh rates dynamically."
                )
            )
            missingRequirements.add("Connect via ADB or Shizuku to grant system access")
        }

        // 5. Target FPS Configuration
        val isFpsTargetSupported = when {
            targetFps <= 60 -> true
            targetFps <= 120 -> maxRate >= 119.0f || specs.isVivoOrIqoo
            targetFps >= 144 -> is144HzAvailable
            else -> true
        }

        if (isFpsTargetSupported) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "$targetFps FPS Target",
                    description = "Configuration Available",
                    status = CheckStatus.PASSED,
                    details = "Compositor pipeline and refresh timings match requested $targetFps FPS."
                )
            )
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "$targetFps FPS Target",
                    description = "Hardware Limited",
                    status = CheckStatus.FAILED,
                    details = "Hardware panel reports maximum refresh rate of ${maxRate.toInt()} Hz."
                )
            )
            missingRequirements.add("Device cannot reach requested $targetFps FPS target")
        }

        // 6. Shizuku & Root status (Optional)
        if (shizukuRunning) {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Shizuku Service",
                    description = "Available (Active)",
                    status = CheckStatus.PASSED,
                    details = "Privileged API access is available.",
                    isOptional = true
                )
            )
        } else {
            checkItems.add(
                CompatibilityCheckItem(
                    title = "Shizuku Service",
                    description = if (shizukuInstalled) "Installed (Stopped)" else "Not Installed",
                    status = CheckStatus.UNAVAILABLE,
                    details = "Shizuku is optional. Standard ADB commands can be used instead.",
                    isOptional = true
                )
            )
        }

        val requiredChecks = checkItems.filter { !it.isOptional }
        val passedCount = requiredChecks.count { it.status == CheckStatus.PASSED }
        val totalCount = requiredChecks.size

        val isReady = passedCount >= 4 && is144HzAvailable && isAndroidCompatible
        val summary = if (isReady) {
            "READY FOR 144 FPS"
        } else {
            "NOT READY"
        }

        return CompatibilityResult(
            manufacturer = specs.manufacturer,
            model = specs.model,
            brand = specs.brand,
            deviceName = specs.deviceName,
            androidVersion = specs.androidVersion,
            maxRefreshRate = maxRate,
            currentRefreshRate = specs.currentRefreshRate,
            isVivoOrIqoo = specs.isVivoOrIqoo,
            is144HzDisplayAvailable = is144HzAvailable,
            isAndroidCompatible = isAndroidCompatible,
            isAdbConnected = isAdbDebugging,
            isShizukuAvailable = shizukuRunning,
            hasWriteSecureSettings = hasSecureSettings,
            isRootDetected = isRoot,
            items = checkItems,
            passedChecksCount = passedCount,
            totalRequiredChecks = totalCount,
            isReadyFor144Fps = isReady,
            readinessSummary = summary,
            missingRequirements = missingRequirements
        )
    }
}
