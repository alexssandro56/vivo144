package com.vivo144.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log

enum class Table(val shell: String) { SYSTEM("system"), SECURE("secure"), GLOBAL("global") }

/**
 * Read/write of Settings with per-call try/catch, a log line (tag IqooUtil)
 * and read-back. Apps may only write *public* System keys (WRITE_SETTINGS) and
 * Secure/Global keys with WRITE_SECURE_SETTINGS; vendor keys such as
 * screen_brightness_float or vivo_* are refused to apps, so those fall back to
 * `settings put` through Shizuku when it is available.
 */
object SettingsIO {
    const val TAG = "IqooUtil"

    fun canWriteSystem(c: Context) = Settings.System.canWrite(c)
    fun hasSecure(c: Context) =
        c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    fun uri(t: Table, key: String): Uri = when (t) {
        Table.SYSTEM -> Settings.System.getUriFor(key)
        Table.SECURE -> Settings.Secure.getUriFor(key)
        Table.GLOBAL -> Settings.Global.getUriFor(key)
    }

    /** Current value, or null when missing/unreadable. */
    fun get(c: Context, t: Table, key: String): String? {
        try {
            val cr = c.contentResolver
            return when (t) {
                Table.SYSTEM -> Settings.System.getString(cr, key)
                Table.SECURE -> Settings.Secure.getString(cr, key)
                Table.GLOBAL -> Settings.Global.getString(cr, key)
            }
        } catch (_: Throwable) {
            // Apps targeting Android 12+ may not read non-public keys.
        }
        if (ShizukuState.status.value != ShizukuState.Status.READY) return null
        val r = Shell.exec("settings get ${t.shell} ${Shell.q(key)}")
        return if (r.ok && r.out != "null") r.out else null
    }

    data class Write(val ok: Boolean, val via: String, val readBack: String?, val error: String?)

    /** Writes [value] (null = delete) and reads it back. */
    fun put(c: Context, t: Table, key: String, value: String?): Write {
        var err: String? = null
        val viaApi = try {
            val cr = c.contentResolver
            when (t) {
                Table.SYSTEM -> Settings.System.putString(cr, key, value)
                Table.SECURE -> Settings.Secure.putString(cr, key, value)
                Table.GLOBAL -> Settings.Global.putString(cr, key, value)
            }
        } catch (e: Throwable) {
            err = e.message ?: e.toString(); false
        }
        var via = "app"
        if (!viaApi) {
            if (ShizukuState.ready(1000)) {
                val cmd = if (value == null) "settings delete ${t.shell} ${Shell.q(key)}"
                else "settings put ${t.shell} ${Shell.q(key)} ${Shell.q(value)}"
                val r = Shell.exec(cmd)
                via = "shizuku"
                if (!r.ok) err = r.all.ifEmpty { "exit ${r.code}" } else err = null
            } else {
                via = "nenhum"
            }
        }
        val back = get(c, t, key)
        val same = back == value ||
            (back?.toDoubleOrNull() != null && back?.toDoubleOrNull() == value?.toDoubleOrNull())
        val ok = err == null && same
        Log.i(TAG, "put ${t.shell}/$key=$value via=$via ok=$ok readBack=$back err=$err")
        return Write(ok, via, back, err)
    }
}
