package com.vivo144.app.core

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.view.Display
import rikka.shizuku.ShizukuBinderWrapper

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

    /**
     * DIRECT  – the app looked rms up itself (works where SELinux allows it)
     * SHIZUKU – rms was looked up *by the Shizuku server* (shell), so the
     *           lookup is not blocked; calls go through ShizukuBinderWrapper
     * DEX     – last resort: this APK's RmsCli run via app_process as shell
     *           (the same technique as RmsVote.dex, with our own code)
     */
    enum class Path { DIRECT, SHIZUKU, DEX, NONE }

    @Volatile var path = Path.NONE
        private set
    /** What happened on the last lookup, shown in the UI. */
    @Volatile var diag = ""
        private set
    @Volatile private var cached: IBinder? = null
    @Volatile private var dexReady = false
    @Volatile private var lastTry = 0L

    lateinit var app: Context

    @Synchronized
    fun binder(): IBinder? {
        cached?.let { if (it.isBinderAlive) return it }
        cached = null
        if (path == Path.DEX && dexReady) return null
        // Don't hammer the lookup when nothing works.
        val now = System.currentTimeMillis()
        if (path == Path.NONE && now - lastTry < 3000) return null
        lastTry = now
        val notes = mutableListOf<String>()

        // 1) Direct lookup from the app process.
        try {
            val b = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "rms") as IBinder?
            when {
                b == null -> notes += "direto: rms oculto para apps"
                read(b) == null -> notes += "direto: rms não respondeu"
                else -> { path = Path.DIRECT; diag = "Direto"; cached = b; return b }
            }
        } catch (t: Throwable) {
            notes += "direto: ${t.cause ?: t}"
        }

        if (!ShizukuState.ready(1500)) {
            notes += "Shizuku indisponível"
            path = Path.NONE; diag = notes.joinToString(" · ")
            return null
        }

        // 2) Ask servicemanager *through Shizuku*, so the lookup runs as shell.
        try {
            val raw = lookupViaShizuku("rms")
            if (raw == null) {
                notes += "shizuku: servicemanager não devolveu rms"
            } else {
                val w = ShizukuBinderWrapper(raw)
                if (read(w) != null) {
                    path = Path.SHIZUKU; diag = "Shizuku (busca como shell)"; cached = w; return w
                }
                notes += "shizuku: rms não respondeu"
            }
        } catch (t: Throwable) {
            notes += "shizuku: ${t.cause ?: t}"
        }

        // 3) Our own RmsCli through app_process as shell, like the official RmsVote CLI.
        if (prepareDex()) {
            path = Path.DEX; diag = "Shell via app_process"
            return null
        }
        notes += "app_process: $dexError"
        path = Path.NONE; diag = notes.joinToString(" · ")
        return null
    }

    private fun lookupViaShizuku(name: String): IBinder? {
        val ctxObj = Class.forName("com.android.internal.os.BinderInternal")
            .getMethod("getContextObject").invoke(null) as IBinder
        val sm = Class.forName("android.os.IServiceManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(ctxObj))!!
        for (m in listOf("checkService", "getService", "checkService2", "getService2")) {
            val r = try {
                sm.javaClass.getMethod(m, String::class.java).invoke(sm, name)
            } catch (_: NoSuchMethodException) { continue }
            unwrap(r)?.let { return it }
        }
        return null
    }

    /** Android 16 may return a `Service` union instead of a bare IBinder. */
    private fun unwrap(r: Any?): IBinder? {
        if (r == null) return null
        if (r is IBinder) return r
        for (getter in listOf("getServiceWithMetadata", "getBinder")) {
            try {
                val v = r.javaClass.getMethod(getter).invoke(r)
                if (v is IBinder) return v
                if (v != null) {
                    val f = v.javaClass.getField("service").get(v)
                    if (f is IBinder) return f
                }
            } catch (_: Throwable) { }
        }
        return try { r.javaClass.getField("service").get(r) as? IBinder } catch (_: Throwable) { null }
    }

    @Volatile private var dexError = ""

    /** Checks that `app_process` can run our RmsCli as shell and reach rms. */
    private fun prepareDex(): Boolean {
        if (dexReady) return true
        val r = dex("get")
        dexReady = r.ok && r.out.startsWith("rate=")
        dexError = if (dexReady) "" else r.all.take(160)
        return dexReady
    }

    private fun dex(args: String): Shell.Result {
        val apk = app.applicationInfo.sourceDir
        return Shell.exec("CLASSPATH=${Shell.q(apk)} app_process /system/bin com.vivo144.app.core.RmsCli $args")
    }

    /** Votes [fps] in the FixRate scene. Voting again with the same handle replaces it. */
    fun vote(fps: Int): Boolean {
        val b = binder()
        if (b == null) return path == Path.DEX && dex("set $fps").ok
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
        val b = binder()
        if (b == null) return path == Path.DEX && dex("release 0 $HANDLE").ok
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

    @Volatile private var dexRate: Float? = null
    @Volatile private var dexRateAt = 0L

    /** Rate the vendor has applied, or null when rms can't be reached. */
    fun rate(): Float? {
        val b = binder()
        if (b != null) return read(b)
        if (path != Path.DEX) return null
        // app_process starts a whole runtime: read at most every 5 s.
        val now = System.currentTimeMillis()
        if (now - dexRateAt > 5000) {
            dexRateAt = now
            dexRate = Regex("[0-9]+(\\.[0-9]+)?").find(dex("get").out)?.value?.toFloatOrNull()
        }
        return dexRate
    }

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
