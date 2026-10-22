package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String = "",
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Performance : Screen(
        route = "performance",
        title = "Performance",
        selectedIcon = Icons.Filled.Speed,
        unselectedIcon = Icons.Outlined.Speed
    )

    data object GameProfiles : Screen(
        route = "game_profiles",
        title = "Games",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    )

    data object Advanced : Screen(
        route = "advanced",
        title = "Advanced",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )

    data object Diagnostics : Screen(
        route = "diagnostics",
        title = "Diagnostics",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    )

    data object Adb : Screen(
        route = "adb",
        title = "ADB Setup"
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings"
    )

    data object About : Screen(
        route = "about",
        title = "About"
    )

    companion object {
        val bottomNavScreens: List<Screen>
            get() = listOf(Home, Performance, GameProfiles, Advanced, Diagnostics)
    }
}
