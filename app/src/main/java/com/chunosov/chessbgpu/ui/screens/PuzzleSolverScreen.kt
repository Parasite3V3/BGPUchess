package com.chunosov.chessbgpu.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.*
import com.chunosov.chessbgpu.engine.ChessLibAdapter
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.ui.PuzzleManagerUIAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.chunosov.chessbgpu.data.ChessPuzzle as DataChessPuzzle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleSolverScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val puzzleManager = remember { PuzzleManagerUIAdapter(context) }
    val currentPuzzle by puzzleManager.currentPuzzle.collectAsState()
    val currentFen by puzzleManager.currentFen.collectAsState()
    val isChessLibReady by puzzleManager.isChessLibReady.collectAsState()
    val showSuccessDialog by puzzleManager.showSuccessDialog.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Состояние для шахматной доски
    val board = remember { mutableStateOf<ChessBoard?>(null) }
    val selectedPosition = remember { mutableStateOf<Position?>(null) }
    val possibleMoves = remember { mutableStateOf<Set<Position>>(emptySet()) }
    
    // Состояние для обработки неправильных ходов
    val showIncorrectMove = remember { mutableStateOf(false) }
    val incorrectPosition = remember { mutableStateOf<Position?>(null) }
    
    // Состояние для отображения статуса решения задачи
    val showSuccessMessage = remember { mutableStateOf(false) }
    val isPuzzleComplete = remember { mutableStateOf(false) }
    
    // Состояние для подсказки
    val showHint = remember { mutableStateOf(false) }
    val hintMessage = remember { mutableStateOf<String?>(null) }
    
    // Состояние для проверки задачи
    val showValidationResult = remember { mutableStateOf(false) }
    val validationMessage = remember { mutableStateOf("") }
    
    // Отладочное состояние
    val debugMessage = remember { mutableStateOf<String?>(null) }
    val showDebugMessage = remember { mutableStateOf(false) }
    
    // Состояние для диалогового окна
    val openDialog = remember { mutableStateOf(false) }
    
    // Функция для отображения отладочного сообщения
    fun showDebug(message: String, durationMillis: Long = 3000) {
        debugMessage.value = message
        showDebugMessage.value = true
        
        coroutineScope.launch {
            delay(durationMillis)
            showDebugMessage.value = false
        }
    }
    
    // Инициализируем ChessLib при первом запуске
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            puzzleManager.initializeChessLib()
            
            // Добавляем отладочный вывод для диагностики проблемы
            val puzzle = puzzleManager.currentPuzzle.value
            if (puzzle == null) {
                showDebug("DEBUG: Текущая задача в puzzleManager равна null")
            } else {
                showDebug("Загружена задача ${puzzle.id} с начальным FEN: ${puzzle.initialFen}")
                
                // Инициализируем доску сразу
                val chessBoard = ChessBoard()
                chessBoard.setPositionFromFen(puzzle.initialFen)
                board.value = chessBoard
            }
        }
    }
    
    // Отслеживаем изменение значения currentPuzzle
    LaunchedEffect(currentPuzzle) {
        if (currentPuzzle == null) {
            showDebug("currentPuzzle изменился на null")
        } else {
            showDebug("Загружена задача: ${currentPuzzle!!.id}")
            
            // Если FEN еще не установлен, инициализируем с начальным FEN
            val initialFen = currentPuzzle!!.initialFen
            showDebug("Устанавливаем начальный FEN: $initialFen")
            
            val chessBoard = ChessBoard()
            chessBoard.setPositionFromFen(initialFen)
            board.value = chessBoard
        }
    }
    
    // Инициализируем доску при изменении FEN
    LaunchedEffect(currentFen) {
        val fenValue = currentFen
        if (fenValue != null) {
            showDebug("FEN изменился: $fenValue")
            
            val chessBoard = ChessBoard()
            chessBoard.setPositionFromFen(fenValue)
            board.value = chessBoard
        }
    }
    
    // Показываем сообщение об успешном решении задачи
    LaunchedEffect(isPuzzleComplete.value) {
        if (isPuzzleComplete.value) {
            showSuccessMessage.value = true
            delay(3000) // Показываем сообщение на 3 секунды
            showSuccessMessage.value = false
        }
    }
    
    // Показываем диалоговое окно при изменении showSuccessDialog
    LaunchedEffect(showSuccessDialog) {
        if (showSuccessDialog) {
            openDialog.value = true
        }
    }
    
    // Диалоговое окно для отображения сообщения об успешном решении задачи
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { 
                openDialog.value = false 
            },
            title = { Text("Поздравляем!") },
            text = { Text("Задача успешно решена!") },
            confirmButton = {
                Button(
                    onClick = { 
                        openDialog.value = false
                        onNavigateBack()
                    }
                ) {
                    Text("К списку задач")
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        openDialog.value = false 
                    }
                ) {
                    Text("Остаться")
                }
            }
        )
    }
    
    // Функция для получения подсказки
    fun getHint() {
        coroutineScope.launch {
            val hint = puzzleManager.getHint()
            if (hint != null && hint.toString().length >= 4) {
                try {
                    // Преобразуем алгебраическую нотацию в более понятный формат
                    val hintStr = hint.toString()
                    val fromPos = hintStr.substring(0, 2)
                    val toPos = hintStr.substring(2, 4)
                    
                    // Проверяем корректность координат
                    val fromPosition = Position.fromAlgebraicNotation(fromPos)
                    val toPosition = Position.fromAlgebraicNotation(toPos)
                    
                    if (fromPosition != null && toPosition != null) {
                        // Получаем информацию о фигуре
                        val piece = board.value?.getPiece(fromPosition)
                        
                        // Получаем название фигуры на русском
                        val pieceName = when (piece?.type) {
                            PieceType.QUEEN -> "Ферзь"
                            PieceType.ROOK -> "Ладья"
                            PieceType.BISHOP -> "Слон"
                            PieceType.KNIGHT -> "Конь"
                            PieceType.KING -> "Король"
                            PieceType.PAWN -> "Пешка"
                            else -> "Фигура"
                        }
                        
                        // Формируем сообщение подсказки
                        hintMessage.value = "$pieceName с $fromPos на $toPos"
                        showHint.value = true
                        
                        // Подсвечиваем рекомендуемый ход на доске
                        selectedPosition.value = fromPosition
                        possibleMoves.value = setOf(toPosition)
                        
                        // Скрываем подсказку через 5 секунд
                        delay(5000)
                        showHint.value = false
                        selectedPosition.value = null
                        possibleMoves.value = emptySet()
                    } else {
                        showDebug("Некорректный формат подсказки")
                    }
                } catch (e: Exception) {
                    showDebug("Ошибка при обработке подсказки: ${e.message}")
                }
            } else {
                showDebug("Подсказка недоступна")
            }
        }
    }
    
    // Функция для отображения сообщения об ошибке
    fun showErrorMessage(message: String, duration: Long = 3000) {
        coroutineScope.launch {
            hintMessage.value = message
            showHint.value = true
            
            // Скрываем сообщение через указанное время
            delay(duration)
            showHint.value = false
        }
    }
    
    // Функция для проверки задачи
    fun validatePuzzle() {
        puzzleManager.validatePuzzle { result ->
            if (result.isValid) {
                validationMessage.value = "Задача корректна"
            } else {
                validationMessage.value = "Ошибка в задаче: ${result.errorMessage}"
            }
            showValidationResult.value = true
            
            // Скрываем сообщение через 5 секунд
            coroutineScope.launch {
                delay(5000)
                showValidationResult.value = false
            }
        }
    }
    
    // Функция для исправления задачи
    fun fixPuzzle() {
        puzzleManager.fixPuzzleSolution { fixedSolution ->
            if (fixedSolution != null) {
                validationMessage.value = "Задача исправлена. Новое решение содержит ${fixedSolution.size} ходов"
            } else {
                validationMessage.value = "Не удалось исправить задачу"
            }
            showValidationResult.value = true
            
            // Скрываем сообщение через 5 секунд
            coroutineScope.launch {
                delay(5000)
                showValidationResult.value = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Решение задачи") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка подсказки
                    IconButton(onClick = { getHint() }) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Подсказка")
                    }
                    
                    // Кнопка проверки задачи
                    if (isChessLibReady) {
                        IconButton(onClick = { validatePuzzle() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Проверить задачу")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Добавляем прокрутку для всего содержимого
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Описание задачи
            currentPuzzle?.let { puzzle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = puzzle.puzzleType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = puzzle.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Шахматная доска
            board.value?.let { chessBoard ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        ChessBoard(
                            board = chessBoard,
                            selectedPosition = selectedPosition.value,
                            possibleMoves = possibleMoves.value,
                            onSquareClick = { position ->
                                onChessBoardClick(
                                    position = position,
                                    board = chessBoard, 
                                    selectedPosition = selectedPosition,
                                    possibleMoves = possibleMoves,
                                    puzzleManager = puzzleManager,
                                    showIncorrectMove = showIncorrectMove,
                                    incorrectPosition = incorrectPosition,
                                    isPuzzleComplete = isPuzzleComplete,
                                    coroutineScope = coroutineScope,
                                    useChessLib = isChessLibReady,
                                    showErrorMessage = { message, duration ->
                                        showErrorMessage(message, duration)
                                    }
                                )
                            },
                            isBlackBottom = false,
                            kingInCheck = null,
                            lightSquareColor = if (showIncorrectMove.value && 
                                            selectedPosition.value == incorrectPosition.value)
                                            Color(0xFFFF6B6B) else Color(0xFFF0D9B5),
                            darkSquareColor = if (showIncorrectMove.value && 
                                            selectedPosition.value == incorrectPosition.value)
                                            Color(0xFFCC3333) else Color(0xFFB58863)
                        )
                    }
                }
            } ?: run {
                // Показываем заглушку, пока доска не загрузилась
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.LightGray)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Индикатор прогресса
            currentPuzzle?.let { puzzle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прогресс: ${puzzle.currentMoveIndex}/${puzzle.solutionMoves.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Button(
                        onClick = {
                            puzzleManager.undoLastMove { previousFen ->
                                if (previousFen != null) {
                                    // Обновляем доску
                                    board.value?.let { chessBoard ->
                                        // Полностью пересоздаем доску с новым FEN, а не просто обновляем текущую
                                        val newBoard = ChessBoard()
                                        newBoard.setPositionFromFen(previousFen)
                                        board.value = newBoard
                                        
                                        // Сбрасываем выделение
                                        selectedPosition.value = null
                                        possibleMoves.value = emptySet()
                                        
                                        // Показываем сообщение об отмене хода
                                        showDebug("Ход отменен")
                                    }
                                } else {
                                    showDebug("Невозможно отменить ход")
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Отменить ход")
                    }
                }
            }
            
            // Сообщение-подсказка
            if (showHint.value && hintMessage.value != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hintMessage.value!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Сообщение о валидации задачи
            if (showValidationResult.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = validationMessage.value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Сообщение об успешном решении
            if (showSuccessMessage.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Задача успешно решена!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Отладочное сообщение
            if (showDebugMessage.value && debugMessage.value != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0xFF333333), shape = MaterialTheme.shapes.medium)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = debugMessage.value!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
            // Кнопки для управления шахматной задачей
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        // Возвращаемся к списку задач
                        onNavigateBack()
                    }
                ) {
                    Text("К списку задач")
                }
            }
        }
    }
}

/**
 * Обрабатывает клик по шахматной доске
 */
private fun onChessBoardClick(
    position: Position,
    board: ChessBoard,
    selectedPosition: MutableState<Position?>,
    possibleMoves: MutableState<Set<Position>>,
    puzzleManager: PuzzleManagerUIAdapter,
    showIncorrectMove: MutableState<Boolean>,
    incorrectPosition: MutableState<Position?>,
    isPuzzleComplete: MutableState<Boolean>,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    useChessLib: Boolean = false,
    showErrorMessage: (message: String, duration: Long) -> Unit
) {
    val piece = board.getPiece(position)
    val currentTurn = "WHITE" // По умолчанию ход белых
    
    // Если уже есть выбранная позиция
    if (selectedPosition.value != null) {
        // Если кликнули по той же клетке, снимаем выделение
        if (position == selectedPosition.value) {
            selectedPosition.value = null
            possibleMoves.value = emptySet()
            return
        }
        
        // Если кликнули по возможному ходу
        if (position in possibleMoves.value) {
            val from = selectedPosition.value!!
            val to = position
            
            // Получаем информацию о фигуре
            val movingPiece = board.getPiece(from) ?: return
            
            // Создаем алгебраическую нотацию хода
            val moveNotation = "${from.toAlgebraicNotation()}${to.toAlgebraicNotation()}"
            
            // Добавляем отладочную информацию
            println("DEBUG: Пользователь сделал ход: $moveNotation")
            println("DEBUG: Позиция фигуры: from=${from.row},${from.col} to=${to.row},${to.col}")
            println("DEBUG: Алгебраическая нотация: from=${from.toAlgebraicNotation()} to=${to.toAlgebraicNotation()}")
            println("DEBUG: Тип фигуры: ${movingPiece.type}, цвет: ${movingPiece.color}")
            
            // Проверяем, правильный ли это ход для текущей задачи
            val isCorrectMove = puzzleManager.checkMove(moveNotation)
            println("DEBUG: Результат проверки хода: $isCorrectMove")
            
            // Получаем информацию о текущей задаче для отладки
            puzzleManager.currentPuzzle.value?.let { puzzle ->
                println("DEBUG: Текущая задача: ${puzzle.id}")
                println("DEBUG: Текущий индекс хода: ${puzzleManager.currentMoveIndex.value}")
                println("DEBUG: Ожидаемый ход: ${puzzle.solutionMoves.getOrNull(puzzleManager.currentMoveIndex.value)}")
            }
            
            if (isCorrectMove) {
                // Делаем ход на доске
                board.movePiece(from, to)
                
                // Обновляем состояние в PuzzleManager
                puzzleManager.makeMove(moveNotation)
                
                // Очищаем выделение
                selectedPosition.value = null
                possibleMoves.value = emptySet()
                
                // Показываем сообщение о правильном ходе
                showErrorMessage("Правильный ход!", 1500)
                
                // Проверяем, является ли это первым ходом во второй задаче (ферзь на b8)
                puzzleManager.currentPuzzle.value?.let { puzzle ->
                    if (puzzle.id == "mate-in-two-001" && puzzleManager.currentMoveIndex.value == 1) {
                        // Если это первый ход в задаче мат в 2 хода, делаем автоматический ход конем
                        coroutineScope.launch {
                            delay(1000) // Задержка 1 секунда перед ходом компьютера
                            
                            // Получаем ход компьютера (конь с d7 на b8)
                            val computerMove = "d7b8"
                            val fromNotation = computerMove.substring(0, 2)
                            val toNotation = computerMove.substring(2, 4)
                            
                            val fromPos = Position.fromAlgebraicNotation(fromNotation)
                            val toPos = Position.fromAlgebraicNotation(toNotation)
                            
                            if (fromPos != null && toPos != null) {
                                // Показываем ход компьютера
                                showErrorMessage("Ход компьютера: Конь берет ферзя на b8", 2000)
                                
                                // Визуально отмечаем клетки хода компьютера
                                selectedPosition.value = fromPos
                                possibleMoves.value = setOf(toPos)
                                
                                // Делаем задержку для анимации
                                delay(600) // Задержка перед перемещением фигуры
                                
                                // Выполняем ход на доске
                                board.movePiece(fromPos, toPos)
                                
                                // Получаем FEN после хода компьютера
                                val resultingFen = board.getFen()
                                println("DEBUG: FEN после хода компьютера: $resultingFen")
                                
                                // Обновляем состояние в PuzzleManager
                                puzzleManager.makeComputerMove(resultingFen) { success ->
                                    println("DEBUG: Результат makeComputerMove: $success")
                                    if (success) {
                                        // Вызываем обработчик хода компьютера для второй задачи
                                        coroutineScope.launch {
                                            puzzleManager.handleComputerMoveInMateInTwo()
                                            
                                            // Сбрасываем выделение после хода
                                            delay(300)
                                            selectedPosition.value = null
                                            possibleMoves.value = emptySet()
                                        }
                                    }
                                }
                            }
                        }
                        return
                    }
                }
                
                // Проверяем, завершена ли задача
                puzzleManager.isPuzzleComplete { complete ->
                    isPuzzleComplete.value = complete
                    if (complete) {
                        showErrorMessage("Поздравляем! Задача решена!", 3000)
                    } else {
                        // Если задача не завершена, делаем ход компьютера
                        coroutineScope.launch {
                            delay(1000) // Небольшая задержка перед ходом компьютера
                            
                            puzzleManager.getComputerResponse { computerMove ->
                                if (computerMove != null && computerMove.toString().length >= 4) {
                                    val moveStr = computerMove.toString()
                                    val fromNotation = moveStr.substring(0, 2)
                                    val toNotation = moveStr.substring(2, 4)
                                    
                                    val fromPos = Position.fromAlgebraicNotation(fromNotation)
                                    val toPos = Position.fromAlgebraicNotation(toNotation)
                                    
                                    if (fromPos != null && toPos != null) {
                                        // Показываем ход компьютера
                                        showErrorMessage("Ход компьютера: ${computerMove.uppercase()}", 1500)
                                        
                                        // Визуально отмечаем клетки хода компьютера
                                        selectedPosition.value = fromPos
                                        possibleMoves.value = setOf(toPos)
                                        
                                        // Делаем задержку для анимации
                                        coroutineScope.launch {
                                            delay(600) // Задержка перед перемещением фигуры
                                            
                                            // Выполняем ход на доске
                                            board.movePiece(fromPos, toPos)
                                            
                                            // Получаем FEN после хода компьютера
                                            val resultingFen = board.getFen()
                                            println("DEBUG: FEN после хода компьютера: $resultingFen")
                                            
                                            // Обновляем состояние в PuzzleManager
                                            puzzleManager.makeComputerMove(resultingFen) { success ->
                                                println("DEBUG: Результат makeComputerMove: $success")
                                                if (success) {
                                                    // Вызываем обработчик хода компьютера для второй задачи
                                                    puzzleManager.currentPuzzle.value?.let { puzzle ->
                                                        if (puzzle.id == "mate-in-two-001") {
                                                            coroutineScope.launch {
                                                                puzzleManager.handleComputerMoveInMateInTwo()
                                                            }
                                                        }
                                                    }
                                                    
                                                    // Сбрасываем выделение после хода
                                                    coroutineScope.launch {
                                                        delay(300)
                                                        selectedPosition.value = null
                                                        possibleMoves.value = emptySet()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Дополнительная проверка - возможно ход корректный, но не соответствует решению
                // Проверим, является ли ход допустимым с точки зрения шахматных правил
                val isPossibleMove = position in possibleMoves.value
                
                if (isPossibleMove) {
                    // Ход допустим по правилам, но не соответствует решению задачи
                    showErrorMessage("Ход возможен, но не является решением задачи", 2000)
                }
                
                // Показываем ошибку
                showIncorrectMove.value = true
                incorrectPosition.value = position
                
                // Очищаем выделение
                selectedPosition.value = null
                possibleMoves.value = emptySet()
                
                // Скрываем ошибку через некоторое время
                coroutineScope.launch {
                    delay(500)
                    showIncorrectMove.value = false
                    incorrectPosition.value = null
                }
                
                showErrorMessage("Неправильный ход. Попробуйте еще раз.", 2000)
            }
        } else {
            // Если кликнули по другой клетке (не из возможных ходов)
            // проверяем, можно ли выбрать эту клетку
            
            // Очищаем текущее выделение
            selectedPosition.value = null
            possibleMoves.value = emptySet()
            
            // И пробуем выбрать новую клетку (чтобы не дублировать код)
            onChessBoardClick(
                position = position,
                board = board,
                selectedPosition = selectedPosition,
                possibleMoves = possibleMoves,
                puzzleManager = puzzleManager,
                showIncorrectMove = showIncorrectMove,
                incorrectPosition = incorrectPosition,
                isPuzzleComplete = isPuzzleComplete,
                coroutineScope = coroutineScope,
                useChessLib = useChessLib,
                showErrorMessage = showErrorMessage
            )
        }
    } else {
        // Если нет выбранной позиции, проверяем, можно ли выбрать кликнутую клетку
        if (piece != null) {
            val isCurrentPlayerWhite = currentTurn == "WHITE"
            val isPieceWhite = piece.color == PieceColor.WHITE
            
            // Можно выбирать только свои фигуры
            if (isPieceWhite == isCurrentPlayerWhite) {
                selectedPosition.value = position
                
                // Получаем все возможные ходы для выбранной фигуры
                possibleMoves.value = board.getPossibleMoves(position)
                
                // Если нет возможных ходов, показываем сообщение
                if (possibleMoves.value.isEmpty()) {
                    showErrorMessage("Нет возможных ходов для этой фигуры", 1500)
                    selectedPosition.value = null
                }
            } else {
                showErrorMessage("Сейчас ход ${if (isCurrentPlayerWhite) "белых" else "черных"}", 2000)
            }
        }
    }
} 