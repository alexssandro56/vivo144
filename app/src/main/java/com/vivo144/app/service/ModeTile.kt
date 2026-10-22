package com.vivo144.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vivo144.app.core.Engine
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs
import com.example.MainActivity

/** Quick Settings tile: Desligado → Sempre → Automático → Desligado. */
class ModeTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        Prefs.mode = when (Prefs.mode) {
            Mode.OFF -> Mode.ALWAYS
            Mode.ALWAYS -> Mode.AUTO
            Mode.AUTO -> Mode.OFF
        }
        render()
        Engine.apply(this) { msg ->
            if (msg.startsWith("Não consegui") || msg.startsWith("O Android")) openApp()
        }
    }

    private fun render() {
        val t = qsTile ?: return
        t.label = "${Prefs.fps} Hz"
        t.state = if (Prefs.mode == Mode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        t.subtitle = when (Prefs.mode) {
            Mode.OFF -> "Desligado"
            Mode.ALWAYS -> "Sempre"
            Mode.AUTO -> "Automático"
        }
        t.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(i)
        }
    }
}
