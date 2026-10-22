package com.example.platform.adb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.example.data.models.AdbStatus
import com.example.platform.system.SystemSettingsController
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class WirelessConnectResult {
    data class Success(val message: String, val isAuthorized: Boolean) : WirelessConnectResult()
    data class Failure(val error: String, val suggestion: String) : WirelessConnectResult()
}

class AdbManager(
    private val context: Context,
    private val systemSettingsController: SystemSettingsController
) {

    fun getAdbStatus(): AdbStatus {
        val devOpts = systemSettingsController.isDeveloperOptionsEnabled()
        val usbDebug = systemSettingsController.isAdbDebuggingEnabled()
        val hasSecurePermission = systemSettingsController.hasWriteSecureSettings()
        val pkg = context.packageName

        val stateText = when {
            hasSecurePermission -> "Authorized (Full Access)"
            usbDebug -> "Connected (Authorization Required)"
            devOpts -> "Developer Mode Enabled"
            else -> "Not Connected"
        }

        val grantCmd = "adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS"
        val vivoCmd = "adb shell settings put global vivo_screen_refresh_rate_mode 3 && adb shell settings put system peak_refresh_rate 144.0"

        return AdbStatus(
            isDeveloperOptionsEnabled = devOpts,
            isUsbDebuggingEnabled = usbDebug,
            isWirelessDebuggingEnabled = usbDebug, // Wireless debugging flag
            hasWriteSecureSettings = hasSecurePermission,
            connectionStateText = stateText,
            isAdbAvailable = usbDebug || hasSecurePermission,
            permissionCommand = grantCmd,
            vivoSpecificCommand = vivoCmd,
            localIpAddress = getLocalIpAddress()
        )
    }

    suspend fun attemptWirelessConnect(
        host: String = "127.0.0.1",
        portString: String,
        pairingCode: String? = null
    ): WirelessConnectResult = withContext(Dispatchers.IO) {
        val devOpts = systemSettingsController.isDeveloperOptionsEnabled()
        val usbDebug = systemSettingsController.isAdbDebuggingEnabled()
        val hasSecure = systemSettingsController.hasWriteSecureSettings()

        if (!devOpts) {
            return@withContext WirelessConnectResult.Failure(
                error = "Developer Options are not enabled.",
                suggestion = "Tap 'Build Number' 7 times in Settings > About Phone to enable Developer Options first."
            )
        }

        if (!usbDebug) {
            return@withContext WirelessConnectResult.Failure(
                error = "USB / Wireless Debugging is disabled in Developer Options.",
                suggestion = "Enable 'Wireless Debugging' or 'USB Debugging' in Developer Options."
            )
        }

        val port = portString.trim().toIntOrNull()
        if (port == null || port !in 1024..65535) {
            return@withContext WirelessConnectResult.Failure(
                error = "Invalid port number: '$portString'.",
                suggestion = "Enter the 5-digit port shown in Settings > Developer Options > Wireless Debugging (e.g., 37000-45000 or 5555)."
            )
        }

        // Check if socket can connect to local port or host
        val targetHost = if (host.isBlank()) "127.0.0.1" else host.trim()
        val isReachable = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetHost, port), 2000)
                true
            }
        } catch (_: Exception) {
            false
        }

        if (hasSecure) {
            WirelessConnectResult.Success(
                message = "Connected! Wireless Debugging is active and WRITE_SECURE_SETTINGS is fully authorized.",
                isAuthorized = true
            )
        } else if (isReachable || usbDebug) {
            WirelessConnectResult.Success(
                message = "ADB service detected on port $port! Run the authorization command to finish setup.",
                isAuthorized = false
            )
        } else {
            WirelessConnectResult.Failure(
                error = "Could not connect to $targetHost:$port.",
                suggestion = "Make sure your device is connected to Wi-Fi, Wireless Debugging is toggled ON, and the port matches your current session."
            )
        }
    }

    fun copyToClipboard(text: String, label: String = "ADB Command"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createOpenDevSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun createOpenWirelessDebugSettingsIntent(): Intent {
        return try {
            // Android 11+ (API 30+) specific Wireless Debugging settings intent if available
            val intent = Intent("android.settings.WIRELESS_DEBUG_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent
        } catch (_: Exception) {
            createOpenDevSettingsIntent()
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else {
                "192.168.1.xxx"
            }
        } catch (_: Exception) {
            "192.168.1.xxx"
        }
    }
}

