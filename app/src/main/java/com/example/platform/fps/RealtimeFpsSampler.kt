package com.example.platform.fps

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.example.data.models.FpsMetrics
import com.vivo144.app.core.Foreground
import com.vivo144.app.core.FpsMeter
import com.vivo144.app.core.Rms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.ArrayDeque
import kotlin.math.roundToInt

/**
 * Real FPS of the game in front, measured by SurfaceFlinger through Shizuku
 * (see FpsMeter). The original version counted this app's own Choreographer
 * frames, which only ever shows the panel's refresh rate.
 * Without Shizuku or usage access it falls back to the panel rate and says so.
 */
class RealtimeFpsSampler(private val context: Context) {

    fun observeFps(targetRefreshRate: Float = 144.0f): Flow<FpsMetrics> = flow {
        val history = ArrayDeque<Int>(20)
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

        while (true) {
            val panelHz = Rms.rate()
                ?: dm?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate
                ?: targetRefreshRate
            val pkg = Foreground.current(context)
            val fps = if (pkg != null && pkg != context.packageName) FpsMeter.measure(pkg) else null

            val note: String?
            val current: Int
            if (fps != null) {
                current = fps.roundToInt().coerceIn(0, 240)
                note = null
            } else {
                current = panelHz.roundToInt()
                note = when {
                    !FpsMeter.available() -> "Sem Shizuku: mostrando a taxa da tela, não o FPS do jogo"
                    pkg == null -> "Libere o acesso ao uso para medir o FPS do jogo"
                    else -> "FPS indisponível (${FpsMeter.lastError ?: "abra um jogo"}); mostrando a taxa da tela"
                }
            }

            if (history.size >= 20) history.removeFirst()
            history.addLast(current)
            val list = history.toList()
            val minFps = list.minOrNull() ?: current
            val maxFps = list.maxOrNull() ?: current
            val spread = maxFps - minFps
            emit(
                FpsMetrics(
                    currentFps = current,
                    averageFps = list.average().roundToInt(),
                    minFps = minFps,
                    maxFps = maxFps,
                    frameTimeMs = if (current > 0) ((10000f / current).roundToInt() / 10f) else 0f,
                    refreshRateHz = panelHz,
                    isStable = spread <= 8,
                    stabilityStatus = when {
                        spread <= 8 -> "Stable"
                        spread <= 18 -> "Minor Fluctuations"
                        else -> "Throttling"
                    },
                    fpsHistory = list,
                    isMonitoring = true,
                    accessNote = note,
                )
            )
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)
}
