package com.chunosov.chessbgpu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chunosov.chessbgpu.navigation.ChessNavGraph
import com.chunosov.chessbgpu.ui.components.BottomNavigationBar
import com.chunosov.chessbgpu.ui.theme.ChessBGPUTheme
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessBGPUTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: ChessViewModel = viewModel()
                    
                    // Инициализация контекста для доступа к SharedPreferences
                    LaunchedEffect(viewModel) {
                        viewModel.initializeContext(applicationContext)
                        // Загружаем сохраненные игры при старте приложения
                        viewModel.loadSavedGames()
                    }

                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            ChessNavGraph(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}