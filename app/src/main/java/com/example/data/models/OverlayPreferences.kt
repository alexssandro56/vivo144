package com.example.data.models

enum class OverlayHudStyle(val displayName: String, val description: String) {
    COMPACT_PILL(
        displayName = "Gaming Pill",
        description = "Sleek compact badge showing live FPS & Hz"
    ),
    EXPANDED_CARD(
        displayName = "Diagnostic Card",
        description = "Full HUD with live graph, frame times, 1% lows & temp"
    ),
    MINIMAL_BADGE(
        displayName = "Minimal Numeric",
        description = "Tiny floating number with low screen footprint"
    ),
    GRAPH_ONLY(
        displayName = "Sparkline Graph",
        description = "Real-time scrolling frame stability graph"
    )
}

data class OverlayPreferences(
    val isOverlayEnabled: Boolean = false,
    val hudStyle: OverlayHudStyle = OverlayHudStyle.COMPACT_PILL,
    val showFrameTime: Boolean = true,
    val showMinFps: Boolean = true,
    val showRefreshRate: Boolean = true,
    val showBatteryTemp: Boolean = true,
    val showCpuUsage: Boolean = true,
    val opacityPercent: Int = 90,
    val autoLaunchOnGames: Boolean = true,
    val snapToEdges: Boolean = true,
    val accentColorHex: Long = 0xFFA855F7,
    val posX: Int = 50,
    val posY: Int = 180
) {
    val opacityFloat: Float
        get() = (opacityPercent / 100f).coerceIn(0.2f, 1.0f)

    companion object {
        val DEFAULT = OverlayPreferences()
    }
}
