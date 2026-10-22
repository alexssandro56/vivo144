package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VivoFpsApplication
import com.example.data.AppContainer
import com.example.data.models.FpsMetrics
import com.example.data.models.OverlayHudStyle
import com.example.data.models.OverlayPreferences
import com.example.platform.fps.RealtimeFpsSampler
import com.example.ui.overlay.FpsFloatingHudView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FpsOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fpsObservationJob: Job? = null
    private var prefsObservationJob: Job? = null
    private var batteryObservationJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var floatingHudView: FpsFloatingHudView? = null
    private var isViewAttached = false

    private lateinit var layoutParams: WindowManager.LayoutParams
    private var currentPreferences = OverlayPreferences.DEFAULT
    private var currentMetrics = FpsMetrics()
    private var currentTempCelsius = 36.5f

    private val appContainer: AppContainer
        get() = (application as? VivoFpsApplication)?.appContainer
            ?: AppContainer(applicationContext)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_STYLE -> {
                toggleHudStyle()
            }
            ACTION_START -> {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(
                        this,
                        "Overlay permission required to display FPS counter",
                        Toast.LENGTH_LONG
                    ).show()
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(NOTIFICATION_ID, buildNotification(currentMetrics))
                startOverlay()
            }
        }

        return START_STICKY
    }

    private fun startOverlay() {
        currentPreferences = appContainer.userPreferencesRepository.overlayPreferencesFlow.value

        initLayoutParams()
        attachOverlayView()
        observePreferences()
        observeFps()
        observeBatteryTemp()

        appContainer.userPreferencesRepository.setOverlayEnabled(true)
    }

    private fun initLayoutParams() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentPreferences.posX
            y = currentPreferences.posY
        }
    }

    private fun attachOverlayView() {
        if (isViewAttached) return

        try {
            floatingHudView = FpsFloatingHudView(
                context = this,
                windowManager = windowManager,
                layoutParams = layoutParams,
                onSavePosition = { x, y ->
                    appContainer.userPreferencesRepository.setOverlayPosition(x, y)
                },
                onCloseRequested = {
                    stopSelf()
                }
            ).apply {
                updatePreferences(currentPreferences)
                updateMetrics(currentMetrics, currentTempCelsius)
            }

            windowManager.addView(floatingHudView, layoutParams)
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun observePreferences() {
        prefsObservationJob?.cancel()
        prefsObservationJob = serviceScope.launch {
            appContainer.userPreferencesRepository.overlayPreferencesFlow.collectLatest { prefs ->
                currentPreferences = prefs
                floatingHudView?.updatePreferences(prefs)
            }
        }
    }

    private fun observeFps() {
        fpsObservationJob?.cancel()
        val fpsSampler = RealtimeFpsSampler(this)

        fpsObservationJob = serviceScope.launch {
            val targetFps = appContainer.userPreferencesRepository.userPreferencesFlow.value.targetFps.toFloat()
            fpsSampler.observeFps(targetRefreshRate = targetFps).collectLatest { metrics ->
                currentMetrics = metrics
                floatingHudView?.updateMetrics(metrics, currentTempCelsius)
                updateNotification(metrics)
            }
        }
    }

    private fun observeBatteryTemp() {
        batteryObservationJob?.cancel()
        batteryObservationJob = serviceScope.launch {
            try {
                appContainer.batteryRepository.observeBattery().collectLatest { info ->
                    currentTempCelsius = info.temperatureCelsius ?: currentTempCelsius
                    floatingHudView?.updateMetrics(currentMetrics, currentTempCelsius)
                }
            } catch (_: Exception) {}
        }
    }

    private fun toggleHudStyle() {
        val nextStyle = when (currentPreferences.hudStyle) {
            OverlayHudStyle.COMPACT_PILL -> OverlayHudStyle.EXPANDED_CARD
            OverlayHudStyle.EXPANDED_CARD -> OverlayHudStyle.MINIMAL_BADGE
            OverlayHudStyle.MINIMAL_BADGE -> OverlayHudStyle.GRAPH_ONLY
            OverlayHudStyle.GRAPH_ONLY -> OverlayHudStyle.COMPACT_PILL
        }
        appContainer.userPreferencesRepository.setOverlayHudStyle(nextStyle)
    }

    private fun updateNotification(metrics: FpsMetrics) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(metrics))
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun buildNotification(metrics: FpsMetrics): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FpsOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleStyleIntent = Intent(this, FpsOverlayService::class.java).apply {
            action = ACTION_TOGGLE_STYLE
        }
        val toggleStylePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleStyleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎮 ${metrics.currentFps} FPS • ${metrics.refreshRateHz.toInt()} Hz")
            .setContentText("Frame time: ${metrics.frameTimeMs} ms • Temp: ${String.format("%.1f°C", currentTempCelsius)} • ${metrics.stabilityStatus}")
            .setSmallIcon(R.drawable.ic_qs_high_performance)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_qs_high_performance, "HUD Style", toggleStylePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vivo Real-Time FPS Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live frames-per-second monitoring status on top of games"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        if (isViewAttached && floatingHudView != null) {
            try {
                windowManager.removeView(floatingHudView)
            } catch (_: Exception) {}
            isViewAttached = false
            floatingHudView = null
        }

        appContainer.userPreferencesRepository.setOverlayEnabled(false)
    }

    companion object {
        const val CHANNEL_ID = "channel_vivo_fps_overlay"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.example.service.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.service.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_STYLE = "com.example.service.action.TOGGLE_STYLE"

        fun start(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
