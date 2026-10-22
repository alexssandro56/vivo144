package com.vivo144.app.core

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

data class AppEntry(val pkg: String, val label: String, val isGame: Boolean)

object Apps {
    /** Well-known games that don't always declare the game category. */
    private val KNOWN_GAMES = setOf(
        "com.activision.callofduty.shooter", "com.activision.callofduty.warzone",
        "com.tencent.tmgp.cod", "com.supercell.brawlstars", "com.supercell.clashroyale",
        "com.supercell.clashofclans", "com.tencent.ig", "com.pubg.imobile", "com.pubg.krmobile",
        "com.tencent.tmgp.pubgmhd", "com.dts.freefireth", "com.dts.freefiremax",
        "com.miHoYo.GenshinImpact", "com.HoYoverse.hkrpgoversea", "com.HoYoverse.Nap",
        "com.mobile.legends", "com.riotgames.league.wildrift", "com.tencent.tmgp.sgame",
        "com.levelinfinite.sgameGlobal", "com.ea.gp.fifamobile", "com.netease.newspike",
        "com.garena.game.codm", "com.mojang.minecraftpe", "com.roblox.client",
        "com.epicgames.fortnite", "com.kurogame.wutheringwaves.global", "com.proximabeta.mf.uamo"
    )

    fun launchable(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ctx.packageName }
            .map { ai ->
                @Suppress("DEPRECATION")
                val game = ai.category == ApplicationInfo.CATEGORY_GAME ||
                    (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                    ai.packageName in KNOWN_GAMES
                AppEntry(ai.packageName, ai.loadLabel(pm).toString(), game)
            }
            .sortedWith(compareByDescending<AppEntry> { it.isGame }.thenBy { it.label.lowercase() })
    }

    /** First run: pre-select every game found. */
    fun seedGames(ctx: Context) {
        if (Prefs.gamesSeeded) return
        Prefs.games = launchable(ctx).filter { it.isGame }.map { it.pkg }.toSet()
        Prefs.gamesSeeded = true
    }

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun canOverlay(ctx: Context) = Settings.canDrawOverlays(ctx)

    fun ignoresBattery(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun canNotify(ctx: Context) =
        ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * One tap with Shizuku: grants everything the automatic mode needs instead
     * of sending the user through four settings screens.
     */
    fun grantAllWithShizuku(ctx: Context): String {
        val p = ctx.packageName
        val steps = listOf(
            "Acesso ao uso" to "appops set $p GET_USAGE_STATS allow",
            "Sobrepor outros apps" to "appops set $p SYSTEM_ALERT_WINDOW allow",
            "Notificações" to "pm grant $p android.permission.POST_NOTIFICATIONS",
            "Sem otimização de bateria" to "cmd deviceidle whitelist +$p",
            "Rodar em segundo plano" to "appops set $p RUN_ANY_IN_BACKGROUND allow",
            "Alterar configurações do sistema" to "appops set $p WRITE_SETTINGS allow",
            "Configurações seguras" to "pm grant $p android.permission.WRITE_SECURE_SETTINGS",
        )
        if (!Shell.ready()) return "Shizuku não está pronto."
        val lines = steps.map { (name, cmd) ->
            val r = Shell.exec(cmd)
            (if (r.ok) "✓ " else "✗ ") + name + if (r.ok) "" else " — ${r.all}"
        }
        val acc = if (Access.enabled(ctx)) "✓ Acessibilidade" else "• " + Access.enable(ctx)
        return (lines + acc).joinToString("\n")
    }
}
