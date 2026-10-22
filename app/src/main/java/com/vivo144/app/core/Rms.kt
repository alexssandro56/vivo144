package com.vivo144.app.core

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.view.Display
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * vivo's "rms" binder (com.vivo.rms.IRM), spoken like the framework's own
 * RefreshRateRequester: raw Parcels, no interface token. Same layout as
 * AndroPlus-org/vivo-HighRefresh-NoRoot (MIT).
 *
 * The server checks no uid and treats caller 0 as system_server, so an
 * ordinary app can vote directly. Shizuku is only a fallback for builds where
 * SELinux hides the service from apps.
 */
object Rms {
    private const val TX_RELEASE = 24
    private const val TX_GET = 26
    private const val TX_ACQUIRE_V2 = 31
    private const val HANDLE = 900001L
    private const val FLAGS = 15     // own config bits | private fps | vsync dfps | ignore brightness
    private const val PRIORITY = 100

    enum class Path { DIRECT, SHIZUKU, NONE }

    @Volatile var path = Path.NONE
        private set
    @Volatile private var cached: IBinder? = null

    @Synchronized
    fun binder(): IBinder? {
        cached?.let { if (it.isBinderAlive) return it }
        cached = null
        try {
            val b = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "rms") as IBinder?
            if (b != null && read(b) != null) {
                path = Path.DIRECT
                cached = b
                return b
            }
        } catch (_: Throwable) { }
        if (ShizukuState.ready(1500)) {
            try {
                val raw = SystemServiceHelper.getSystemService("rms")
                if (raw != null) {
                    val w = ShizukuBinderWrapper(raw)
                    path = Path.SHIZUKU
                    cached = w
                    return w
                }
            } catch (_: Throwable) { }
        }
        path = Path.NONE
        return null
    }

    /** Votes [fps] in the FixRate scene. Voting again with the same handle replaces it. */
    fun vote(fps: Int): Boolean {
        val b = binder() ?: return false
        val d = Parcel.obtain()
        return try {
            d.writeLong(HANDLE)
            d.writeString("FixRate")
            d.writeString("Vivo144")
            d.writeInt(-1)        // display: all
            d.writeInt(0)         // resolution
            d.writeInt(fps)
            d.writeInt(0)         // dfps
            d.writeInt(0)         // configBits
            d.writeInt(0)         // states
            d.writeInt(PRIORITY)
            d.writeInt(0)         // duration: until released
            d.writeInt(0)         // caller: system_server owns the vote
            d.writeInt(0)         // client
            d.writeInt(FLAGS)
            d.writeInt(0)
            d.writeInt(0)
            b.transact(TX_ACQUIRE_V2, d, null, IBinder.FLAG_ONEWAY)
        } catch (_: Throwable) {
            cached = null; false
        } finally {
            d.recycle()
        }
    }

    fun release(): Boolean {
        val b = binder() ?: return false
        val d = Parcel.obtain()
        return try {
            d.writeInt(0)
            d.writeLong(HANDLE)
            b.transact(TX_RELEASE, d, null, IBinder.FLAG_ONEWAY)
        } catch (_: Throwable) {
            cached = null; false
        } finally {
            d.recycle()
        }
    }

    /** Rate the vendor has applied, or null when rms can't be reached. */
    fun rate(): Float? = binder()?.let { read(it) }

    private fun read(b: IBinder): Float? {
        val d = Parcel.obtain()
        val r = Parcel.obtain()
        return try {
            b.transact(TX_GET, d, r, 0)
            r.readException()
            r.readFloat()
        } catch (_: Throwable) {
            null
        } finally {
            d.recycle(); r.recycle()
        }
    }

    /** "Vivo144|144", or "reqFps:<activity>|120" when the foreground app sets its own mode (it wins). */
    fun reason(ctx: Context): String? {
        val key = "vivo_rms_active_request_reason"
        try {
            Settings.Secure.getString(ctx.contentResolver, key)?.let { if (it.isNotBlank()) return it }
        } catch (_: Throwable) { }
        if (ShizukuState.status.value != ShizukuState.Status.READY) return null
        val r = Shell.exec("settings get secure $key")
        return r.out.takeIf { r.ok && it.isNotBlank() && it != "null" }
    }

    /**
     * Rates that can be pinned, highest first. OriginOS hides 144 Hz from the
     * public display modes (it is a "private fps", persist.sys.private_fps),
     * so those props are merged in, and 144 is always offered.
     */
    fun panelRates(ctx: Context): List<Int> {
        val rates = sortedSetOf<Int>(compareByDescending { it })
        try {
            val dm = ctx.getSystemService(DisplayManager::class.java)
            dm.getDisplay(Display.DEFAULT_DISPLAY).supportedModes.forEach { rates += Math.round(it.refreshRate) }
        } catch (_: Throwable) { }
        for (prop in listOf("persist.sys.private_fps", "persist.vivo.phone.fps_max")) {
            getprop(prop).split(',', ' ', ';', ':')
                .mapNotNull { it.trim().toFloatOrNull()?.let { f -> Math.round(f) } }
                .filter { it in 60..240 }
                .forEach { rates += it }
        }
        rates += 144
        return rates.toList()
    }

    private fun getprop(name: String): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", name))
        p.inputStream.bufferedReader().use { it.readText() }.trim().also { p.waitFor() }
    } catch (_: Throwable) { "" }
}
