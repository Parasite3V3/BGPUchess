package com.chunosov.chessbgpu.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import com.chunosov.chessbgpu.viewmodel.GameState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.chunosov.chessbgpu.ui.components.GameEndDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(viewModel: ChessViewModel) {
    val board by viewModel.board.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val possibleMoves by viewModel.possibleMoves.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val kingInCheck by viewModel.kingInCheck.collectAsState()
    val hasTimeControl by viewModel.hasTimeControl.collectAsState()
    val whiteTimeRemaining by viewModel.whiteTimeRemaining.collectAsState()
    val blackTimeRemaining by viewModel.blackTimeRemaining.collectAsState()
    val showGameEndDialog by viewModel.showGameEndDialog.collectAsState()
    val currentViewedMoveIndex by viewModel.currentViewedMoveIndex.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val isViewingSavedGame = currentViewedMoveIndex >= 0
    
    // Отладочный вывод для контроля состояния игры
    LaunchedEffect(gameState) {
        println("ChessScreen: Текущее состояние игры = $gameState, показывать диалог = $showGameEndDialog")
        println("ChessScreen: Просмотр сохраненной партии: $isViewingSavedGame, индекс хода: $currentViewedMoveIndex")
    }
    
    // Усиленная логика - автоматически показываем диалог окончания игры при любом завершающем состоянии игры
    LaunchedEffect(gameState) {
        if ((gameState is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate || 
             gameState is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Stalemate || 
             gameState == com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_TIME || 
             gameState == com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_TIME ||
             gameState == com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Draw) && 
            !showGameEndDialog) {
            println("ChessScreen: Обнаружено завершающее состояние игры: $gameState")
            // Вместо вызова функции, которой нет, устанавливаем значение _showGameEndDialog в true
            // viewModel.showGameEndDialog()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isViewingSavedGame) "Просмотр партии" else "Шахматы") },
                actions = {
                    if (!isViewingSavedGame) {
                        // Добавляем тестовую кнопку для проверки диалога мата
                        IconButton(onClick = { viewModel.forceCheckmate() }) {
                            Text("Тест МАТА", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                        }
                        
                        IconButton(onClick = { viewModel.resetGame() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Новая игра")
                        }
                    } else {
                        // Если просматриваем сохраненную партию, показываем надпись о просмотре
                        Text("Режим просмотра", 
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Отображение оставшегося времени для черных
            if (hasTimeControl && !isViewingSavedGame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(blackTimeRemaining),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // Если находимся в режиме просмотра партии, показываем информацию
            if (isViewingSavedGame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Просмотр сохраненной партии",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Шахматная доска
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ChessBoard(
                    board = board,
                    selectedPosition = selectedPosition,
                    possibleMoves = possibleMoves,
                    onSquareClick = { position -> 
                        // Блокируем ходы при просмотре сохраненной партии
                        if (!isViewingSavedGame) {
                            viewModel.onSquareClick(position)
                        } else {
                            // При просмотре партии ничего не происходит при клике
                            println("Клик по доске в режиме просмотра партии игнорируется")
                        }
                    },
                    isBlackBottom = board is com.chunosov.chessbgpu.model.BlackChangeBoard,
                    kingInCheck = kingInCheck
                )
            }
            
            // Добавляем навигацию для просмотра партии
            if (isViewingSavedGame) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.viewPreviousMove() },
                        enabled = currentViewedMoveIndex > 0,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Предыдущий ход")
                    }
                    
                    Text(
                        text = "${currentViewedMoveIndex + 1}/${moveHistory.size}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Button(
                        onClick = { viewModel.viewNextMove() },
                        enabled = currentViewedMoveIndex < moveHistory.size - 1,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Следующий ход")
                    }
                }
            }
            
            // Отображение оставшегося времени для белых
            if (hasTimeControl && !isViewingSavedGame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(whiteTimeRemaining),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // Информация о текущем состоянии игры
            if (!isViewingSavedGame) {
                GameStateInfo(gameState, currentTurn)
            }
        }
    }

    // Показываем диалог окончания игры, если игра завершена и не в режиме просмотра
    if (showGameEndDialog && !isViewingSavedGame) {
        val isGameSaved by viewModel.isGameSaved.collectAsState()
        
        com.chunosov.chessbgpu.ui.components.GameEndDialog(
            gameState = when (val state = gameState) {
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Checkmate(state.inCheck)
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Check(state.inCheck)
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Stalemate -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Stalemate
                com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_TIME -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.WHITE_WINS_BY_TIME
                com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_TIME -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.BLACK_WINS_BY_TIME
                com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Draw -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Draw
                com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_RESIGNATION -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Resigned(com.chunosov.chessbgpu.model.PieceColor.WHITE)
                com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_RESIGNATION -> 
                    com.chunosov.chessbgpu.viewmodel.GameState.Resigned(com.chunosov.chessbgpu.model.PieceColor.BLACK)
                else -> com.chunosov.chessbgpu.viewmodel.GameState.Playing
            },
            onSaveGame = { viewModel.saveCurrentGameManually() },
            onBackToMenu = { viewModel.onBackToMenuFromEndDialog() },
            isSaved = isGameSaved
        )
    }
}

@Composable
private fun GameStateInfo(gameState: com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState, currentTurn: PieceColor) {
    // Определяем текст состояния игры
    val stateText = when (gameState) {
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check -> "Шах ${if (gameState.inCheck == PieceColor.WHITE) "белым" else "черным"}!"
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> "Мат! ${if (gameState.inCheck == PieceColor.WHITE) "Черные" else "Белые"} выиграли!"
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Stalemate -> "Пат! Ничья"
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_TIME -> "Время белых истекло. Черные выиграли!"
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_TIME -> "Время черных истекло. Белые выиграли!"
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Draw -> "Ничья!"
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Playing -> "Ход ${if (currentTurn == PieceColor.WHITE) "белых" else "черных"}"
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.NOT_STARTED -> ""
        else -> ""
    }
    
    // Определяем цвет фона в зависимости от состояния игры
    val backgroundColor = when (gameState) {
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check -> Color(0xFFFFD700) // Золотой для шаха
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> Color(0xFFFF6347) // Томатный для мата
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Stalemate, com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Draw -> Color(0xFF90EE90) // Светло-зеленый для ничьи
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_TIME, com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_TIME -> Color(0xFFFF6347) // Томатный для победы по времени
        else -> MaterialTheme.colorScheme.surface
    }
    
    // Определяем цвет текста в зависимости от состояния игры
    val textColor = when (gameState) {
        is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check, is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate, 
        com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.BLACK_WINS_BY_TIME, com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.WHITE_WINS_BY_TIME -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (gameState) {
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check, is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> 8.dp
                else -> 4.dp
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            text = stateText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = when (gameState) {
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check, is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            fontSize = when (gameState) {
                is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Check, is com.chunosov.chessbgpu.viewmodel.ChessViewModel.GameState.Checkmate -> 20.sp
                else -> 16.sp
            }
        )
    }
}

private fun formatTime(timeMillis: Long): String {
    val minutes = timeMillis / 60000
    val seconds = (timeMillis % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
} 