package com.vivo144.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vivo144.app.core.AppEntry
import com.vivo144.app.core.Apps
import com.vivo144.app.core.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GamesScreen() {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var selected by remember { mutableStateOf(Prefs.games) }
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            Apps.seedGames(ctx)
            Apps.launchable(ctx)
        }
        selected = Prefs.games
    }

    fun toggle(pkg: String, on: Boolean) {
        selected = if (on) selected + pkg else selected - pkg
        Prefs.games = selected
    }

    val list = apps
    val shown = list?.filter { a ->
        (showAll || a.isGame || a.pkg in selected) &&
            (query.isBlank() || a.label.contains(query, true) || a.pkg.contains(query, true))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Jogos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "No modo Automático, a taxa alvo liga sozinha quando um destes apps abre.",
                style = MaterialTheme.typography.bodySmall, color = TextDim,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Buscar app") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${selected.size} escolhido(s)",
                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Neon,
                )
                FilterChip(selected = showAll, onClick = { showAll = !showAll }, label = { Text("Mostrar todos os apps") })
            }
        }
        if (shown == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            if (shown.isEmpty()) {
                item {
                    Text(
                        "Nenhum jogo detectado. Toque em \"Mostrar todos os apps\" e marque os seus.",
                        style = MaterialTheme.typography.bodyMedium, color = TextDim,
                    )
                }
            }
            items(shown, key = { it.pkg }) { a ->
                AppRow(a, a.pkg in selected) { toggle(a.pkg, it) }
            }
        }
    }
}

@Composable
private fun AppRow(a: AppEntry, checked: Boolean, onChange: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, a.pkg) {
        value = withContext(Dispatchers.IO) {
            try { ctx.packageManager.getApplicationIcon(a.pkg).toImage() } catch (_: Throwable) { null }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Surface2)) {
            icon?.let { Image(it, null, Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (a.isGame) "Jogo · " else "") + a.pkg,
                style = MaterialTheme.typography.bodySmall, color = TextDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
