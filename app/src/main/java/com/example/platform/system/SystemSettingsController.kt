package com.example.platform.system

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

sealed class SettingsApplyResult {
    object Success : SettingsApplyResult()
    data class PermissionDenied(val missingPermission: String, val adbGrantCommand: String) : SettingsApplyResult()
    data class Failed(val error: String) : SettingsApplyResult()
}

class SystemSettingsController(private val context: Context) {

    fun hasWriteSecureSettings(): Boolean {
        return context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    }

    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun isAdbDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun getCurrentPeakRefreshRate(): Float? {
        return try {
            Settings.System.getFloat(context.contentResolver, "peak_refresh_rate")
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentMinRefreshRate(): Float? {
        return try {
            Settings.System.getFloat(context.contentResolver, "min_refresh_rate")
        } catch (_: Exception) {
            null
        }
    }

    fun applyRefreshRate(targetHz: Float, isVivoDevice: Boolean): SettingsApplyResult {
        // Real path on OriginOS: vote in vivo's rms service (no permission needed).
        // The Settings keys below are kept as a best-effort extra only.
        val fps = targetHz.toInt()
        if (com.vivo144.app.core.Rms.vote(fps)) {
            com.vivo144.app.core.Prefs.fps = fps
            if (com.vivo144.app.core.Prefs.mode == com.vivo144.app.core.Mode.OFF) {
                com.vivo144.app.core.Prefs.mode = com.vivo144.app.core.Mode.ALWAYS
            }
            if (hasWriteSecureSettings()) try { applyLegacyKeys(targetHz, isVivoDevice) } catch (_: Exception) { }
            return SettingsApplyResult.Success
        }
        if (!hasWriteSecureSettings()) {
            val pkgName = context.packageName
            return SettingsApplyResult.PermissionDenied(
                missingPermission = "android.permission.WRITE_SECURE_SETTINGS",
                adbGrantCommand = "adb shell pm grant $pkgName android.permission.WRITE_SECURE_SETTINGS"
            )
        }

        return try {
            applyLegacyKeys(targetHz, isVivoDevice)
            SettingsApplyResult.Success
        } catch (se: SecurityException) {
            val pkgName = context.packageName
            SettingsApplyResult.PermissionDenied(
                missingPermission = "android.permission.WRITE_SECURE_SETTINGS",
                adbGrantCommand = "adb shell pm grant $pkgName android.permission.WRITE_SECURE_SETTINGS"
            )
        } catch (e: Exception) {
            SettingsApplyResult.Failed(e.localizedMessage ?: "Unknown system configuration error")
        }
    }

    /** The original Settings-key method (ignored by OriginOS in games). */
    private fun applyLegacyKeys(targetHz: Float, isVivoDevice: Boolean) {
        run {
            val cr = context.contentResolver
            // Standard Android Peak Refresh Rate Keys
            Settings.System.putFloat(cr, "peak_refresh_rate", targetHz)
            Settings.System.putFloat(cr, "min_refresh_rate", targetHz)
            Settings.System.putFloat(cr, "user_refresh_rate", targetHz)

            // Vivo / iQOO OriginOS / FuntouchOS Screen Refresh Mode Flags
            // mode 3: 144Hz / Custom High Mode; mode 2: 120Hz; mode 1: 90Hz; mode 0: 60Hz
            if (isVivoDevice) {
                val vivoMode = when {
                    targetHz >= 143.0f -> 3
                    targetHz >= 119.0f -> 2
                    targetHz >= 89.0f -> 1
                    else -> 0
                }
                try {
                    Settings.Global.putInt(cr, "vivo_screen_refresh_rate_mode", vivoMode)
                    Settings.Global.putInt(cr, "iqoo_game_high_fps_boost", 1)
                } catch (_: Exception) {
                    // Vivo global key write fallback
                }
            }
        }
    }

    fun restoreDefaults(): SettingsApplyResult {
        // Release the rms vote: the panel goes back to vivo's automatic LTPO.
        val released = com.vivo144.app.core.Rms.release()
        if (released) com.vivo144.app.core.Prefs.mode = com.vivo144.app.core.Mode.OFF
        if (released && !hasWriteSecureSettings()) return SettingsApplyResult.Success
        if (!hasWriteSecureSettings()) {
            val pkgName = context.packageName
            return SettingsApplyResult.PermissionDenied(
                missingPermission = "android.permission.WRITE_SECURE_SETTINGS",
                adbGrantCommand = "adb shell pm grant $pkgName android.permission.WRITE_SECURE_SETTINGS"
            )
        }

        return try {
            val cr = context.contentResolver
            // Reset to dynamic standard values (0 or dynamic range)
            Settings.System.putFloat(cr, "peak_refresh_rate", 144.0f)
            Settings.System.putFloat(cr, "min_refresh_rate", 60.0f)
            try {
                Settings.Global.putInt(cr, "vivo_screen_refresh_rate_mode", 0) // Auto / Smart Switch
            } catch (_: Exception) { }
            SettingsApplyResult.Success
        } catch (se: SecurityException) {
            val pkgName = context.packageName
            SettingsApplyResult.PermissionDenied(
                missingPermission = "android.permission.WRITE_SECURE_SETTINGS",
                adbGrantCommand = "adb shell pm grant $pkgName android.permission.WRITE_SECURE_SETTINGS"
            )
        } catch (e: Exception) {
            SettingsApplyResult.Failed(e.localizedMessage ?: "Failed to restore system settings")
        }
    }
}
