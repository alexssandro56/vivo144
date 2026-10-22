package com.example.platform.fps

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Display
import com.example.data.models.FpsMetrics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque

class RealtimeFpsSampler(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun observeFps(targetRefreshRate: Float = 144.0f): Flow<FpsMetrics> = callbackFlow {
        var frameCount = 0
        var lastCalcTimeNanos = System.nanoTime()
        val fpsHistoryQueue = ArrayDeque<Int>(20)
        for (i in 0 until 10) {
            fpsHistoryQueue.add(targetRefreshRate.toInt())
        }

        var isRunning = true

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val currentDisplayHz = display?.refreshRate ?: targetRefreshRate

        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRunning) return

                frameCount++
                val now = System.nanoTime()
                val elapsedNanos = now - lastCalcTimeNanos

                // Calculate every 500ms
                if (elapsedNanos >= 500_000_000L) {
                    val seconds = elapsedNanos.toDouble() / 1_000_000_000.0
                    val measuredFps = ((frameCount.toDouble() / seconds)).toInt().coerceIn(1, 240)
                    val frameTimeMs = if (measuredFps > 0) 1000.0f / measuredFps.toFloat() else 0.0f

                    if (fpsHistoryQueue.size >= 20) {
                        fpsHistoryQueue.removeFirst()
                    }
                    fpsHistoryQueue.addLast(measuredFps)

                    val historyList = fpsHistoryQueue.toList()
                    val avgFps = if (historyList.isNotEmpty()) historyList.average().toInt() else measuredFps
                    val minFps = historyList.minOrNull() ?: measuredFps
                    val maxFps = historyList.maxOrNull() ?: measuredFps

                    val isStable = (maxFps - minFps) <= 8
                    val stabilityStatus = when {
                        isStable -> "Stable"
                        (maxFps - minFps) <= 18 -> "Minor Fluctuations"
                        else -> "Throttling"
                    }

                    trySend(
                        FpsMetrics(
                            currentFps = measuredFps,
                            averageFps = avgFps,
                            minFps = minFps,
                            maxFps = maxFps,
                            frameTimeMs = String.format("%.1f", frameTimeMs).toFloatOrNull() ?: frameTimeMs,
                            refreshRateHz = currentDisplayHz,
                            isStable = isStable,
                            stabilityStatus = stabilityStatus,
                            fpsHistory = historyList,
                            isMonitoring = true,
                            accessNote = null
                        )
                    )

                    frameCount = 0
                    lastCalcTimeNanos = now
                }

                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        mainHandler.post {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        awaitClose {
            isRunning = false
            mainHandler.post {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }
    }
}
