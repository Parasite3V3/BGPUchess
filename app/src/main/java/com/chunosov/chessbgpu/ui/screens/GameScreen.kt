package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.ui.components.GameSettingsDialog
import com.chunosov.chessbgpu.ui.theme.ChessBGPUTheme
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import com.chunosov.chessbgpu.viewmodel.GameState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import com.chunosov.chessbgpu.ui.components.MoveHistoryPanel
import com.chunosov.chessbgpu.ui.components.ExitGameDialog
import com.chunosov.chessbgpu.ui.components.NewGameConfirmationDialog
import com.chunosov.chessbgpu.ui.components.GameEndDialog
import com.chunosov.chessbgpu.ui.components.ResignConfirmationDialog
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.chunosov.chessbgpu.utils.ThemeManager
import com.chunosov.chessbgpu.utils.SoundManager
import androidx.constraintlayout.compose.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight

@Composable
fun GameScreen(
    viewModel: ChessViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val soundManager = remember { SoundManager.getInstance(context) }
    
    // Инициализируем SoundManager
    LaunchedEffect(Unit) {
        soundManager.setEnabled(themeManager.isSoundEnabled())
    }
    
    // Получаем цвета доски из темы
    val boardColors = themeManager.getBoardColors()
    val lightSquareColor = boardColors.first
    val darkSquareColor = boardColors.second
    
    val board by viewModel.board.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val whiteTimeRemaining by viewModel.whiteTimeRemaining.collectAsState()
    val blackTimeRemaining by viewModel.blackTimeRemaining.collectAsState()
    val hasTimeControl by viewModel.hasTimeControl.collectAsState()
    val kingInCheck by viewModel.kingInCheck.collectAsState()
    val possibleMoves by viewModel.possibleMoves.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val showExitDialog by viewModel.showExitDialog.collectAsState(false)
    val showNewGameDialog by viewModel.showNewGameConfirmationDialog.collectAsState()
    val showPawnPromotionDialog by viewModel.showPawnPromotionDialog.collectAsState()
    val currentViewedMoveIndex by viewModel.currentViewedMoveIndex.collectAsState()
    val showGameEndDialog by viewModel.showGameEndDialog.collectAsState()
    val showResignConfirmationDialog by viewModel.showResignConfirmationDialog.collectAsState()
    
    // Конвертируем состояние игры из ChessViewModel.GameState в GameState
    val convertedGameState = when (val state = gameState) {
        is ChessViewModel.GameState.NOT_STARTED -> GameState.NOT_STARTED
        is ChessViewModel.GameState.Playing -> GameState.Playing
        is ChessViewModel.GameState.Check -> GameState.Check(state.inCheck)
        is ChessViewModel.GameState.Checkmate -> GameState.Checkmate(state.inCheck)
        is ChessViewModel.GameState.Stalemate -> GameState.Stalemate
        is ChessViewModel.GameState.Draw -> GameState.Draw
        is ChessViewModel.GameState.WHITE_WINS_BY_TIME -> GameState.WHITE_WINS_BY_TIME
        is ChessViewModel.GameState.BLACK_WINS_BY_TIME -> GameState.BLACK_WINS_BY_TIME
        is ChessViewModel.GameState.WHITE_WINS_BY_RESIGNATION -> GameState.Resigned(PieceColor.WHITE)
        is ChessViewModel.GameState.BLACK_WINS_BY_RESIGNATION -> GameState.Resigned(PieceColor.BLACK)
        else -> GameState.Playing
    }
    
    // Определяем, находимся ли мы в режиме просмотра
    val isViewMode = currentViewedMoveIndex >= 0 || 
                    (convertedGameState !is GameState.Playing && 
                     convertedGameState !is GameState.NOT_STARTED && 
                     convertedGameState !is GameState.Check && // Важно: при шахе не переходим в режим просмотра
                     moveHistory.isNotEmpty())
    
    // Обработчик кнопки "Назад"
    BackHandler {
        if (isViewMode) {
            // Если мы в режиме просмотра - просто возвращаемся назад
            onNavigateBack()
        } else {
            // В режиме игры - показываем диалог подтверждения
            viewModel.onGameExit()
        }
    }
    
    var showSettings by remember { mutableStateOf(false) }
    
    // Следим за изменением диалога выхода
    LaunchedEffect(showExitDialog) {
        // Если диалог был закрыт и игра завершена, возвращаемся назад
        if (!showExitDialog && convertedGameState !is GameState.Playing && convertedGameState !is GameState.NOT_STARTED) {
            onNavigateBack()
        }
    }
    
    // Следим за изменением состояния игры для воспроизведения звуков
    LaunchedEffect(convertedGameState) {
        if (soundManager.isEnabled()) {
            when (convertedGameState) {
                is GameState.Check -> soundManager.playSound(SoundManager.SoundType.CHECK)
                is GameState.Checkmate -> soundManager.playSound(SoundManager.SoundType.CHECKMATE)
                GameState.Stalemate, GameState.Draw -> soundManager.playSound(SoundManager.SoundType.DRAW)
                GameState.WHITE_WINS_BY_TIME, GameState.BLACK_WINS_BY_TIME -> soundManager.playSound(SoundManager.SoundType.GAME_END)
                GameState.NOT_STARTED -> soundManager.playSound(SoundManager.SoundType.GAME_START)
                else -> { /* Ничего не воспроизводим */ }
            }
        }
    }
    
    // Показываем диалог подтверждения новой игры, если нужно
    if (showNewGameDialog) {
        NewGameConfirmationDialog(
            onConfirm = {
                viewModel.resetGame()
                viewModel.hideNewGameConfirmationDialog()
            },
            onDismiss = {
                viewModel.hideNewGameConfirmationDialog()
            }
        )
    }
    
    // Показываем диалог выхода из игры, если нужно
    if (showExitDialog) {
        ExitGameDialog(
            onDismiss = {
                viewModel.cancelExit()
            },
            onConfirm = {
                viewModel.confirmExit()
                onNavigateBack()
            }
        )
    }
    
    // Показываем диалог выбора фигуры при превращении пешки
    if (showPawnPromotionDialog) {
        com.chunosov.chessbgpu.ui.components.PawnPromotionDialog(
            pawnColor = currentTurn,
            onPieceSelected = { pieceType ->
                viewModel.promotePawn(pieceType)
            },
            onDismiss = {
                viewModel.dismissPawnPromotionDialog()
            }
        )
    }
    
    // Показываем диалог окончания игры, если игра завершена
    if (showGameEndDialog) {
        val isGameSaved by viewModel.isGameSaved.collectAsState()
        
        GameEndDialog(
            gameState = convertedGameState,
            onSaveGame = {
                viewModel.saveCurrentGameManually()
            },
            onBackToMenu = {
                viewModel.onBackToMenuFromEndDialog()
                onNavigateBack()
            },
            isSaved = isGameSaved
        )
    }
    
    if (showSettings) {
        GameSettingsDialog(
            onConfirm = { settings ->
                viewModel.setGameOptions(settings.opponent, settings.timeControl, settings.playerColor)
                viewModel.applyGameSettings()
                showSettings = false
            },
            onDismissRequest = {
                showSettings = false
            }
        )
    }
    
    // Показываем диалог подтверждения сдачи, если нужно
    if (showResignConfirmationDialog) {
        ResignConfirmationDialog(
            currentTurn = currentTurn,
            onConfirm = { viewModel.confirmResign() },
            onDismiss = { viewModel.cancelResign() }
        )
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val (blackTimeRef, whiteTimeRef, moveHistoryRef, boardRef, statusRef, undoButtonRef, newGameButtonRef, viewModeTextRef, startGameButtonRef) = createRefs()

        // Отображение текущего хода или статуса просмотра
        if (isViewMode) {
            Text(
                text = "Просмотр сохраненной партии",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.constrainAs(statusRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        } else {
            Text(
                text = when (convertedGameState) {
                    is GameState.Playing -> if (currentTurn == PieceColor.WHITE) "Ход белых" else "Ход черных"
                    is GameState.NOT_STARTED -> "Новая игра"
                    is GameState.Check -> "Шах ${if (convertedGameState.inCheck == PieceColor.WHITE) "белому" else "черному"} королю"
                    is GameState.Checkmate -> "Мат! ${if (convertedGameState.loser == PieceColor.WHITE) "Черные" else "Белые"} победили"
                    is GameState.Stalemate -> "Пат! Ничья"
                    is GameState.WHITE_WINS_BY_TIME -> "Белые победили по времени"
                    is GameState.BLACK_WINS_BY_TIME -> "Черные победили по времени"
                    is GameState.Resigned -> {
                        val resigned = if (convertedGameState.resignedColor == PieceColor.WHITE) "Белые" else "Черные"
                        val winner = if (convertedGameState.resignedColor == PieceColor.WHITE) "Черные" else "Белые"
                        "$resigned сдались! $winner победили"
                    }
                    else -> "Игра завершена"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.constrainAs(statusRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
            
            // Добавляем кнопку "Начать игру", если игра еще не началась
            if (convertedGameState is GameState.NOT_STARTED) {
                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier
                        .constrainAs(startGameButtonRef) {
                            top.linkTo(statusRef.bottom, 8.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text("Начать игру")
                }
            }
        }

        // Таймеры
        if (hasTimeControl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .constrainAs(whiteTimeRef) {
                        top.linkTo(statusRef.bottom, 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Время белых: ${formatTime(whiteTimeRemaining)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Время черных: ${formatTime(blackTimeRemaining)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // История ходов
        MoveHistoryPanel(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .constrainAs(moveHistoryRef) {
                    top.linkTo(
                        when {
                            hasTimeControl -> whiteTimeRef.bottom
                            convertedGameState is GameState.NOT_STARTED -> startGameButtonRef.bottom
                            else -> statusRef.bottom
                        }, 8.dp
                    )
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.value(150.dp)
                },
            moves = moveHistory,
            onMoveClicked = if (!isViewMode) { 
                { moveIndex -> viewModel.viewSavedGameMove(moveIndex) }
            } else {
                { /* Ничего не делаем при клике в режиме просмотра */ }
            },
            board = board,
            currentMoveIndex = currentViewedMoveIndex,
            clickable = !isViewMode
        )

        // Шахматная доска
        ChessBoard(
            board = board,
            selectedPosition = selectedPosition,
            possibleMoves = possibleMoves,
            onSquareClick = { position ->
                // В режиме просмотра нельзя делать ходы
                if (!isViewMode) {
                    // Проверяем, есть ли выбранная позиция и фигура на целевой позиции
                    val targetPiece = board.getPiece(position)
                    val isCapture = selectedPosition != null && targetPiece != null
                    
                    viewModel.onSquareClick(position)
                    
                    // Проигрываем звук в зависимости от типа хода
                    if (selectedPosition != null && position in possibleMoves) {
                        if (isCapture) {
                            soundManager.playSound(SoundManager.SoundType.CAPTURE)
                        } else {
                            soundManager.playSound(SoundManager.SoundType.MOVE)
                        }
                    }
                }
            },
            isBlackBottom = board is com.chunosov.chessbgpu.model.BlackChangeBoard,
            kingInCheck = kingInCheck,
            lightSquareColor = lightSquareColor,
            darkSquareColor = darkSquareColor,
            isInteractive = !isViewMode,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .constrainAs(boardRef) {
                    top.linkTo(moveHistoryRef.bottom, 16.dp)
                    bottom.linkTo(undoButtonRef.top, 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // Кнопки управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(undoButtonRef) {
                    bottom.linkTo(parent.bottom, 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isViewMode) {
                // В режиме просмотра показываем кнопки навигации по ходам
                val canGoNext = currentViewedMoveIndex < moveHistory.size - 1
                val canGoPrevious = currentViewedMoveIndex > 0
                
                // Кнопка "Предыдущий ход"
                Button(
                    onClick = { 
                        if (canGoPrevious) {
                            viewModel.viewSavedGameMove(currentViewedMoveIndex - 1) 
                        }
                    },
                    enabled = canGoPrevious,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
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
                    onClick = { onNavigateBack() },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
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
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text("След. ход")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Следующий ход"
                    )
                }
            } else {
                // В режиме игры показываем стандартные кнопки
                Button(onClick = { viewModel.undoLastMove() }) {
                    Text("Отменить ход")
                }
                
                Button(onClick = { 
                    // Показываем диалог настроек новой игры
                    showSettings = true
                }) {
                    Text("Новая игра")
                }
                
                // Добавляем кнопку "Сдаться", только если игра в процессе
                if (convertedGameState is GameState.Playing || convertedGameState is GameState.Check) {
                    Button(
                        onClick = { viewModel.showResignConfirmation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Сдаться")
                    }
                }
            }
        }
    }
}

@Composable
private fun formatTime(timeMillis: Long): String {
    val minutes = timeMillis / 60000
    val seconds = (timeMillis % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameScreenPreview() {
    ChessBGPUTheme {
        // Для превью используем конструктор без параметров
        val previewViewModel = ChessViewModel()
        GameScreen(viewModel = previewViewModel)
    }
} 