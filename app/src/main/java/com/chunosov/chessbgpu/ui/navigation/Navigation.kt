package com.chunosov.chessbgpu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chunosov.chessbgpu.ui.screens.BoardEditorScreen
import com.chunosov.chessbgpu.ui.screens.GameScreen
import com.chunosov.chessbgpu.ui.screens.HomeScreen
import com.chunosov.chessbgpu.ui.screens.SavedGamesScreen
import com.chunosov.chessbgpu.ui.screens.SettingsScreen
import com.chunosov.chessbgpu.viewmodel.ChessViewModel

// Добавляем новый маршрут для экрана редактора доски
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object SavedGames : Screen("saved_games")
    object Settings : Screen("settings")
    object GameReview : Screen("game_review")
    object BoardEditor : Screen("board_editor") // Новый маршрут
}

// В функции AppNavHost добавляем новый composable для экрана редактора доски
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGame = { navController.navigate(Screen.Game.route) },
                onNavigateToSavedGames = { navController.navigate(Screen.SavedGames.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBoardEditor = { navController.navigate(Screen.BoardEditor.route) } // Новая навигация
            )
        }
        
        composable(Screen.Game.route) {
            GameScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SavedGames.route) {
            SavedGamesScreen(
                viewModel = viewModel,
                onNavigateToGameReview = { navController.navigate(Screen.GameReview.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Добавляем composable для экрана редактора доски
        composable(Screen.BoardEditor.route) {
            BoardEditorScreen(
                viewModel = viewModel,
                onStartGame = { navController.navigate(Screen.Game.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 