package com.vivo144.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.vivo144.app.R
import com.vivo144.app.core.Apps
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs
import com.vivo144.app.core.Rms
import com.vivo144.app.ui.MainActivity

/**
 * Automatic mode: watches which app is in front (UsageStats, polled once a
 * second only while the screen is on) and votes the chosen rate while a
 * selected game is in front, releasing it as soon as you leave. Also draws the
 * optional HUD.
 */
class AutoService : Service() {
    companion object {
        /** What the automatic mode is doing right now, for the Home screen. */
        val status = kotlinx.coroutines.flow.MutableStateFlow("Parado")
    }

    private lateinit var worker: HandlerThread
    private lateinit var bg: Handler
    private val ui = Handler(Looper.getMainLooper())

    private var lastQuery = 0L
    private var foreground: String? = null
    @Volatile private var voted: Boolean? = null   // null = unknown, enforce on next tick
    @Volatile private var votedRate = -1

    private fun idleText() = if (Prefs.idleFps == 0) "LTPO automático" else "${Prefs.idleFps} Hz"
    private var lastNotice = ""
    private var tick = 0

    private var wm: WindowManager? = null
    private var hud: TextView? = null

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_ON) { bg.removeCallbacks(loop); bg.post(loop) }
            else bg.removeCallbacks(loop)
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        voted = null; votedRate = -1
        ui.post { syncHud() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        goForeground("Iniciando…")
        worker = HandlerThread("auto").apply { start() }
        bg = Handler(worker.looper)
        lastQuery = System.currentTimeMillis() - 10 * 60_000L
        val f = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screen, f)
        Prefs.listen(prefsListener)
        syncHud()
        bg.post(loop)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        voted = null; votedRate = -1
        syncHud()
        return START_STICKY
    }

    private val loop = object : Runnable {
        override fun run() {
            step()
            val on = getSystemService(PowerManager::class.java).isInteractive
            if (on) bg.postDelayed(this, 1000)
        }
    }

    private fun step() {
        val auto = Prefs.mode == Mode.AUTO
        if (auto && Apps.hasUsageAccess(this)) {
            val fg = foregroundApp()
            val want = fg != null && fg in Prefs.games
            // Game in front → game rate; otherwise the chosen idle rate (0 = let vivo decide).
            val target = if (want) Prefs.fps else Prefs.idleFps
            if (want != voted || target != votedRate) {
                if (target == 0) Rms.release() else Rms.vote(target)
                voted = want
                votedRate = target
            } else if (target != 0 && tick % 10 == 0 && Rms.path != Rms.Path.DEX) {
                Rms.vote(target) // cheap one-way refresh; same handle replaces the vote
            }
            notice(
                when {
                    want -> "${label(fg!!)} aberto → voto de ${Prefs.fps} Hz enviado"
                    fg == null -> "Fora do jogo → ${idleText()} (nenhum app detectado ainda)"
                    else -> "Fora do jogo → ${idleText()} (na frente: ${label(fg)})"
                }
            )
        } else if (auto) {
            notice("Falta liberar o acesso ao uso (Ajustes do app)")
        } else {
            notice("Monitor de Hz")
        }
        if (Prefs.hud && hud != null) {
            // In the app_process path every read starts a runtime; don't do that during a game.
            val r = if (Rms.path == Rms.Path.DEX) null else Rms.rate()
            val text = if (r == null) (if (voted == true) "${Prefs.fps} Hz ●" else "LTPO") else "${Math.round(r)} Hz" + if (voted == true) " ●" else ""
            val good = if (r == null) voted == true else Math.round(r) >= Prefs.fps
            ui.post {
                hud?.text = text
                hud?.setTextColor(if (good) 0xFF3DFFB0.toInt() else 0xFFFFC266.toInt())
            }
        }
        tick++
    }

    /** Last app that resumed an activity, from UsageStats events since the previous poll. */
    private fun foregroundApp(): String? {
        val usm = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(lastQuery, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.packageName != "com.android.systemui") {
                foreground = e.packageName
            }
        }
        lastQuery = now
        return foreground
    }

    private fun label(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Throwable) { pkg }

    // ---------- notification ----------

    private fun goForeground(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("auto", "Modo automático", NotificationManager.IMPORTANCE_MIN))
        val n = build(text)
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun build(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "auto")
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("Vivo 144Hz")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun notice(text: String) {
        status.value = text
        if (text == lastNotice) return
        lastNotice = text
        getSystemService(NotificationManager::class.java).notify(1, build(text))
    }

    // ---------- HUD ----------

    @SuppressLint("ClickableViewAccessibility")
    private fun syncHud() {
        val want = Prefs.hud && Apps.canOverlay(this)
        if (want && hud == null) {
            val w = getSystemService(WindowManager::class.java)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 140 }
            val d = resources.displayMetrics.density
            val v = TextView(this).apply {
                text = "… Hz"
                textSize = 12f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(0xFF3DFFB0.toInt())
                setPadding((10 * d).toInt(), (5 * d).toInt(), (10 * d).toInt(), (5 * d).toInt())
                background = GradientDrawable().apply { cornerRadius = 14 * d; setColor(0xCC0B0F14.toInt()) }
            }
            var x0 = 0; var y0 = 0; var tx = 0f; var ty = 0f
            v.setOnTouchListener { view: View, e: MotionEvent ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { x0 = lp.x; y0 = lp.y; tx = e.rawX; ty = e.rawY }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = x0 + (e.rawX - tx).toInt(); lp.y = y0 + (e.rawY - ty).toInt()
                        w.updateViewLayout(view, lp)
                    }
                }
                true
            }
            try { w.addView(v, lp); wm = w; hud = v } catch (_: Throwable) { }
        } else if (!want && hud != null) {
            try { wm?.removeView(hud) } catch (_: Throwable) { }
            hud = null
        }
    }

    override fun onDestroy() {
        status.value = "Parado"
        Prefs.unlisten(prefsListener)
        try { unregisterReceiver(screen) } catch (_: Throwable) { }
        bg.removeCallbacksAndMessages(null)
        worker.quitSafely()
        hud?.let { try { wm?.removeView(it) } catch (_: Throwable) { } }
        hud = null
        super.onDestroy()
    }
}
