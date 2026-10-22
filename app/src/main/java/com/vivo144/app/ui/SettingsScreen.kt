package com.vivo144.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vivo144.app.core.Apps
import com.vivo144.app.core.Engine
import com.vivo144.app.core.Prefs
import com.vivo144.app.core.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** vivo / iQOO auto-start managers, tried in order (same list as vivo HiHz). */
private val AUTOSTART = listOf(
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.safeguard.PurviewTabActivity"),
)

private fun Context.open(i: Intent) = try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (_: Throwable) { false }

private fun Context.appDetails() =
    open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

@Composable
fun SettingsScreen(resume: Int, snack: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val shizuku by ShizukuState.status.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    val key = resume + tick
    val usage = remember(key) { Apps.hasUsageAccess(ctx) }
    val overlay = remember(key) { Apps.canOverlay(ctx) }
    val notif = remember(key) { Apps.canNotify(ctx) }
    val battery = remember(key) { Apps.ignoresBattery(ctx) }
    var onBoot by remember { mutableStateOf(Prefs.onBoot) }
    var hud by remember { mutableStateOf(Prefs.hud) }
    var high by remember { mutableStateOf(Prefs.highPriority) }
    var granting by remember { mutableStateOf(false) }

    LaunchedEffect(resume) { ShizukuState.refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Section("Shizuku") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when (shizuku) {
                            ShizukuState.Status.READY -> "Conectado"
                            ShizukuState.Status.NO_PERMISSION -> "Rodando, sem permissão"
                            ShizukuState.Status.NOT_RUNNING -> "Não está rodando"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Usado no debloat, para ler o reason e para liberar tudo num toque. " +
                            "Os 144 Hz funcionam sem ele quando o modo é \"Direto\".",
                        style = MaterialTheme.typography.bodySmall, color = TextDim,
                    )
                }
                when (shizuku) {
                    ShizukuState.Status.READY -> Pill("OK", Neon)
                    ShizukuState.Status.NO_PERMISSION ->
                        Button(onClick = { ShizukuState.request() }) { Text("Permitir") }
                    ShizukuState.Status.NOT_RUNNING -> {
                        val launch = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        if (launch != null) OutlinedButton(onClick = { ctx.open(launch) }) { Text("Abrir") }
                        else Pill("Instale", Danger)
                    }
                }
            }
        }

        Section("Permissões do modo automático") {
            if (shizuku == ShizukuState.Status.READY && !(usage && overlay && notif && battery)) {
                Button(
                    onClick = {
                        granting = true
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { Apps.grantAllWithShizuku(ctx) }
                            granting = false; tick++
                            snack(r.lines().joinToString("  "))
                        }
                    },
                    enabled = !granting,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (granting) "Liberando…" else "Liberar tudo com 1 toque (Shizuku)") }
            }
            PermRow("Acesso ao uso", "Saber qual jogo está aberto", usage) {
                ctx.open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) || ctx.appDetails()
            }
            PermRow("Sobrepor outros apps", "Indicador flutuante de Hz", overlay) {
                ctx.open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }
            PermRow("Notificações", "Aviso do modo automático", notif) { ctx.appDetails() }
            PermRow("Sem otimização de bateria", "Para a vivo não fechar o serviço", battery) {
                // vivo hides the app from the global list; ask for this app directly.
                ctx.open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) ||
                    ctx.open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) || ctx.appDetails()
            }
            PermRow("Início automático (vivo)", "Necessário para reaplicar após reiniciar", null) {
                AUTOSTART.any { ctx.open(Intent().setComponent(it)) } || ctx.appDetails()
            }
        }

        Section("Comportamento") {
            ToggleRow("Reaplicar depois de reiniciar", "O voto da vivo some no reboot", onBoot) {
                onBoot = it; Prefs.onBoot = it
            }
            ToggleRow(
                "Prioridade máxima (teste)",
                "Vota com prioridade 10000 em vez de 100. Veja se \"Quem controla a tela\" passa a mostrar Vivo144 dentro do jogo.",
                high,
            ) {
                high = it; Prefs.highPriority = it
                Engine.apply(ctx, snack)
            }
            ToggleRow("Indicador flutuante de Hz", "Mostra a taxa real por cima dos jogos", hud) {
                if (it && !overlay) {
                    snack("Libere \"Sobrepor outros apps\" primeiro.")
                } else {
                    hud = it; Prefs.hud = it
                    Engine.apply(ctx, snack)
                }
            }
        }

        Section("Sobre") {
            Text(
                "Voto direto no serviço rms da vivo (mesma técnica do vivo HiHz / AndroPlus, MIT). " +
                    "Sem interpolação: os quadros são reais. Adicione o bloco \"144 Hz\" nas Configurações " +
                    "Rápidas para trocar de modo sem abrir o app.",
                style = MaterialTheme.typography.bodySmall, color = TextDim,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermRow(title: String, sub: String, ok: Boolean?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        when (ok) {
            true -> Icon(Icons.Rounded.CheckCircle, null, tint = Neon)
            false -> Icon(Icons.Rounded.Warning, null, tint = Amber)
            null -> Text("Abrir", style = MaterialTheme.typography.labelLarge, color = Cyan)
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
