package com.rapidocr.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rapidocr.app.ui.history.HistoryScreen
import com.rapidocr.app.ui.home.HomeScreen
import com.rapidocr.app.ui.result.ResultScreen
import com.rapidocr.app.ui.settings.SettingsScreen
import com.rapidocr.app.viewmodel.HistoryViewModel
import com.rapidocr.app.viewmodel.OcrViewModel
import com.rapidocr.app.viewmodel.OcrUiState
import com.rapidocr.app.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainNavigation(
    ocrViewModel: OcrViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(Screen.Home, Screen.History, Screen.Settings)

    val showBottomBar = currentDestination?.route in listOf("home", "history", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToResult = { navController.navigate("result") },
                    onNavigateToCamera = { navController.navigate("camera") },
                    viewModel = ocrViewModel
                )
            }
            composable("camera") {
                com.rapidocr.app.ui.camera.CameraScreen(
                    onPhotoCaptured = { bitmap ->
                        ocrViewModel.recognize(bitmap)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("result") {
                val state = ocrViewModel.uiState.value
                if (state is OcrUiState.Success) {
                    ResultScreen(
                        result = state.result,
                        bitmap = ocrViewModel.getCurrentBitmap(),
                        text = state.result.fullText,
                        onBack = {
                            ocrViewModel.reset()
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable("history") {
                HistoryScreen(
                    onRecordClick = { },
                    viewModel = historyViewModel
                )
            }
            composable("settings") {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
