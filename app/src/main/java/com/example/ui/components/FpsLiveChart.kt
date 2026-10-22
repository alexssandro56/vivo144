package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FpsLiveChart(
    fpsHistory: List<Int>,
    targetFps: Int = 144,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1015))
            .padding(12.dp)
            .testTag("fps_live_chart")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Y-Axis target indicator header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TARGET: ${targetFps} FPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFA855F7)
                )
                Text(
                    text = "REALTIME FRAME PACING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Line Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0 || fpsHistory.isEmpty()) return@Canvas

                val minScaleFps = 50f
                val maxScaleFps = (targetFps.toFloat() + 10f).coerceAtLeast(150f)
                val range = maxScaleFps - minScaleFps

                // Draw Grid Guidelines (60, 90, 120, 144)
                val guidelineFps = listOf(60, 90, 120, 144).filter { it <= maxScaleFps }
                guidelineFps.forEach { fps ->
                    val y = height - ((fps - minScaleFps) / range) * height
                    drawLine(
                        color = if (fps == targetFps) Color(0x40A855F7) else Color(0x203E4254),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = if (fps == targetFps) 1.5f else 1f
                    )
                }

                val points = fpsHistory.mapIndexed { index, fps ->
                    val x = if (fpsHistory.size > 1) {
                        index * (width / (fpsHistory.size - 1))
                    } else {
                        width / 2f
                    }
                    val normalizedY = ((fps.toFloat() - minScaleFps) / range).coerceIn(0f, 1f)
                    val y = height - (normalizedY * height)
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    // Filled Area under graph
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x35A855F7),
                                Color(0x05A855F7),
                                Color.Transparent
                            )
                        )
                    )

                    // Line Graph Stroke
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2
                            val midY = (prev.y + curr.y) / 2
                            quadraticBezierTo(prev.x, prev.y, midX, midY)
                            if (i == points.size - 1) {
                                lineTo(curr.x, curr.y)
                            }
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = Color(0xFFA855F7),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw end pulsing point
                    val lastPoint = points.last()
                    drawCircle(
                        color = Color(0x40A855F7),
                        radius = 8.dp.toPx(),
                        center = lastPoint
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = lastPoint
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-10s",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
                Text(
                    text = "-5s",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
                Text(
                    text = "Live",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA855F7),
                    fontSize = 10.sp
                )
            }
        }
    }
}
