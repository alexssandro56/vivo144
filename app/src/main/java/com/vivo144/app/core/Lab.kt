package com.vivo144.app.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Key lab: only the keys below may be written. Each test backs up the
 * original first, writes, reads back and schedules an automatic revert.
 */
object Lab {
    data class Key(val table: Table, val key: String, val type: String, val observed: String)

    val KEYS = listOf(
        Key(Table.SYSTEM, "vivo_hbm_level", "int", "0"),
        Key(Table.SYSTEM, "vivo_max_hbm_limit", "int", "-1"),
        Key(Table.SYSTEM, "vivo_elec_sre", "int", "-1"),
        Key(Table.SYSTEM, "lcm_highbrightness_float", "float", "0.9"),
        Key(Table.SYSTEM, "screen_auto_brightness_adj", "float", "-0.35505247"),
        Key(Table.SYSTEM, "vivo_screen_auto_brightness_adj", "float", "0.50707483"),
        Key(Table.SECURE, "vivo_sre_lux", "int", "-1"),
        Key(Table.SECURE, "vivo_adaptive_app_brightness_state", "int", "0"),
    )

    /** Read only, shown for reference. */
    val READ_ONLY = listOf("main_normal_max_brightness_nit", "main_hbm_max_brightness_nit")

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun valid(k: Key, v: String) = when (k.type) {
        "int" -> v.toIntOrNull() != null
        else -> v.toFloatOrNull() != null
    }

    /** Secure keys need WRITE_SECURE_SETTINGS (or Shizuku); otherwise read-only. */
    fun writable(c: Context, k: Key) =
        k.table == Table.SYSTEM || SettingsIO.hasSecure(c) || ShizukuState.status.value == ShizukuState.Status.READY

    fun test(c: Context, k: Key, value: String, done: (String) -> Unit) {
        val ctx = c.applicationContext
        io.execute {
            if (!valid(k, value)) { done("Valor inválido para ${k.type}."); return@execute }
            Util.backupIfNeeded(ctx, k.table, k.key)
            val w = SettingsIO.put(ctx, k.table, k.key, value)
            val secs = Util.labRevertSeconds
            val msg = (if (w.ok) "✓ " else "✗ ") + "${k.key} = ${w.readBack} via ${w.via}" +
                (w.error?.let { " — $it" } ?: "") + if (secs > 0 && w.ok) " · volta sozinho em ${secs}s" else ""
            done(msg)
            if (secs > 0 && w.ok) main.postDelayed({ restore(ctx, k) { done(it) } }, secs * 1000L)
        }
    }

    fun restore(c: Context, k: Key, done: (String) -> Unit) {
        val ctx = c.applicationContext
        io.execute {
            if (!Util.hasBackup(k.table, k.key)) { done("${k.key}: sem backup, nada a restaurar."); return@execute }
            val w = SettingsIO.put(ctx, k.table, k.key, Util.backup(k.table, k.key))
            if (w.ok) Util.dropBackup(k.table, k.key)
            done((if (w.ok) "↺ " else "✗ ") + "${k.key} restaurado = ${w.readBack}" + (w.error?.let { " — $it" } ?: ""))
        }
    }

    fun restoreAll(c: Context, done: (String) -> Unit) {
        KEYS.filter { Util.hasBackup(it.table, it.key) }.forEach { restore(c, it, done) }
    }

    fun read(c: Context, k: Key) = SettingsIO.get(c, k.table, k.key)
}
