package com.chunosov.chessbgpu.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chunosov.chessbgpu.ui.screens.*
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import com.chunosov.chessbgpu.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object GameOptions : Screen("game_options")
    object Game : Screen("game")
    object Analysis : Screen("analysis")
    object Tasks : Screen("tasks")
    object PuzzleSolver : Screen("puzzle_solver")
    object Learning : Screen("learning")
    object Settings : Screen("settings")
    object SavedGames : Screen("saved_games")
    object GameReview : Screen("game_review")
    object BoardEditor : Screen("board_editor")
}

@Composable
fun ChessNavGraph(
    navController: NavHostController,
    viewModel: ChessViewModel
) {
    // Создаем экземпляр SettingsViewModel
    val settingsViewModel: SettingsViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onStartGame = { navController.navigate(Screen.GameOptions.route) },
                onAnalysis = { 
                    // Загружаем сохраненные игры перед переходом на экран анализа
                    viewModel.loadSavedGames()
                    navController.navigate(Screen.Analysis.route) 
                },
                onTasks = { navController.navigate(Screen.Tasks.route) },
                onLearning = { navController.navigate(Screen.Learning.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onExit = { android.os.Process.killProcess(android.os.Process.myPid()) },
                onBoardEditor = { navController.navigate(Screen.BoardEditor.route) }
            )
        }

        composable(Screen.GameOptions.route) {
            GameOptionsScreen(
                viewModel = viewModel,
                onStartGame = { navController.navigate(Screen.Game.route) }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = viewModel,
                onNavigateBack = { 
                    // После подтверждения выхода из игры, возвращаемся в главное меню
                    navController.popBackStack() 
                }
            )
        }

        composable(Screen.Analysis.route) {
            AnalysisScreen(
                viewModel = viewModel,
                onGameSelected = { gameId ->
                    // Загружаем выбранную партию для просмотра
                    viewModel.viewSavedGame(gameId)
                    // Переходим на экран просмотра партии (GameReviewScreen вместо GameScreen)
                    navController.navigate(Screen.GameReview.route)
                },
                onNavigateToSavedGames = {
                    navController.navigate(Screen.SavedGames.route)
                }
            )
        }

        composable(Screen.Tasks.route) {
            TasksScreen(
                onPuzzleSelected = { puzzleId ->
                    // Сохраняем ID выбранной задачи и переходим к её решению
                    navController.navigate(Screen.PuzzleSolver.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PuzzleSolver.route) {
            PuzzleSolverScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Learning.route) {
            LearningScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel
            )
        }

        // Новый экран для просмотра истории партий
        composable(Screen.SavedGames.route) {
            SavedGamesScreen(
                viewModel = viewModel,
                onNavigateToGameReview = {
                    navController.navigate(Screen.GameReview.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Новый экран для просмотра выбранной партии
        composable(Screen.GameReview.route) {
            GameReviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Новый экран для редактора доски
        composable(Screen.BoardEditor.route) {
            BoardEditorScreen(
                viewModel = viewModel,
                onStartGame = { 
                    // После настройки позиции переходим на экран игры
                    navController.navigate(Screen.Game.route) {
                        // Очищаем стек навигации до экрана игры, чтобы кнопка "назад" вела в главное меню
                        popUpTo(Screen.MainMenu.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 