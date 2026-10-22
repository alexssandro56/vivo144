package com.example.platform.device

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.example.data.models.DeviceSpecs

class DeviceInfoProvider(private val context: Context) {

    fun getDeviceSpecs(hasWritePermission: Boolean, isAdbConnected: Boolean): DeviceSpecs {
        val rawManufacturer = Build.MANUFACTURER ?: "Android"
        val manufacturer = rawManufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL ?: "Device"
        val rawBrand = Build.BRAND.takeIf { !it.isNullOrBlank() } ?: rawManufacturer
        val brand = rawBrand.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val deviceName = Build.DEVICE.takeIf { !it.isNullOrBlank() } ?: Build.PRODUCT ?: model
        val androidRelease = Build.VERSION.RELEASE ?: "Unknown"
        val sdkInt = Build.VERSION.SDK_INT
        val buildNumber = Build.DISPLAY.takeIf { !it.isNullOrBlank() } ?: Build.ID ?: "Unknown"

        val isVivoOrIqoo = rawManufacturer.contains("vivo", ignoreCase = true) ||
                rawBrand.contains("vivo", ignoreCase = true) ||
                rawBrand.contains("iqoo", ignoreCase = true) ||
                model.contains("iqoo", ignoreCase = true) ||
                rawManufacturer.contains("bbk", ignoreCase = true)

        val (currentRefreshRate, supportedRates) = getDisplayRefreshRates()

        val is144Supported = supportedRates.any { it >= 143.0f }

        val customOsBrand = when {
            isVivoOrIqoo -> "OriginOS / FuntouchOS"
            rawManufacturer.contains("xiaomi", ignoreCase = true) -> "HyperOS / MIUI"
            rawManufacturer.contains("samsung", ignoreCase = true) -> "One UI"
            rawManufacturer.contains("oneplus", ignoreCase = true) -> "OxygenOS / ColorOS"
            else -> "Standard Android System"
        }

        return DeviceSpecs(
            model = model,
            manufacturer = manufacturer,
            brand = brand,
            deviceName = deviceName,
            androidVersion = "Android $androidRelease (API $sdkInt)",
            sdkInt = sdkInt,
            buildNumber = buildNumber,
            currentRefreshRate = currentRefreshRate,
            supportedRefreshRates = supportedRates,
            isVivoOrIqoo = isVivoOrIqoo,
            is144HzSupported = is144Supported,
            hasWriteSecureSettingsPermission = hasWritePermission,
            isAdbConnected = isAdbConnected,
            customOsBrand = customOsBrand
        )
    }

    private fun getDisplayRefreshRates(): Pair<Float, List<Float>> {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay

            val currentRate = display?.refreshRate ?: 60.0f
            val modes = display?.supportedModes ?: emptyArray()

            val ratesList = modes.map { it.refreshRate }
                .distinct()
                .sorted()

            val finalRates = if (ratesList.isNotEmpty()) {
                ratesList
            } else {
                listOf(60.0f, 90.0f, 120.0f, 144.0f)
            }

            Pair(currentRate, finalRates)
        } catch (_: Exception) {
            Pair(60.0f, listOf(60.0f, 90.0f, 120.0f, 144.0f))
        }
    }
}
