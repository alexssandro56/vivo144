package com.vivo144.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vivo144.app.core.Debloat
import com.vivo144.app.core.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DebloatScreen(snack: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val shizuku by ShizukuState.status.collectAsState()
    var states by remember { mutableStateOf<Map<String, Debloat.State>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Debloat.Bloat?>(null) }
    var custom by remember { mutableStateOf("") }

    suspend fun reload() {
        states = withContext(Dispatchers.IO) { Debloat.states() }
    }

    fun run(block: () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            val msg = withContext(Dispatchers.IO) { block() }
            reload()
            busy = false
            snack(msg)
        }
    }

    LaunchedEffect(shizuku) { if (shizuku == ShizukuState.Status.READY) reload() }

    val ready = shizuku == ShizukuState.Status.READY
    val present = Debloat.LIST.count { states[it.pkg] == Debloat.State.INSTALLED }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Debloat CN", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Remove só para o seu usuário, dá para restaurar quando quiser. Apps essenciais " +
                    "(gamewatch, pem, launcher, System UI, teclado atual) são sempre recusados.",
                style = MaterialTheme.typography.bodySmall, color = TextDim,
            )
            Spacer(Modifier.height(12.dp))
            if (!ready) {
                Section("") {
                    Text("Precisa do Shizuku. Veja a aba Ajustes.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { confirmAll = true },
                        enabled = !busy && present > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (present > 0) "Remover $present de uma vez" else "Tudo limpo")
                    }
                    FilledTonalIconButton(onClick = { scope.launch { reload() } }, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, null)
                    }
                }
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }

        items(Debloat.LIST, key = { it.pkg }) { b ->
            val st = states[b.pkg] ?: Debloat.State.UNKNOWN
            Section("") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(b.pkg, style = MaterialTheme.typography.bodySmall, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(b.desc, style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                    when (st) {
                        Debloat.State.INSTALLED -> Pill("Instalado", Amber)
                        Debloat.State.DISABLED -> Pill("Desativado", Cyan)
                        Debloat.State.REMOVED -> Pill("Removido", Neon)
                        Debloat.State.ABSENT -> Pill("Não existe", TextDim)
                        Debloat.State.UNKNOWN -> Pill("?", TextDim)
                    }
                }
                if (ready && st != Debloat.State.ABSENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (st == Debloat.State.INSTALLED) {
                            OutlinedButton(onClick = { confirmOne = b }, enabled = !busy) { Text("Remover") }
                        } else {
                            OutlinedButton(onClick = { run { Debloat.restore(b.pkg) } }, enabled = !busy) { Text("Restaurar") }
                        }
                    }
                }
            }
        }

        if (ready) item {
            Section("Outro pacote") {
                OutlinedTextField(
                    value = custom, onValueChange = { custom = it.trim() },
                    placeholder = { Text("com.exemplo.app") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { run { Debloat.remove(custom) } }, enabled = !busy && custom.isNotEmpty()) { Text("Remover") }
                    OutlinedButton(onClick = { run { Debloat.restore(custom) } }, enabled = !busy && custom.isNotEmpty()) { Text("Restaurar") }
                }
            }
        }
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Remover $present apps?") },
            text = { Text("Todos os apps da lista que ainda estão instalados. Cada um pode ser restaurado depois.") },
            confirmButton = { TextButton(onClick = { confirmAll = false; run { Debloat.removeAllPresent() } }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancelar") } },
        )
    }
    confirmOne?.let { b ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text("Remover ${b.name}?") },
            text = { Text(b.pkg + "\n\nDá para restaurar depois.") },
            confirmButton = { TextButton(onClick = { confirmOne = null; run { Debloat.remove(b.pkg) } }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text("Cancelar") } },
        )
    }
}
