package com.vivo144.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.vivo144.app.core.Brightness
import com.vivo144.app.core.SettingsIO
import com.vivo144.app.core.Util
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One accessibility service for the two features that need one:
 *  (a) Power bridge: when the vivo assistant window opens (held Power ~0.5 s),
 *      close it and open the native power menu. It never filters key events,
 *      so Power + Volume-down screenshots are untouched.
 *  (b) Brightness overlay: a TYPE_ACCESSIBILITY_OVERLAY window whose
 *      screenBrightness overrides auto-brightness. It never sets
 *      preferredRefreshRate / preferredDisplayModeId, so the 144 Hz vote stays.
 */
class UtilityAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: UtilityAccessibilityService? = null
            private set

        const val CLOSE_DELAY_MS = 0L
        const val DIALOG_DELAY_MS = 80L
        private const val COOLDOWN_MS = 1500L
        private const val DISCOVERY_MS = 30_000L

        /** Last 20 Power-bridge triggers. */
        val triggers = MutableStateFlow<List<String>>(emptyList())
        /** Windows seen during discovery: "hh:mm:ss  package  class". */
        val discovered = MutableStateFlow<List<Pair<String, String>>>(emptyList())
        /** Discovery end time (elapsedRealtime), 0 = off. */
        val discoveryUntil = MutableStateFlow(0L)

        private fun now() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }

    private val main = Handler(Looper.getMainLooper())
    private var lastTrigger = 0L
    private var overlay: View? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "watched_packages") applyFilter()
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_OFF && Util.revertOnScreenOff && Util.bypass) {
                Thread { Brightness.disable(applicationContext) }.start()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Util.listen(prefsListener)
        applyFilter()
        val f = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenOff, f)
        // Put the overlay back if the bypass was on (e.g. after a reboot or a kill).
        if (Util.bypass) showOverlay(Util.level)
        Log.i(SettingsIO.TAG, "accessibility service connected")
    }

    /** Watched packages from prefs; null (= every package) while discovering. */
    fun applyFilter() {
        val info = serviceInfo ?: return
        info.packageNames = if (discoveryUntil.value > SystemClock.elapsedRealtime()) null
        else Util.watched.ifEmpty { listOf("com.vivo.agent") }.toTypedArray()
        serviceInfo = info
    }

    fun startDiscovery() {
        discovered.value = emptyList()
        discoveryUntil.value = SystemClock.elapsedRealtime() + DISCOVERY_MS
        applyFilter()
        main.removeCallbacks(stopDiscovery)
        main.postDelayed(stopDiscovery, DISCOVERY_MS)
    }

    private val stopDiscovery = Runnable {
        discoveryUntil.value = 0L
        applyFilter()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        val cls = e.className?.toString() ?: ""

        if (discoveryUntil.value > SystemClock.elapsedRealtime()) {
            if (pkg != packageName) {
                discovered.value = (discovered.value + ("${now()}  $pkg  $cls" to pkg)).takeLast(40)
            }
            return // discovery never closes anything
        }
        if (!Util.powerBridge || pkg !in Util.watched) return

        val t = SystemClock.elapsedRealtime()
        if (t - lastTrigger < COOLDOWN_MS) return
        lastTrigger = t

        val action = Util.closeAction
        main.postDelayed({
            when (action) {
                Util.Close.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                Util.Close.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                Util.Close.NENHUMA -> Unit
            }
            main.postDelayed({ performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }, DIALOG_DELAY_MS)
        }, CLOSE_DELAY_MS)

        val line = "${now()}  $pkg  $cls  → ${action.name} + menu de energia"
        triggers.value = (listOf(line) + triggers.value).take(20)
        Log.i(SettingsIO.TAG, "power bridge: $line")
    }

    override fun onInterrupt() {}

    // ---------------- brightness overlay ----------------

    fun showOverlay(level: Float) {
        main.post {
            val wm = getSystemService(WindowManager::class.java)
            val full = Util.fullOverlay
            val lp = (overlay?.layoutParams as? WindowManager.LayoutParams) ?: WindowManager.LayoutParams(
                if (full) WindowManager.LayoutParams.MATCH_PARENT else 1,
                if (full) WindowManager.LayoutParams.MATCH_PARENT else 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            lp.screenBrightness = level.coerceIn(0.01f, 1f)
            if (Util.hdr) {
                try { lp.colorMode = ActivityInfo.COLOR_MODE_HDR } catch (_: Throwable) { }
                if (Build.VERSION.SDK_INT >= 35) try {
                    lp.javaClass.getMethod("setDesiredHdrHeadroom", Float::class.javaPrimitiveType).invoke(lp, 5.0f)
                } catch (_: Throwable) { }
            } else {
                try { lp.colorMode = ActivityInfo.COLOR_MODE_DEFAULT } catch (_: Throwable) { }
            }
            try {
                val v = overlay
                if (v == null) {
                    val nv = View(this).apply { setBackgroundColor(Color.argb(2, 255, 255, 255)) }
                    wm.addView(nv, lp)
                    overlay = nv
                } else {
                    wm.updateViewLayout(v, lp)
                }
            } catch (t: Throwable) {
                Log.w(SettingsIO.TAG, "overlay failed", t)
            }
        }
    }

    /** Re-create the overlay (size/HDR options changed). */
    fun rebuildOverlay() {
        removeOverlay()
        if (Util.bypass) showOverlay(Util.level)
    }

    fun removeOverlay() {
        main.post {
            overlay?.let { v ->
                try { getSystemService(WindowManager::class.java).removeView(v) } catch (_: Throwable) { }
            }
            overlay = null
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        if (instance !== this) return
        instance = null
        Util.unlisten(prefsListener)
        try { unregisterReceiver(screenOff) } catch (_: Throwable) { }
        removeOverlay()
    }
}
