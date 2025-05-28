package com.chunosov.chessbgpu.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.chunosov.chessbgpu.model.SavedGame
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.ui.components.MoveHistoryPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGameViewScreen(
    savedGame: SavedGame,
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    // Загружаем сохраненную игру при первом отображении экрана
    LaunchedEffect(savedGame) {
        viewModel.viewSavedGame(savedGame.id)
    }
    
    // Следим за изменениями в истории ходов и индексе текущего хода
    val moveHistory by viewModel.moveHistory.collectAsState()
    val currentMoveIndex by viewModel.currentViewedMoveIndex.collectAsState()
    val totalMoves = moveHistory.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотр партии") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Информация о партии
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Дата: ${savedGame.formattedDate}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Ходов: ${savedGame.moveCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (savedGame.result.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Результат: ${savedGame.result}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // История ходов в новом улучшенном формате
            MoveHistoryPanel(
                moves = moveHistory,
                onMoveClicked = { index -> 
                    viewModel.viewSavedGameMove(index)
                },
                board = viewModel.board.collectAsState().value,
                currentMoveIndex = currentMoveIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 16.dp)
            )
            
            // Доска (будет использоваться компонент ChessBoard из основной игры)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Здесь отображение доски из viewModel
                ChessBoard(
                    board = viewModel.board.collectAsState().value,
                    selectedPosition = null,
                    possibleMoves = emptySet(),
                    onSquareClick = { /* Отключаем клики */ },
                    isBlackBottom = viewModel.board.collectAsState().value is com.chunosov.chessbgpu.model.BlackChangeBoard,
                    kingInCheck = viewModel.kingInCheck.collectAsState().value
                )
            }
            
            // Контролы для перемещения по ходам
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { 
                        // К первому ходу
                        viewModel.viewSavedGameMove(0)
                    },
                    enabled = currentMoveIndex > 0
                ) {
                    Text("« Начало")
                }
                
                IconButton(
                    onClick = { 
                        // Предыдущий ход
                        if (currentMoveIndex > 0) {
                            viewModel.viewSavedGameMove(currentMoveIndex - 1)
                        }
                    },
                    enabled = currentMoveIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Предыдущий ход"
                    )
                }
                
                Text(
                    text = "${currentMoveIndex + 1} / ${totalMoves}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(
                    onClick = { 
                        // Следующий ход
                        if (currentMoveIndex < totalMoves - 1) {
                            viewModel.viewSavedGameMove(currentMoveIndex + 1)
                        }
                    },
                    enabled = currentMoveIndex < totalMoves - 1
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Следующий ход"
                    )
                }
                
                Button(
                    onClick = { 
                        // К последнему ходу
                        viewModel.viewSavedGameMove(totalMoves - 1)
                    },
                    enabled = currentMoveIndex < totalMoves - 1
                ) {
                    Text("Конец »")
                }
            }
        }
    }
} 