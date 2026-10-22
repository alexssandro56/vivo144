@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.vivo144.app.ui

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vivo144.app.core.Access
import com.vivo144.app.core.Brightness
import com.vivo144.app.core.Lab
import com.vivo144.app.core.SettingsIO
import com.vivo144.app.core.Table
import com.vivo144.app.core.Util
import com.vivo144.app.service.UtilityAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToolsScreen(resume: Int, snack: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // live service / permission state
    var running by remember { mutableStateOf(Access.running()) }
    var enabled by remember { mutableStateOf(Access.enabled(ctx)) }
    var canWrite by remember { mutableStateOf(SettingsIO.canWriteSystem(ctx)) }
    var secure by remember { mutableStateOf(SettingsIO.hasSecure(ctx)) }
    LaunchedEffect(resume) {
        while (true) {
            running = Access.running(); enabled = Access.enabled(ctx)
            canWrite = SettingsIO.canWriteSystem(ctx); secure = SettingsIO.hasSecure(ctx)
            delay(1000)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Extras: brilho e botão Power", style = MaterialTheme.typography.headlineSmall)
        BrightnessCard(running, canWrite, snack)
        PowerCard(running, enabled, secure, snack)
        DiagnosticsCard(snack)
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------- brightness

@Composable
private fun BrightnessCard(running: Boolean, canWrite: Boolean, snack: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bypass by remember { mutableStateOf(Util.bypass) }
    var pct by remember { mutableFloatStateOf(Util.level * 100f) }
    var revert by remember { mutableStateOf(Util.revertOnScreenOff) }
    var stored by remember { mutableStateOf<Int?>(null) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            stored = withContext(Dispatchers.IO) { Brightness.current255(ctx) }
            if (!dragging) bypass = Util.bypass
            delay(1500)
        }
    }
    // Keys follow the slider with a short debounce; the overlay follows instantly.
    LaunchedEffect(pct, bypass) {
        if (!bypass) return@LaunchedEffect
        delay(150)
        withContext(Dispatchers.IO) { Brightness.setLevel(ctx, pct / 100f) }
    }

    fun turn(on: Boolean) {
        bypass = on
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                if (on) Brightness.apply(ctx, pct / 100f) else Brightness.disable(ctx)
            }
            snack(r.text)
        }
    }

    Section("Brilho e bypass do sensor de luz") {
        if (!canWrite) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Falta a permissão \"Alterar configurações do sistema\".", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall, color = Amber)
                TextButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Permitir") }
            }
        }
        if (!running) {
            Text("O bypass por overlay requer o serviço de acessibilidade ativo.",
                style = MaterialTheme.typography.bodySmall, color = Amber)
        }
        ToggleLine("Ativar bypass do sensor de luz (overlay)", null, bypass) { turn(it) }
        Text(
            "Brilho atual: ${pct.toInt()}% (valor gravado: ${stored ?: "?"} de 255)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = pct,
            onValueChange = {
                dragging = true
                pct = it
                if (bypass) UtilityAccessibilityService.instance?.showOverlay(it / 100f)
            },
            onValueChangeFinished = { dragging = false; Util.level = pct / 100f },
            valueRange = 1f..100f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pct = 100f; turn(true) }, modifier = Modifier.weight(1f)) { Text("Brilho máximo (100%)") }
            OutlinedButton(onClick = {
                bypass = false
                scope.launch { snack(withContext(Dispatchers.IO) { Brightness.restoreEverything(ctx) }.text) }
            }, modifier = Modifier.weight(1f)) { Text("Restaurar originais") }
        }
        ToggleLine("Reverter ao apagar a tela", null, revert) { revert = it; Util.revertOnScreenOff = it }
        Hint(
            "100% é o teto normal do painel (~770 nits). O teto de alto brilho (~1722 nits, HBM) é controlado " +
                "pela vivo e só será alcançado se algum experimento do Laboratório funcionar."
        )
        Hint("Brilho máximo contínuo gera calor e desgasta a tela. Use com moderação.")
    }
}

// ---------------------------------------------------------------- power

@Composable
private fun PowerCard(running: Boolean, enabled: Boolean, secure: Boolean, snack: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bridge by remember { mutableStateOf(Util.powerBridge) }
    var watched by remember { mutableStateOf(Util.watched.joinToString(", ")) }
    val until by UtilityAccessibilityService.discoveryUntil.collectAsState()
    val found by UtilityAccessibilityService.discovered.collectAsState()
    var nowMs by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(until) { while (until > 0) { nowMs = android.os.SystemClock.elapsedRealtime(); delay(500) } }
    val discovering = until > nowMs

    fun open(i: Intent) = try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Throwable) { }

    Section("Botão Power → menu de energia") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Serviço de acessibilidade", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            when {
                running -> Pill("Ativo", Neon)
                enabled -> Pill("Ligado, sem conexão", Amber)
                else -> Pill("Inativo", Danger)
            }
        }
        ToggleLine("Abrir menu de energia ao segurar o Power (0,5 s)", "Com isto ligado, o assistente não abre pelo Power.", bridge) {
            bridge = it; Util.powerBridge = it
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Acessibilidade") }
            if (!running) {
                Button(onClick = {
                    scope.launch { snack(withContext(Dispatchers.IO) { Access.enable(ctx) }) }
                }) { Text("Ativar automaticamente") }
            }
            OutlinedButton(onClick = {
                open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
            }) { Text("Informações do app") }
        }
        if (!secure) {
            Hint("\"Ativar automaticamente\" usa o Shizuku ou a permissão WRITE_SECURE_SETTINGS (veja Ajustes).")
        }
        Hint("Se o serviço aparecer bloqueado: tente ativá-lo uma vez; depois, em Informações do app → menu ⋮ → Permitir configurações restritas.")
        Hint("Mantenha ligada, no assistente da vivo, a opção que abre o assistente ao segurar o Power: é ela que dispara este módulo.")

        HorizontalDivider()
        Text("Pacotes vigiados", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = watched,
            onValueChange = { watched = it },
            singleLine = false,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Separe por vírgula. Padrão: com.vivo.agent") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                Util.watched = watched.split(',', '\n', ' ').map { it.trim() }.filter { it.isNotEmpty() }
                watched = Util.watched.joinToString(", ")
                snack("Lista salva.")
            }) { Text("Salvar lista") }
            Button(
                onClick = { UtilityAccessibilityService.instance?.startDiscovery() ?: snack("Ative o serviço primeiro.") },
                enabled = running && !discovering,
            ) { Text(if (discovering) "Descobrindo… ${((until - nowMs) / 1000).coerceAtLeast(0)}s" else "Modo descoberta (30 s)") }
        }
        if (discovering || found.isNotEmpty()) {
            Hint("Segure o Power por 0,5 s agora. Toque num pacote abaixo para vigiá-lo.")
            found.reversed().forEach { (line, pkg) ->
                Text(
                    line,
                    Modifier.fillMaxWidth().clickable {
                        Util.watched = Util.watched + pkg
                        watched = Util.watched.joinToString(", ")
                        snack("$pkg adicionado à lista vigiada.")
                    }.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                    color = if (pkg in Util.watched) Neon else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Hint("Para o serviço não ser encerrado: permita início automático e trave o app na multitarefa.")
    }
}

// ---------------------------------------------------------------- diagnostics + lab

@Composable
private fun DiagnosticsCard(snack: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Section("") {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("Diagnóstico e experimentos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null)
        }
        if (open) DiagnosticsBody(snack)
    }
}

@Composable
private fun DiagnosticsBody(snack: (String) -> Unit) {
    val ctx = LocalContext.current
    var hdr by remember { mutableStateOf(Util.hdr) }
    var full by remember { mutableStateOf(Util.fullOverlay) }
    var close by remember { mutableStateOf(Util.closeAction) }
    val triggers by UtilityAccessibilityService.triggers.collectAsState()

    Text("Ação de fechamento do assistente", style = MaterialTheme.typography.labelLarge, color = TextDim)
    val opts = Util.Close.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opts.forEachIndexed { i, o ->
            SegmentedButton(
                selected = close == o,
                onClick = { close = o; Util.closeAction = o },
                shape = SegmentedButtonDefaults.itemShape(i, opts.size),
            ) { Text(if (o == Util.Close.NENHUMA) "Nenhuma" else o.name) }
        }
    }
    ToggleLine("HDR no overlay (experimental)", "Provavelmente sem efeito: o HBM do Android está desligado neste aparelho.", hdr) {
        hdr = it; Util.hdr = it; UtilityAccessibilityService.instance?.rebuildOverlay()
    }
    ToggleLine("Overlay em tela inteira (teste)", "Padrão 1×1 px. Tela inteira só para teste e pode gastar mais bateria.", full) {
        full = it; Util.fullOverlay = it; UtilityAccessibilityService.instance?.rebuildOverlay()
    }

    Text("Últimos gatilhos do Power", style = MaterialTheme.typography.labelLarge, color = TextDim)
    if (triggers.isEmpty()) Hint("Nenhum ainda.")
    triggers.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }

    HorizontalDivider()
    LabSection(snack)
}

@Composable
private fun LabSection(snack: (String) -> Unit) {
    val ctx = LocalContext.current
    val values = remember { mutableStateMapOf<String, String>() }
    val log = remember { mutableStateListOf<String>() }
    var revertSecs by remember { mutableStateOf(Util.labRevertSeconds.toString()) }
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    fun push(s: String) { log.add(0, "${fmt.format(Date())}  $s"); while (log.size > 60) log.removeAt(log.size - 1) }

    // Live values only while this card is open.
    DisposableEffect(Unit) {
        val h = Handler(Looper.getMainLooper())
        val io = java.util.concurrent.Executors.newSingleThreadExecutor()
        val observers = Lab.KEYS.map { k ->
            object : ContentObserver(h) {
                override fun onChange(selfChange: Boolean) {
                    io.execute {
                        val v = Lab.read(ctx, k) ?: "(vazio)"
                        h.post {
                            val old = values[k.key]
                            if (old != v) { values[k.key] = v; push("${k.key}: $old → $v") }
                        }
                    }
                }
            }.also { ctx.contentResolver.registerContentObserver(SettingsIO.uri(k.table, k.key), false, it) }
        }
        io.execute {
            val init = (Lab.KEYS.map { it.key to (Lab.read(ctx, it) ?: "(vazio)") } +
                Lab.READ_ONLY.map { it to (SettingsIO.get(ctx, Table.SYSTEM, it) ?: "(vazio)") })
            h.post { init.forEach { (k, v) -> values[k] = v } }
        }
        onDispose {
            observers.forEach { ctx.contentResolver.unregisterContentObserver(it) }
            io.shutdown()
        }
    }

    Text("Laboratório de chaves", style = MaterialTheme.typography.titleMedium)
    Hint("Só as chaves abaixo podem ser gravadas. Cada teste salva o original, grava, lê de volta e volta sozinho.")
    Lab.READ_ONLY.forEach {
        Text("$it = ${values[it] ?: "…"}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = TextDim)
    }
    OutlinedTextField(
        value = revertSecs,
        onValueChange = { v -> revertSecs = v.filter { it.isDigit() }.take(4); revertSecs.toIntOrNull()?.let { Util.labRevertSeconds = it } },
        label = { Text("Reverter após (s, 0 = não reverter)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    Lab.KEYS.forEach { k -> LabKeyRow(k, values[k.key], snack = { push(it); snack(it) }) }

    OutlinedButton(onClick = { Lab.restoreAll(ctx) { m -> Handler(Looper.getMainLooper()).post { push(m) } } }, modifier = Modifier.fillMaxWidth()) {
        Text("Restaurar tudo (laboratório)")
    }
    Text("Log de mudanças", style = MaterialTheme.typography.labelLarge, color = TextDim)
    if (log.isEmpty()) Hint("Nenhuma mudança vista ainda.")
    log.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun LabKeyRow(k: Lab.Key, value: String?, snack: (String) -> Unit) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val writable = Lab.writable(ctx, k)
    val post: (String) -> Unit = { m -> Handler(Looper.getMainLooper()).post { snack(m) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${k.table.shell}/${k.key}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "atual: ${value ?: "…"} · original observado: ${k.observed} · ${k.type}" + if (!writable) " · somente leitura" else "",
            style = MaterialTheme.typography.bodySmall, color = TextDim,
        )
        if (writable) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it.trim() }, singleLine = true,
                    placeholder = { Text("valor") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                FilledTonalButton(onClick = { Lab.test(ctx, k, input, post) }, enabled = input.isNotEmpty()) { Text("Testar") }
                TextButton(onClick = { Lab.restore(ctx, k, post) }) { Text("Restaurar") }
            }
        }
    }
}

// ---------------------------------------------------------------- small pieces

@Composable
private fun ToggleLine(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = TextDim)
}
