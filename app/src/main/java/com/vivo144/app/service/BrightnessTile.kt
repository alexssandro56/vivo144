package com.vivo144.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vivo144.app.core.Brightness
import com.vivo144.app.core.Util
import com.vivo144.app.ui.MainActivity

/** "Brilho Turbo": bypass on at the saved level (100% if never set) / originals back. */
class BrightnessTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        if (UtilityAccessibilityService.instance == null) { openAppCollapsing(); return }
        val on = !Util.bypass
        Util.bypass = on
        render()
        Thread {
            if (on) Brightness.apply(applicationContext, Util.level) else Brightness.disable(applicationContext)
        }.start()
    }

    private fun render() {
        val t = qsTile ?: return
        t.label = "Brilho Turbo"
        t.state = when {
            UtilityAccessibilityService.instance == null -> Tile.STATE_UNAVAILABLE
            Util.bypass -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        t.subtitle = if (Util.bypass) "${Math.round(Util.level * 100)}%" else "Automático"
        t.updateTile()
    }
}

/** "Botão Power": toggles power_bridge_enabled. */
class PowerBridgeTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        Util.powerBridge = !Util.powerBridge
        render()
    }

    private fun render() {
        val t = qsTile ?: return
        t.label = "Botão Power"
        t.state = if (Util.powerBridge) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.subtitle = when {
            UtilityAccessibilityService.instance == null -> "Serviço inativo"
            Util.powerBridge -> "Menu de energia"
            else -> "Assistente"
        }
        t.updateTile()
    }
}

@Suppress("DEPRECATION")
internal fun TileService.openAppCollapsing() {
    val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= 34) {
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
    } else {
        startActivityAndCollapse(i)
    }
}
