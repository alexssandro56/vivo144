package com.vivo144.app.core

import android.content.Context
import android.content.SharedPreferences

enum class Mode { OFF, ALWAYS, AUTO }

/** App settings. Call [init] once from Application.onCreate. */
object Prefs {
    private lateinit var p: SharedPreferences

    fun init(c: Context) {
        p = c.applicationContext.getSharedPreferences("vivo144", Context.MODE_PRIVATE)
    }

    var mode: Mode
        get() = runCatching { Mode.valueOf(p.getString("mode", Mode.OFF.name)!!) }.getOrDefault(Mode.OFF)
        set(v) = p.edit().putString("mode", v.name).apply()

    var fps: Int
        get() = p.getInt("fps", 144)
        set(v) = p.edit().putInt("fps", v).apply()

    var games: Set<String>
        get() = p.getStringSet("games", emptySet())!!.toSet()
        set(v) = p.edit().putStringSet("games", v.toSet()).apply()

    /** Per-game rate from the game profiles ("pkg=fps"); falls back to [fps]. */
    var gameFps: Map<String, Int>
        get() = p.getStringSet("game_fps", emptySet())!!.mapNotNull { e ->
            val i = e.lastIndexOf('=')
            if (i <= 0) null else e.substring(0, i) to (e.substring(i + 1).toIntOrNull() ?: return@mapNotNull null)
        }.toMap()
        set(v) = p.edit().putStringSet("game_fps", v.map { "${it.key}=${it.value}" }.toSet()).apply()

    fun fpsFor(pkg: String?): Int = pkg?.let { gameFps[it] } ?: fps

    /** True once installed games were added to the game-profile database. */
    var profilesSeeded: Boolean
        get() = p.getBoolean("profiles_seeded", false)
        set(v) = p.edit().putBoolean("profiles_seeded", v).apply()

    /** True once the games list was pre-filled from detected games. */
    var gamesSeeded: Boolean
        get() = p.getBoolean("games_seeded", false)
        set(v) = p.edit().putBoolean("games_seeded", v).apply()

    /** Automatic mode, outside games: 0 = release (vivo LTPO decides), else pin this rate. */
    var idleFps: Int
        get() = p.getInt("idle_fps", 0)
        set(v) = p.edit().putInt("idle_fps", v).apply()

    var onBoot: Boolean
        get() = p.getBoolean("on_boot", true)
        set(v) = p.edit().putBoolean("on_boot", v).apply()

    /** Experimental: vote with a much higher priority, to test if it beats an app's own request. */
    var highPriority: Boolean
        get() = p.getBoolean("high_priority", false)
        set(v) = p.edit().putBoolean("high_priority", v).apply()

    var hud: Boolean
        get() = p.getBoolean("hud", false)
        set(v) = p.edit().putBoolean("hud", v).apply()

    fun listen(l: SharedPreferences.OnSharedPreferenceChangeListener) = p.registerOnSharedPreferenceChangeListener(l)
    fun unlisten(l: SharedPreferences.OnSharedPreferenceChangeListener) = p.unregisterOnSharedPreferenceChangeListener(l)
}
