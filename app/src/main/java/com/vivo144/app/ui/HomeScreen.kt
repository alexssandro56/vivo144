@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.vivo144.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivo144.app.core.Apps
import com.vivo144.app.core.Engine
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs
import com.vivo144.app.core.Rms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(resume: Int, snack: (String) -> Unit, goGames: () -> Unit, goSettings: () -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(Prefs.mode) }
    var fps by remember { mutableIntStateOf(Prefs.fps) }
    val rates = remember { Rms.panelRates(ctx).filter { it >= 60 }.ifEmpty { listOf(144, 120, 90, 60) } }
    var rate by remember { mutableStateOf<Float?>(null) }
    var path by remember { mutableStateOf(Rms.Path.NONE) }
    var reason by remember { mutableStateOf<String?>(null) }
    var diag by remember { mutableStateOf("") }
    val usage = remember(resume) { Apps.hasUsageAccess(ctx) }
    val gameCount = remember(resume) { Prefs.games.size }

    LaunchedEffect(resume) {
        mode = Prefs.mode; fps = Prefs.fps
        while (true) {
            val r = withContext(Dispatchers.IO) { Rms.rate() }
            rate = r; path = Rms.path; diag = Rms.diag
            delay(500)
        }
    }
    LaunchedEffect(resume) {
        while (true) {
            reason = withContext(Dispatchers.IO) { Rms.reason(ctx) }
            delay(3000)
        }
    }

    fun setMode(m: Mode) {
        if (m == Mode.AUTO && !usage) {
            snack("O modo automático precisa do acesso ao uso. Libere em Ajustes.")
            goSettings()
            return
        }
        mode = m
        Prefs.mode = m
        Engine.apply(ctx, snack)
    }

    val maxRate = (rates.maxOrNull() ?: 144).toFloat()
    val live = rate
    val atTarget = live != null && Math.round(live) >= fps
    val color = when {
        live == null -> Danger
        atTarget -> Neon
        else -> Amber
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Vivo 144Hz", style = MaterialTheme.typography.headlineSmall)
                Text("iQOO 15T · sem root", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
            when (path) {
                Rms.Path.DIRECT -> Pill("Direto", Neon)
                Rms.Path.SHIZUKU -> Pill("Shizuku", Cyan)
                Rms.Path.DEX -> Pill("Shell", Cyan)
                Rms.Path.NONE -> Pill("Sem acesso", Danger)
            }
        }

        // Hero gauge
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HzGauge(value = live ?: 0f, max = maxRate, color = color) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (live == null) "--" else Math.round(live).toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = color,
                    )
                    Text("Hz agora", style = MaterialTheme.typography.labelLarge, color = TextDim)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (mode) {
                            Mode.OFF -> "LTPO automático"
                            Mode.ALWAYS -> "Fixo em $fps Hz"
                            Mode.AUTO -> "$fps Hz nos jogos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (path == Rms.Path.NONE && diag.isNotEmpty()) {
            Text(
                "Diagnóstico: $diag",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                textAlign = TextAlign.Center,
            )
            val clip = LocalClipboardManager.current
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = {
                    clip.setText(AnnotatedString("Diagnóstico: $diag\n\n${Rms.report}"))
                    snack("Diagnóstico copiado. Cole na conversa.")
                }) { Text("Copiar diagnóstico completo") }
            }
        }
        reason?.let {
            Text(
                "Quem controla a tela: $it",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                textAlign = TextAlign.Center,
            )
        }

        Section("Modo") {
            val modes = listOf(Mode.OFF to "Desligado", Mode.ALWAYS to "Sempre", Mode.AUTO to "Automático")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { setMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(label, maxLines = 1) }
                }
            }
            Text(
                when (mode) {
                    Mode.OFF -> "A vivo decide a taxa (1–120 Hz). Melhor para bateria."
                    Mode.ALWAYS -> "Tela travada em $fps Hz em tudo, até você desligar. Gasta mais bateria."
                    Mode.AUTO -> "Liga $fps Hz sozinho quando um jogo escolhido abre e muda para a taxa de \"Fora dos jogos\" quando você sai."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
            )
        }

        Section("Taxa alvo") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rates.forEach { r ->
                    FilterChip(
                        selected = fps == r,
                        onClick = {
                            fps = r
                            Prefs.fps = r
                            if (mode != Mode.OFF) Engine.apply(ctx, snack)
                        },
                        label = { Text("$r Hz", fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
        }

        if (reason?.startsWith("reqFps") == true || reason?.startsWith("LayoutParams") == true) {
            Section("") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = Amber)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "O jogo está pedindo a própria taxa pela janela dele (${reason?.substringAfterLast('|')} Hz), " +
                            "e esse pedido vence o voto. Escolha a taxa máxima dentro do jogo ou teste " +
                            "\"Prioridade máxima\" em Ajustes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (mode == Mode.AUTO) {
            val auto by com.vivo144.app.service.AutoService.status.collectAsState()
            Section("Automático agora") {
                Text(auto, style = MaterialTheme.typography.bodyMedium, color = if (auto == "Parado") Danger else Neon)
                if (auto == "Parado") {
                    Text(
                        "O serviço não está rodando. Abra Ajustes e libere \"Sem otimização de bateria\" e \"Início automático\".",
                        style = MaterialTheme.typography.bodySmall, color = TextDim,
                    )
                }
            }
            Section("Fora dos jogos") {
                var idle by remember { mutableIntStateOf(Prefs.idleFps) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(0) + rates.filter { it < fps }).forEach { r ->
                        FilterChip(
                            selected = idle == r,
                            onClick = { idle = r; Prefs.idleFps = r },
                            label = { Text(if (r == 0) "Automático (LTPO)" else "$r Hz", fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }
                Text(
                    if (idle == 0) "Ao sair do jogo, a vivo volta a decidir a taxa (1–120 Hz)."
                    else "Ao sair do jogo, a tela fica fixa em $idle Hz.",
                    style = MaterialTheme.typography.bodySmall, color = TextDim,
                )
            }
            Section("Jogos no automático") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (gameCount == 0) "Nenhum jogo escolhido ainda." else "$gameCount app(s) escolhidos.",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = goGames) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Escolher")
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
