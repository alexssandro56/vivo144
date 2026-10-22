package com.vivo144.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.vivo144.app.core.Engine
import com.vivo144.app.core.Mode
import com.vivo144.app.core.Prefs
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var resume by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Bg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Surface1.toArgb()),
        )
        super.onCreate(savedInstanceState)
        // Opening the app is also a safe moment to (re)start the background service.
        if (Prefs.mode == Mode.AUTO || Prefs.hud) Engine.apply(this)
        setContent { VivoTheme { Root(resume) } }
    }

    override fun onResume() {
        super.onResume()
        resume++
    }
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("Início", Icons.Rounded.Home),
    Tab("Jogos", Icons.Rounded.PlayArrow),
    Tab("Debloat", Icons.Rounded.Delete),
    Tab("Extras", Icons.Rounded.Build),
    Tab("Ajustes", Icons.Rounded.Settings),
)

@Composable
private fun Root(resume: Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack: (String) -> Unit = { msg -> scope.launch { host.currentSnackbarData?.dismiss(); host.showSnackbar(msg) } }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(host) },
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Bg,
                            indicatorColor = Neon,
                            selectedTextColor = Neon,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(resume, snack, goGames = { tab = 1 }, goSettings = { tab = 4 })
                1 -> GamesScreen()
                2 -> DebloatScreen(snack)
                3 -> ToolsScreen(resume, snack)
                else -> SettingsScreen(resume, snack)
            }
        }
    }
}
