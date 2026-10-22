package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.AppContainer
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.adb.AdbScreen
import com.example.ui.screens.adb.AdbViewModel
import com.example.ui.screens.advanced.AdvancedScreen
import com.example.ui.screens.advanced.AdvancedViewModel
import com.example.ui.screens.diagnostics.DiagnosticsScreen
import com.example.ui.screens.diagnostics.DiagnosticsViewModel
import com.example.ui.screens.games.GameProfilesScreen
import com.example.ui.screens.games.GameProfilesViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.performance.PerformanceScreen
import com.example.ui.screens.performance.PerformanceViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Performance.route,
        Screen.GameProfiles.route,
        Screen.Advanced.route,
        Screen.Diagnostics.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    Screen.bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val icon = if (selected) screen.selectedIcon else screen.unselectedIcon

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("bottom_nav_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        fpsUnlockRepository = appContainer.fpsUnlockRepository,
                        unlockFpsUseCase = appContainer.unlockFpsUseCase,
                        restoreDefaultsUseCase = appContainer.restoreDefaultsUseCase,
                        batteryRepository = appContainer.batteryRepository,
                        thermalRepository = appContainer.thermalRepository,
                        compatibilityRepository = appContainer.compatibilityRepository,
                        systemPerformanceRepository = appContainer.systemPerformanceRepository,
                        performanceProfileRepository = appContainer.performanceProfileRepository,
                        groundedCompatibilityRepository = appContainer.groundedCompatibilityRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAdb = { navController.navigate(Screen.Adb.route) },
                    onNavigateToFpsConfig = { navController.navigate(Screen.Advanced.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                    onNavigateToPerformance = { navController.navigate(Screen.Performance.route) },
                    onNavigateToGameProfiles = { navController.navigate(Screen.GameProfiles.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onOpenVivo = { navController.navigate(it) }
                )
            }

            composable(Screen.Performance.route) {
                val performanceViewModel: PerformanceViewModel = viewModel(
                    factory = PerformanceViewModel.provideFactory(
                        systemPerformanceRepository = appContainer.systemPerformanceRepository,
                        batteryRepository = appContainer.batteryRepository,
                        thermalRepository = appContainer.thermalRepository,
                        performanceProfileRepository = appContainer.performanceProfileRepository,
                        fpsUnlockRepository = appContainer.fpsUnlockRepository,
                        userPreferencesRepository = appContainer.userPreferencesRepository
                    )
                )
                PerformanceScreen(
                    viewModel = performanceViewModel
                )
            }

            composable(Screen.GameProfiles.route) {
                val gameProfilesViewModel: GameProfilesViewModel = viewModel(
                    factory = GameProfilesViewModel.provideFactory(
                        gameProfileRepository = appContainer.gameProfileRepository,
                        userPreferencesRepository = appContainer.userPreferencesRepository
                    )
                )
                GameProfilesScreen(
                    viewModel = gameProfilesViewModel
                )
            }

            composable(Screen.Advanced.route) {
                val advancedViewModel: AdvancedViewModel = viewModel(
                    factory = AdvancedViewModel.provideFactory(
                        fpsUnlockRepository = appContainer.fpsUnlockRepository,
                        unlockFpsUseCase = appContainer.unlockFpsUseCase,
                        restoreDefaultsUseCase = appContainer.restoreDefaultsUseCase,
                        performanceProfileRepository = appContainer.performanceProfileRepository,
                        shizukuDetector = appContainer.shizukuDetector,
                        checkAdbConnectionUseCase = appContainer.checkAdbConnectionUseCase
                    )
                )
                AdvancedScreen(
                    viewModel = advancedViewModel,
                    onNavigateToAdb = { navController.navigate(Screen.Adb.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenVivo = { navController.navigate(it) }
                )
            }

            // ---- vivo144 engine screens ----
            composable(com.example.ui.vivo.VivoRoutes.ENGINE) {
                com.example.ui.vivo.VivoPage("Motor 144 Hz", "Modo automático, taxa e status do rms", { navController.popBackStack() }) { resume, snack ->
                    com.vivo144.app.ui.HomeScreen(
                        resume, snack,
                        goGames = { navController.navigate(Screen.GameProfiles.route) },
                        goSettings = { navController.navigate(com.example.ui.vivo.VivoRoutes.ACCESS) },
                    )
                }
            }
            composable(com.example.ui.vivo.VivoRoutes.DEBLOAT) {
                com.example.ui.vivo.VivoPage("Debloat CN", "Remoção por usuário, restaurável", { navController.popBackStack() }) { _, snack ->
                    com.vivo144.app.ui.DebloatScreen(snack)
                }
            }
            composable(com.example.ui.vivo.VivoRoutes.EXTRAS) {
                com.example.ui.vivo.VivoPage("Brilho e botão Power", "Acessibilidade, overlay e laboratório", { navController.popBackStack() }) { resume, snack ->
                    com.vivo144.app.ui.ToolsScreen(resume, snack)
                }
            }
            composable(com.example.ui.vivo.VivoRoutes.ACCESS) {
                com.example.ui.vivo.VivoPage("Shizuku e permissões", "Liberar tudo com 1 toque", { navController.popBackStack() }) { resume, snack ->
                    com.vivo144.app.ui.SettingsScreen(resume, snack)
                }
            }

            composable(Screen.Diagnostics.route) {
                val diagnosticsViewModel: DiagnosticsViewModel = viewModel(
                    factory = DiagnosticsViewModel.provideFactory(
                        getDeviceDiagnosticsUseCase = appContainer.getDeviceDiagnosticsUseCase,
                        compatibilityRepository = appContainer.compatibilityRepository,
                        groundedCompatibilityRepository = appContainer.groundedCompatibilityRepository
                    )
                )
                DiagnosticsScreen(
                    viewModel = diagnosticsViewModel
                )
            }

            composable(Screen.Adb.route) {
                val adbViewModel: AdbViewModel = viewModel(
                    factory = AdbViewModel.provideFactory(
                        checkAdbConnectionUseCase = appContainer.checkAdbConnectionUseCase,
                        adbManager = appContainer.adbManager
                    )
                )
                AdbScreen(
                    viewModel = adbViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        userPreferencesRepository = appContainer.userPreferencesRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
