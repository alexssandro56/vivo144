package com.vivo144.app.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** App in front, from UsageStats events (needs usage access). */
object Foreground {
    private var last: String? = null
    private var since = 0L

    @Synchronized
    fun current(c: Context): String? {
        if (!Apps.hasUsageAccess(c)) return null
        val usm = c.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        if (since == 0L) since = now - 10 * 60_000L
        val ev = usm.queryEvents(since, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.packageName != "com.android.systemui") {
                last = e.packageName
            }
        }
        since = now
        return last
    }
}

/**
 * Real frame rate of the app in front, measured by SurfaceFlinger:
 * `dumpsys SurfaceFlinger --latency <layer>` lists the actual present time of
 * the last ~128 frames of a layer. Runs through Shizuku (shell); `dumpsys` is
 * a small native binary, so one call per second is cheap.
 */
object FpsMeter {
    private var layer: String? = null
    private var layerPkg: String? = null
    private var layerAt = 0L
    @Volatile var lastError: String? = null
        private set

    fun available() = ShizukuState.status.value == ShizukuState.Status.READY

    /** Frames per second over the last second, 0 when the app drew nothing, null if unknown. */
    @Synchronized
    fun measure(pkg: String): Float? {
        if (!available()) { lastError = "sem Shizuku"; return null }
        val now = System.currentTimeMillis()
        if (pkg != layerPkg || layer == null || now - layerAt > 5000) {
            layer = findLayer(pkg)
            layerPkg = pkg
            layerAt = now
        }
        val l = layer ?: run { lastError = "camada do app não encontrada"; return null }
        val r = Shell.exec("dumpsys SurfaceFlinger --latency ${Shell.q(l)}")
        if (!r.ok) { lastError = r.all.take(120); layer = null; return null }
        val times = r.out.lineSequence().drop(1).mapNotNull { line ->
            val cols = line.trim().split(Regex("\\s+"))
            cols.getOrNull(1)?.toLongOrNull()
        }.filter { it > 0 && it < Long.MAX_VALUE / 2 }.sorted().toList()
        if (times.size < 2) { lastError = null; return 0f }
        val end = times.last()
        // Only count frames that are recent, relative to "now" in the same clock.
        val nowNs = System.nanoTime()
        if (nowNs - end > 1_000_000_000L) { lastError = null; return 0f }
        val window = times.filter { it >= end - 1_000_000_000L }
        if (window.size < 2) { lastError = null; return 0f }
        lastError = null
        return (window.size - 1) * 1e9f / (window.last() - window.first()).toFloat()
    }

    /** Picks the layer that draws the app's content (games usually render into a SurfaceView). */
    private fun findLayer(pkg: String): String? {
        val list = Shell.exec("dumpsys SurfaceFlinger --list").out.lineSequence()
            .map { it.trim() }.filter { it.contains(pkg) }.toList()
        if (list.isEmpty()) return null
        return list.firstOrNull { it.startsWith("SurfaceView") && !it.contains("Background") }
            ?: list.firstOrNull { it.contains("SurfaceView") && !it.contains("Background") }
            ?: list.lastOrNull { !it.contains("Splash") && !it.startsWith("ActivityRecord") && !it.startsWith("Task") }
            ?: list.last()
    }
}
