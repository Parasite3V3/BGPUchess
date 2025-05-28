package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.ui.components.MoveHistoryPanel
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import androidx.compose.ui.platform.LocalContext
import com.chunosov.chessbgpu.utils.ThemeManager
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameReviewScreen(
    viewModel: ChessViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    
    // Получаем цвета доски из темы
    val boardColors = themeManager.getBoardColors()
    val lightSquareColor = boardColors.first
    val darkSquareColor = boardColors.second
    
    val board by viewModel.board.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val currentViewedMoveIndex by viewModel.currentViewedMoveIndex.collectAsState()
    
    // Проверяем наличие ходов для навигации
    val canGoNext = currentViewedMoveIndex < moveHistory.size - 1
    val canGoPrevious = currentViewedMoveIndex > 0
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотр партии") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад в меню")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Информация о текущем ходе
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (moveHistory.isEmpty()) "Партия без ходов" else
                            "Ход ${currentViewedMoveIndex + 1} из ${moveHistory.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (moveHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentViewedMoveIndex % 2 == 0) "Ход белых" else "Ход черных",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // История ходов (некликабельная)
            MoveHistoryPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(bottom = 16.dp),
                moves = moveHistory,
                onMoveClicked = { /* Ничего не делаем при клике */ },
                board = board,
                currentMoveIndex = currentViewedMoveIndex,
                clickable = false // Явно указываем, что история некликабельна
            )
            
            // Шахматная доска (только для просмотра)
            ChessBoard(
                board = board,
                selectedPosition = null,
                possibleMoves = emptySet(),
                onSquareClick = { /* ничего не делаем при клике */ }, // Доска некликабельна
                isBlackBottom = board is com.chunosov.chessbgpu.model.BlackChangeBoard,
                kingInCheck = null,
                lightSquareColor = lightSquareColor,
                darkSquareColor = darkSquareColor,
                isInteractive = false, // Явно указываем, что доска некликабельна
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(vertical = 16.dp)
            )
            
            // Кнопки для навигации по ходам
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "Предыдущий ход"
                Button(
                    onClick = { 
                        if (canGoPrevious) {
                            viewModel.viewSavedGameMove(currentViewedMoveIndex - 1) 
                        }
                    },
                    enabled = canGoPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Предыдущий ход"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Пред. ход")
                }
                
                // Кнопка "Назад в меню"
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Назад в меню")
                }
                
                // Кнопка "Следующий ход"
                Button(
                    onClick = { 
                        if (canGoNext) {
                            viewModel.viewSavedGameMove(currentViewedMoveIndex + 1) 
                        }
                    },
                    enabled = canGoNext,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("След. ход")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Следующий ход"
                    )
                }
            }
        }
    }
} 