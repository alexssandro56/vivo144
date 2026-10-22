package com.vivo144.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import com.vivo144.app.service.AutoService
import com.vivo144.app.service.ModeTile
import java.util.concurrent.Executors

/**
 * Puts the system in the state the settings describe:
 * OFF    → release the vote (LTPO back to automatic)
 * ALWAYS → vote [Prefs.fps] now; the vote lives until reboot
 * AUTO   → AutoService votes while a selected game is in front, releases otherwise
 * The service also runs when the HUD is on.
 */
object Engine {
    private val worker = Executors.newSingleThreadExecutor()

    fun apply(c: Context, done: ((String) -> Unit)? = null) {
        val ctx = c.applicationContext
        worker.execute {
            val msg = when (Prefs.mode) {
                Mode.OFF -> if (Rms.release()) "Desligado: tela volta ao automático (LTPO)." else unreachable()
                Mode.ALWAYS -> if (Rms.vote(Prefs.fps)) "${Prefs.fps} Hz fixos até desligar ou reiniciar." else unreachable()
                Mode.AUTO -> "Automático: ${Prefs.fps} Hz só nos jogos escolhidos."
            }
            val wantService = Prefs.mode == Mode.AUTO || Prefs.hud
            val svc = if (wantService) startService(ctx) else { stopService(ctx); null }
            try {
                TileService.requestListeningState(ctx, ComponentName(ctx, ModeTile::class.java))
            } catch (_: Throwable) { }
            done?.invoke(svc ?: msg)
        }
    }

    private fun unreachable() =
        "Não consegui falar com o serviço rms. Inicie o Shizuku e conceda a permissão em Ajustes."

    /** Returns an error message, or null when it started. */
    private fun startService(ctx: Context): String? = try {
        ctx.startForegroundService(Intent(ctx, AutoService::class.java))
        null
    } catch (t: Throwable) {
        "O Android não deixou iniciar o serviço em segundo plano agora. Abra o app uma vez."
    }

    private fun stopService(ctx: Context) {
        ctx.stopService(Intent(ctx, AutoService::class.java))
    }
}
