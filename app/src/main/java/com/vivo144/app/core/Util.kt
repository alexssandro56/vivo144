package com.vivo144.app.core

import android.content.Context
import android.content.SharedPreferences

/** Preferences of the brightness / Power-button tools ("iqoo_util_prefs"). */
object Util {
    private lateinit var p: SharedPreferences

    fun init(c: Context) {
        p = c.applicationContext.getSharedPreferences("iqoo_util_prefs", Context.MODE_PRIVATE)
    }

    enum class Close { BACK, HOME, NENHUMA }

    var powerBridge: Boolean
        get() = p.getBoolean("power_bridge_enabled", true)
        set(v) = p.edit().putBoolean("power_bridge_enabled", v).apply()

    var watched: List<String>
        get() = (p.getString("watched_packages", "com.vivo.agent") ?: "")
            .split(',', '\n', ' ').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        set(v) = p.edit().putString("watched_packages", v.distinct().joinToString(",")).apply()

    var closeAction: Close
        get() = runCatching { Close.valueOf(p.getString("close_action", "BACK")!!) }.getOrDefault(Close.BACK)
        set(v) = p.edit().putString("close_action", v.name).apply()

    var bypass: Boolean
        get() = p.getBoolean("brightness_bypass_enabled", false)
        set(v) = p.edit().putBoolean("brightness_bypass_enabled", v).apply()

    /** 0.01 – 1.0; 1.0 if never set. */
    var level: Float
        get() = p.getFloat("brightness_level", 1f).coerceIn(0.01f, 1f)
        set(v) = p.edit().putFloat("brightness_level", v.coerceIn(0.01f, 1f)).apply()

    var revertOnScreenOff: Boolean
        get() = p.getBoolean("revert_on_screen_off", false)
        set(v) = p.edit().putBoolean("revert_on_screen_off", v).apply()

    var hdr: Boolean
        get() = p.getBoolean("exp_hdr_overlay", false)
        set(v) = p.edit().putBoolean("exp_hdr_overlay", v).apply()

    var fullOverlay: Boolean
        get() = p.getBoolean("exp_full_overlay", false)
        set(v) = p.edit().putBoolean("exp_full_overlay", v).apply()

    var labRevertSeconds: Int
        get() = p.getInt("lab_revert_seconds", 30)
        set(v) = p.edit().putInt("lab_revert_seconds", v.coerceAtLeast(0)).apply()

    // ---- backups of original values, taken before the first write of each key ----
    private const val NULL = "\u0000null"
    private fun bk(t: Table, key: String) = "bak_${t.shell}_$key"

    fun hasBackup(t: Table, key: String) = p.contains(bk(t, key))

    fun backupIfNeeded(c: Context, t: Table, key: String) {
        if (hasBackup(t, key)) return
        val v = SettingsIO.get(c, t, key)
        p.edit().putString(bk(t, key), v ?: NULL).commit()
    }

    /** Original value (null = was missing). Only meaningful if [hasBackup]. */
    fun backup(t: Table, key: String): String? = p.getString(bk(t, key), NULL).takeIf { it != NULL }

    fun dropBackup(t: Table, key: String) = p.edit().remove(bk(t, key)).apply()

    fun backedUpKeys(): List<Pair<Table, String>> = p.all.keys.filter { it.startsWith("bak_") }.mapNotNull { k ->
        val rest = k.removePrefix("bak_")
        val t = Table.entries.firstOrNull { rest.startsWith(it.shell + "_") } ?: return@mapNotNull null
        t to rest.removePrefix(t.shell + "_")
    }

    fun listen(l: SharedPreferences.OnSharedPreferenceChangeListener) = p.registerOnSharedPreferenceChangeListener(l)
    fun unlisten(l: SharedPreferences.OnSharedPreferenceChangeListener) = p.unregisterOnSharedPreferenceChangeListener(l)
}
