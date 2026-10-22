package com.example.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.example.MainActivity
import com.example.data.models.FpsMetrics
import com.example.data.models.OverlayHudStyle
import com.example.data.models.OverlayPreferences
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class FpsFloatingHudView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onSavePosition: (Int, Int) -> Unit,
    private val onCloseRequested: () -> Unit
) : View(context) {

    private var metrics = FpsMetrics(
        currentFps = 144,
        averageFps = 144,
        minFps = 138,
        maxFps = 144,
        frameTimeMs = 6.9f,
        refreshRateHz = 144f,
        isStable = true,
        stabilityStatus = "Stable",
        fpsHistory = listOf(144, 144, 144, 143, 144, 144, 142, 144, 144, 144)
    )

    private var preferences = OverlayPreferences.DEFAULT
    private var batteryTempCelsius: Float = 36.5f

    // Touch dragging tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchDownTime = 0L
    private var isDragging = false

    // Pulse animator for live dot
    private var pulseAlpha = 1.0f
    private val pulseAnimator = ValueAnimator.ofFloat(0.4f, 1.0f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulseAlpha = it.animatedValue as Float
            invalidate()
        }
    }

    // Dimension helpers (DP to PX)
    private val density = resources.displayMetrics.density
    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = value * density

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val graphLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val graphFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.argb(40, 255, 255, 255)
        pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
    }

    private val closeButtonRect = RectF()
    private val expandButtonRect = RectF()
    private val appShortcutRect = RectF()

    init {
        pulseAnimator.start()
        isClickable = true
        isFocusable = false
    }

    fun updateMetrics(newMetrics: FpsMetrics, tempCelsius: Float = 36.5f) {
        this.metrics = newMetrics
        this.batteryTempCelsius = tempCelsius
        invalidate()
    }

    fun updatePreferences(newPreferences: OverlayPreferences) {
        val styleChanged = this.preferences.hudStyle != newPreferences.hudStyle
        this.preferences = newPreferences
        if (styleChanged) {
            requestLayout()
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int
        val height: Int
        when (preferences.hudStyle) {
            OverlayHudStyle.COMPACT_PILL -> {
                width = dp(148f).toInt()
                height = dp(46f).toInt()
            }
            OverlayHudStyle.EXPANDED_CARD -> {
                width = dp(240f).toInt()
                height = dp(210f).toInt()
            }
            OverlayHudStyle.MINIMAL_BADGE -> {
                width = dp(96f).toInt()
                height = dp(34f).toInt()
            }
            OverlayHudStyle.GRAPH_ONLY -> {
                width = dp(180f).toInt()
                height = dp(76f).toInt()
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val alphaMultiplier = preferences.opacityFloat

        // Determine health color based on FPS
        val fpsColor = when {
            metrics.currentFps >= 120 -> Color.rgb(16, 185, 129) // Emerald Neon
            metrics.currentFps >= 85 -> Color.rgb(245, 158, 11)  // Amber Gold
            else -> Color.rgb(239, 68, 68)                       // Crimson Alert
        }

        when (preferences.hudStyle) {
            OverlayHudStyle.COMPACT_PILL -> drawCompactPill(canvas, w, h, alphaMultiplier, fpsColor)
            OverlayHudStyle.EXPANDED_CARD -> drawExpandedCard(canvas, w, h, alphaMultiplier, fpsColor)
            OverlayHudStyle.MINIMAL_BADGE -> drawMinimalBadge(canvas, w, h, alphaMultiplier, fpsColor)
            OverlayHudStyle.GRAPH_ONLY -> drawGraphOnly(canvas, w, h, alphaMultiplier, fpsColor)
        }
    }

    private fun drawCompactPill(canvas: Canvas, w: Float, h: Float, alphaMultiplier: Float, fpsColor: Int) {
        val cornerRadius = h / 2f
        val rect = RectF(dp(2f), dp(2f), w - dp(2f), h - dp(2f))

        // Background Glass
        val bgAlpha = (225 * alphaMultiplier).toInt().coerceIn(0, 255)
        bgPaint.color = Color.argb(bgAlpha, 12, 14, 22)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Glowing border
        borderPaint.strokeWidth = dp(1.8f)
        val borderAlpha = (200 * alphaMultiplier).toInt().coerceIn(0, 255)
        borderPaint.color = Color.argb(borderAlpha, Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Pulsing Live Dot
        val dotX = dp(16f)
        val dotY = h / 2f
        val dotRadius = dp(4.5f)
        dotPaint.color = Color.argb((255 * pulseAlpha).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)

        // Glow ring around dot
        dotPaint.color = Color.argb((80 * pulseAlpha).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawCircle(dotX, dotY, dotRadius + dp(3f), dotPaint)

        // Large FPS text
        textPaint.textSize = sp(19f)
        textPaint.color = Color.WHITE
        val fpsStr = "${metrics.currentFps}"
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f - dp(3f)
        canvas.drawText(fpsStr, dp(26f), textY, textPaint)

        val fpsStrWidth = textPaint.measureText(fpsStr)

        // "FPS" small label
        labelPaint.textSize = sp(9f)
        labelPaint.color = fpsColor
        canvas.drawText("FPS", dp(28f) + fpsStrWidth, textY - dp(2f), labelPaint)

        // Subtitle bottom badge: "144Hz • 6.9ms"
        subTextPaint.textSize = sp(9.5f)
        subTextPaint.color = Color.argb(190, 203, 213, 225)
        val subInfo = "${metrics.refreshRateHz.toInt()}Hz • ${metrics.frameTimeMs}ms"
        canvas.drawText(subInfo, dp(26f), h - dp(9f), subTextPaint)
    }

    private fun drawExpandedCard(canvas: Canvas, w: Float, h: Float, alphaMultiplier: Float, fpsColor: Int) {
        val cornerRadius = dp(18f)
        val rect = RectF(dp(2f), dp(2f), w - dp(2f), h - dp(2f))

        // Card Glass Background
        val bgAlpha = (240 * alphaMultiplier).toInt().coerceIn(0, 255)
        bgPaint.color = Color.argb(bgAlpha, 10, 12, 18)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Card Glowing Accent Border
        borderPaint.strokeWidth = dp(1.6f)
        val borderAlpha = (180 * alphaMultiplier).toInt().coerceIn(0, 255)
        borderPaint.color = Color.argb(borderAlpha, Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 1. Header Row
        dotPaint.color = Color.argb((255 * pulseAlpha).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawCircle(dp(16f), dp(18f), dp(4f), dotPaint)

        labelPaint.textSize = sp(10f)
        labelPaint.color = Color.argb(220, 168, 85, 247) // Violet label
        canvas.drawText("VIVO TURBO HUD", dp(26f), dp(22f), labelPaint)

        // Close (X) Touch target at top-right
        val closeX = w - dp(24f)
        val closeY = dp(18f)
        closeButtonRect.set(closeX - dp(14f), closeY - dp(14f), closeX + dp(14f), closeY + dp(14f))

        textPaint.textSize = sp(13f)
        textPaint.color = Color.argb(180, 255, 255, 255)
        canvas.drawText("✕", closeX - dp(4f), closeY + dp(4f), textPaint)

        // 2. Primary FPS Readout
        textPaint.textSize = sp(30f)
        textPaint.color = Color.WHITE
        val fpsStr = "${metrics.currentFps}"
        canvas.drawText(fpsStr, dp(14f), dp(58f), textPaint)

        val fpsW = textPaint.measureText(fpsStr)
        labelPaint.textSize = sp(11f)
        labelPaint.color = fpsColor
        canvas.drawText("FPS", dp(18f) + fpsW, dp(44f), labelPaint)

        subTextPaint.textSize = sp(10.5f)
        subTextPaint.color = Color.argb(200, 148, 163, 184)
        canvas.drawText("${metrics.frameTimeMs} ms • ${metrics.stabilityStatus}", dp(18f) + fpsW, dp(58f), subTextPaint)

        // 3. Mini Sparkline Graph Area (x: 14 to w-14, y: 68 to 118)
        val graphLeft = dp(14f)
        val graphRight = w - dp(14f)
        val graphTop = dp(68f)
        val graphBottom = dp(118f)
        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop

        // Grid lines
        canvas.drawLine(graphLeft, graphTop, graphRight, graphTop, gridPaint)
        canvas.drawLine(graphLeft, graphTop + graphHeight / 2f, graphRight, graphTop + graphHeight / 2f, gridPaint)
        canvas.drawLine(graphLeft, graphBottom, graphRight, graphBottom, gridPaint)

        val history = metrics.fpsHistory
        if (history.isNotEmpty()) {
            val maxVal = max(144f, (metrics.maxFps + 5).toFloat())
            val minVal = max(30f, (metrics.minFps - 10).toFloat())
            val range = max(1f, maxVal - minVal)

            val path = Path()
            val fillPath = Path()

            history.forEachIndexed { index, fps ->
                val x = graphLeft + (index.toFloat() / max(1, history.size - 1)) * graphWidth
                val normY = ((fps - minVal) / range).coerceIn(0f, 1f)
                val y = graphBottom - (normY * graphHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, graphBottom)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(graphRight, graphBottom)
            fillPath.close()

            // Draw Fill Gradient
            graphFillPaint.shader = LinearGradient(
                graphLeft, graphTop, graphLeft, graphBottom,
                Color.argb((100 * alphaMultiplier).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor)),
                Color.argb(0, Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor)),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, graphFillPaint)

            // Draw Line
            graphLinePaint.strokeWidth = dp(2f)
            graphLinePaint.color = fpsColor
            canvas.drawPath(path, graphLinePaint)
        }

        // 4. Metrics 2x2 Grid (y: 126 to 168)
        val col1X = dp(14f)
        val col2X = dp(126f)

        // Row 1: 1% Low & Avg FPS
        drawStatBadge(canvas, col1X, dp(138f), "1% LOW", "${metrics.minFps} FPS", fpsColor)
        drawStatBadge(canvas, col2X, dp(138f), "AVG FPS", "${metrics.averageFps}", Color.WHITE)

        // Row 2: Refresh Rate & Battery Temp
        drawStatBadge(canvas, col1X, dp(164f), "HZ RATE", "${metrics.refreshRateHz.toInt()} Hz", Color.argb(220, 56, 189, 248))
        drawStatBadge(canvas, col2X, dp(164f), "TEMP", String.format("%.1f°C", batteryTempCelsius), Color.argb(220, 251, 146, 60))

        // 5. Bottom Shortcut Pill ("Tap to open app")
        val btnTop = dp(178f)
        val btnBottom = dp(200f)
        val btnRect = RectF(dp(14f), btnTop, w - dp(14f), btnBottom)
        appShortcutRect.set(btnRect)

        bgPaint.color = Color.argb(60, 168, 85, 247)
        canvas.drawRoundRect(btnRect, dp(6f), dp(6f), bgPaint)

        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = Color.argb(120, 168, 85, 247)
        canvas.drawRoundRect(btnRect, dp(6f), dp(6f), borderPaint)

        labelPaint.textSize = sp(9.5f)
        labelPaint.color = Color.argb(230, 216, 180, 254)
        val btnLabel = "OPEN VIVO FPS APP"
        val btnLabelW = labelPaint.measureText(btnLabel)
        canvas.drawText(btnLabel, (w - btnLabelW) / 2f, btnTop + dp(14.5f), labelPaint)
    }

    private fun drawStatBadge(canvas: Canvas, x: Float, y: Float, label: String, value: String, valueColor: Int) {
        labelPaint.textSize = sp(8f)
        labelPaint.color = Color.argb(150, 148, 163, 184)
        canvas.drawText(label, x, y - dp(9f), labelPaint)

        textPaint.textSize = sp(12f)
        textPaint.color = valueColor
        canvas.drawText(value, x, y + dp(3f), textPaint)
    }

    private fun drawMinimalBadge(canvas: Canvas, w: Float, h: Float, alphaMultiplier: Float, fpsColor: Int) {
        val cornerRadius = dp(8f)
        val rect = RectF(dp(1f), dp(1f), w - dp(1f), h - dp(1f))

        bgPaint.color = Color.argb((200 * alphaMultiplier).toInt(), 12, 14, 20)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        borderPaint.strokeWidth = dp(1.2f)
        borderPaint.color = Color.argb((160 * alphaMultiplier).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        textPaint.textSize = sp(16f)
        textPaint.color = Color.WHITE
        val fpsStr = "${metrics.currentFps}"
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(fpsStr, dp(10f), textY, textPaint)

        val fpsW = textPaint.measureText(fpsStr)
        labelPaint.textSize = sp(9f)
        labelPaint.color = fpsColor
        canvas.drawText("FPS", dp(14f) + fpsW, textY, labelPaint)
    }

    private fun drawGraphOnly(canvas: Canvas, w: Float, h: Float, alphaMultiplier: Float, fpsColor: Int) {
        val cornerRadius = dp(10f)
        val rect = RectF(dp(1f), dp(1f), w - dp(1f), h - dp(1f))

        bgPaint.color = Color.argb((220 * alphaMultiplier).toInt(), 10, 12, 18)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        borderPaint.strokeWidth = dp(1.2f)
        borderPaint.color = Color.argb((180 * alphaMultiplier).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Top Header
        textPaint.textSize = sp(12f)
        textPaint.color = Color.WHITE
        canvas.drawText("${metrics.currentFps} FPS", dp(10f), dp(18f), textPaint)

        subTextPaint.textSize = sp(9f)
        subTextPaint.color = Color.argb(180, 203, 213, 225)
        canvas.drawText("${metrics.frameTimeMs} ms", w - dp(54f), dp(18f), subTextPaint)

        // Sparkline area
        val graphLeft = dp(8f)
        val graphRight = w - dp(8f)
        val graphTop = dp(24f)
        val graphBottom = h - dp(6f)
        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop

        val history = metrics.fpsHistory
        if (history.isNotEmpty()) {
            val maxVal = max(144f, (metrics.maxFps + 5).toFloat())
            val minVal = max(30f, (metrics.minFps - 10).toFloat())
            val range = max(1f, maxVal - minVal)

            val path = Path()
            val fillPath = Path()

            history.forEachIndexed { index, fps ->
                val x = graphLeft + (index.toFloat() / max(1, history.size - 1)) * graphWidth
                val normY = ((fps - minVal) / range).coerceIn(0f, 1f)
                val y = graphBottom - (normY * graphHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, graphBottom)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(graphRight, graphBottom)
            fillPath.close()

            graphFillPaint.shader = LinearGradient(
                graphLeft, graphTop, graphLeft, graphBottom,
                Color.argb((90 * alphaMultiplier).toInt(), Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor)),
                Color.argb(0, Color.red(fpsColor), Color.green(fpsColor), Color.blue(fpsColor)),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, graphFillPaint)

            graphLinePaint.strokeWidth = dp(1.8f)
            graphLinePaint.color = fpsColor
            canvas.drawPath(path, graphLinePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchDownTime = SystemClock.elapsedRealtime()
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (abs(dx) > dp(6f) || abs(dy) > dp(6f)) {
                    isDragging = true
                }
                if (isDragging) {
                    layoutParams.x = (initialX + dx).toInt()
                    layoutParams.y = (initialY + dy).toInt()
                    try {
                        windowManager.updateViewLayout(this, layoutParams)
                    } catch (_: Exception) {}
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val touchDuration = SystemClock.elapsedRealtime() - touchDownTime
                val dx = abs(event.rawX - initialTouchX)
                val dy = abs(event.rawY - initialTouchY)

                if (!isDragging && dx < dp(8f) && dy < dp(8f) && touchDuration < 350) {
                    // Check button taps inside expanded mode
                    if (preferences.hudStyle == OverlayHudStyle.EXPANDED_CARD) {
                        val touchX = event.x
                        val touchY = event.y

                        if (closeButtonRect.contains(touchX, touchY)) {
                            onCloseRequested()
                            return true
                        }

                        if (appShortcutRect.contains(touchX, touchY)) {
                            openMainActivity()
                            return true
                        }
                    }

                    // Toggle HUD Mode (Pill <-> Expanded)
                    val nextStyle = if (preferences.hudStyle == OverlayHudStyle.COMPACT_PILL) {
                        OverlayHudStyle.EXPANDED_CARD
                    } else {
                        OverlayHudStyle.COMPACT_PILL
                    }
                    updatePreferences(preferences.copy(hudStyle = nextStyle))
                    return true
                }

                if (isDragging && preferences.snapToEdges) {
                    snapToNearestEdge()
                } else {
                    onSavePosition(layoutParams.x, layoutParams.y)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToNearestEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (layoutParams.x + width / 2 < screenWidth / 2) {
            dp(16f).toInt() // Snap Left
        } else {
            screenWidth - width - dp(16f).toInt() // Snap Right
        }

        val startX = layoutParams.x
        ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                layoutParams.x = animator.animatedValue as Int
                try {
                    windowManager.updateViewLayout(this@FpsFloatingHudView, layoutParams)
                } catch (_: Exception) {}
            }
            start()
        }
        onSavePosition(targetX, layoutParams.y)
    }

    private fun openMainActivity() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
}
