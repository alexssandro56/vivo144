package com.vivo144.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vivo144.app.core.Engine
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs

/** Votes live in system_server memory only; put the chosen mode back after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.onBoot || (Prefs.mode == Mode.OFF && !Prefs.hud)) return
        val pr = goAsync()
        Engine.apply(context) { pr.finish() }
    }
}
