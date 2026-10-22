package com.example.ui.vivo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.VivoTopAppBar
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs
import kotlinx.coroutines.launch

/** Routes of the vivo144 engine screens inside the merged app. */
object VivoRoutes {
    const val ENGINE = "vivo_engine"
    const val DEBLOAT = "vivo_debloat"
    const val EXTRAS = "vivo_extras"
    const val ACCESS = "vivo_access"
}

/** Top bar + snackbar + resume counter around a vivo144 screen. */
@Composable
fun VivoPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable (resume: Int, snack: (String) -> Unit) -> Unit,
) {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack: (String) -> Unit = { m -> scope.launch { host.currentSnackbarData?.dismiss(); host.showSnackbar(m) } }
    var resume by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resume++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    Scaffold(
        topBar = { VivoTopAppBar(title = title, subtitle = subtitle, canNavigateBack = true, onNavigateBack = onBack) },
        snackbarHost = { SnackbarHost(host) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) { content(resume, snack) }
    }
}

/** Entry card shown on Home and Advanced. */
@Composable
fun VivoEngineCard(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val mode = remember { mutableStateOf(Prefs.mode) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17181D)),
        border = BorderStroke(1.dp, Color(0xFF2E313D)),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "MOTOR VIVO 144HZ (VOTO NO RMS)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val modeText = when (mode.value) {
                Mode.OFF -> "Desligado (LTPO da vivo)"
                Mode.ALWAYS -> "Sempre em ${Prefs.fps} Hz"
                Mode.AUTO -> "Automático por jogo"
            }
            EngineRow(Icons.Filled.AutoMode, "Modo automático e 144 Hz", modeText) { onOpen(VivoRoutes.ENGINE) }
            HorizontalDivider(color = Color(0xFF2E313D))
            EngineRow(Icons.Filled.CleaningServices, "Debloat CN", "Remover apps chineses com segurança (Shizuku)") { onOpen(VivoRoutes.DEBLOAT) }
            HorizontalDivider(color = Color(0xFF2E313D))
            EngineRow(Icons.Filled.BrightnessHigh, "Brilho e botão Power", "Bypass do sensor de luz, menu de energia, laboratório") { onOpen(VivoRoutes.EXTRAS) }
            HorizontalDivider(color = Color(0xFF2E313D))
            EngineRow(Icons.Filled.AdminPanelSettings, "Shizuku e permissões", "Liberar tudo com 1 toque, início automático") { onOpen(VivoRoutes.ACCESS) }
        }
    }
}

@Composable
private fun EngineRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
