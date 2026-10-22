package com.example.platform.shizuku

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

data class SystemAccessInfo(
    val isAdbEnabled: Boolean,
    val isWirelessDebuggingEnabled: Boolean,
    val hasWriteSecureSettings: Boolean,
    val isShizukuInstalled: Boolean,
    val isShizukuRunning: Boolean,
    val isRootDetected: Boolean,
    val adbStatusText: String,
    val wirelessDebugStatusText: String,
    val shizukuStatusText: String,
    val rootStatusText: String
)

class ShizukuDetector(private val context: Context) {

    fun checkShizukuInstalled(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0) != null
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun checkShizukuRunning(): Boolean {
        if (!checkShizukuInstalled()) return false
        return try {
            // Check if Shizuku provider is responsive
            val cursor = context.contentResolver.query(
                android.net.Uri.parse("content://moe.shizuku.privileged.api.provider"),
                null, null, null, null
            )
            val isRunning = cursor != null
            cursor?.close()
            isRunning
        } catch (_: Exception) {
            false
        }
    }

    fun checkRoot(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun getSystemAccessInfo(
        isAdbConnected: Boolean,
        isWirelessDebugging: Boolean,
        hasWriteSecureSettings: Boolean
    ): SystemAccessInfo {
        val shizukuInstalled = checkShizukuInstalled()
        val shizukuRunning = checkShizukuRunning()
        val isRoot = checkRoot()

        val adbText = when {
            hasWriteSecureSettings -> "Authorized (Full Access)"
            isAdbConnected -> "Connected (Authorization Required)"
            else -> "Not Connected"
        }

        val wirelessText = if (isWirelessDebugging) "Enabled" else "Disabled / Disconnected"

        val shizukuText = when {
            shizukuRunning -> "Available (Active Service)"
            shizukuInstalled -> "Installed (Service Not Started)"
            else -> "Not installed (Optional)"
        }

        val rootText = if (isRoot) "Root Access Detected" else "Not detected"

        return SystemAccessInfo(
            isAdbEnabled = isAdbConnected,
            isWirelessDebuggingEnabled = isWirelessDebugging,
            hasWriteSecureSettings = hasWriteSecureSettings,
            isShizukuInstalled = shizukuInstalled,
            isShizukuRunning = shizukuRunning,
            isRootDetected = isRoot,
            adbStatusText = adbText,
            wirelessDebugStatusText = wirelessText,
            shizukuStatusText = shizukuText,
            rootStatusText = rootText
        )
    }
}
