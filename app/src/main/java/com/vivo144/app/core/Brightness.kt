package com.vivo144.app.core

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.vivo144.app.service.UtilityAccessibilityService
import kotlin.math.roundToInt

/**
 * Brightness bypass: manual mode + the standard keys + the accessibility
 * overlay (window screenBrightness). Only keys confirmed on the device are
 * written here; the vivo HBM keys live in the Lab, never in this flow.
 */
object Brightness {
    private val KEYS = listOf("screen_brightness_mode", "screen_brightness", "screen_brightness_float")

    data class Result(val lines: List<String>) {
        val text get() = lines.joinToString("\n")
    }

    /** Turns the bypass on (or updates it) at [level] 0.01–1.0. Call off the main thread. */
    fun apply(c: Context, level: Float): Result {
        val lv = level.coerceIn(0.01f, 1f)
        Util.level = lv
        Util.bypass = true
        UtilityAccessibilityService.instance?.showOverlay(lv)
        val out = mutableListOf<String>()
        for (k in KEYS) Util.backupIfNeeded(c, Table.SYSTEM, k)
        out += line("screen_brightness_mode", SettingsIO.put(c, Table.SYSTEM, "screen_brightness_mode", "0"))
        out += line("screen_brightness", SettingsIO.put(c, Table.SYSTEM, "screen_brightness", max255(lv).toString()))
        out += line("screen_brightness_float", SettingsIO.put(c, Table.SYSTEM, "screen_brightness_float", lv.toString()))
        if (UtilityAccessibilityService.instance == null) {
            out += "O bypass por overlay requer o serviço de acessibilidade ativo."
        }
        return Result(out)
    }

    /** Only moves the overlay and the brightness keys (slider drag). Call off the main thread. */
    fun setLevel(c: Context, level: Float) {
        val lv = level.coerceIn(0.01f, 1f)
        Util.level = lv
        UtilityAccessibilityService.instance?.showOverlay(lv)
        SettingsIO.put(c, Table.SYSTEM, "screen_brightness", max255(lv).toString())
        SettingsIO.put(c, Table.SYSTEM, "screen_brightness_float", lv.toString())
    }

    /** Bypass off: overlay removed, the three keys back to their originals. */
    fun disable(c: Context): Result {
        Util.bypass = false
        UtilityAccessibilityService.instance?.removeOverlay()
        val out = mutableListOf<String>()
        for (k in KEYS) {
            if (!Util.hasBackup(Table.SYSTEM, k)) continue
            out += line(k, SettingsIO.put(c, Table.SYSTEM, k, Util.backup(Table.SYSTEM, k)))
            Util.dropBackup(Table.SYSTEM, k)
        }
        if (out.isEmpty()) out += "Nada para restaurar."
        return Result(out)
    }

    /** "Restaurar valores originais": every key this app ever backed up, Lab included. */
    fun restoreEverything(c: Context): Result {
        val out = disable(c).lines.toMutableList()
        for ((t, k) in Util.backedUpKeys()) {
            out += line(k, SettingsIO.put(c, t, k, Util.backup(t, k)))
            Util.dropBackup(t, k)
        }
        return Result(out)
    }

    fun max255(level: Float) = maxOf(1, (level * 255f).roundToInt())

    /** Current manual brightness value 0–255, or null. */
    fun current255(c: Context): Int? = SettingsIO.get(c, Table.SYSTEM, "screen_brightness")?.toIntOrNull()

    private fun line(key: String, w: SettingsIO.Write): String {
        val err = w.error?.let { " — $it" } ?: ""
        return (if (w.ok) "✓ " else "✗ ") + "$key = ${w.readBack ?: "(vazio)"} via ${w.via}$err"
    }
}

/** Accessibility service state and automatic enabling. */
object Access {
    fun component(c: Context) = ComponentName(c, UtilityAccessibilityService::class.java).flattenToString()

    fun enabled(c: Context): Boolean {
        val list = try {
            Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } catch (_: Throwable) { null } ?: return UtilityAccessibilityService.instance != null
        return list.split(':').any { it.equals(component(c), true) }
    }

    fun running() = UtilityAccessibilityService.instance != null

    /**
     * Adds our component to enabled_accessibility_services, keeping the
     * existing ones, and sets accessibility_enabled=1. Needs
     * WRITE_SECURE_SETTINGS (or Shizuku). Also gets around "restricted settings".
     */
    fun enable(c: Context): String {
        if (!SettingsIO.hasSecure(c) && !ShizukuState.ready(1000)) {
            return "Precisa de WRITE_SECURE_SETTINGS ou do Shizuku."
        }
        val me = component(c)
        val cur = SettingsIO.get(c, Table.SECURE, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val items = cur.split(':').filter { it.isNotBlank() && it != "null" }.toMutableList()
        if (items.none { it.equals(me, true) }) items += me
        val w1 = SettingsIO.put(c, Table.SECURE, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, items.joinToString(":"))
        val w2 = SettingsIO.put(c, Table.SECURE, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
        return if (w1.ok && w2.ok) "Serviço de acessibilidade ativado." else "Falhou: ${w1.error ?: ""} ${w2.error ?: ""}".trim()
    }
}
