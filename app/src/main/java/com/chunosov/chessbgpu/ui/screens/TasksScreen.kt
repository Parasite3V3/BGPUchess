package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.ChessPuzzle
import com.chunosov.chessbgpu.ui.PuzzleManagerUIAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.chunosov.chessbgpu.data.ChessPuzzle as DataChessPuzzle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onPuzzleSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val puzzleManager = remember { PuzzleManagerUIAdapter(context) }
    val puzzles by puzzleManager.puzzles.collectAsState()
    val isLoading by puzzleManager.isLoading.collectAsState()
    val isChessLibReady by puzzleManager.isChessLibReady.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    // Состояние для сообщения о результате проверки
    val showValidationResult = remember { mutableStateOf(false) }
    val validationMessage = remember { mutableStateOf("") }
    
    // Функция инициализации шахматной библиотеки
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Инициализируем ChessLib при первом запуске
            puzzleManager.initializeChessLib()
            
            // Загружаем задачи, если их еще нет
            if (puzzles.isEmpty()) {
                puzzleManager.loadAndValidateTestPuzzles()
            }
        }
    }
    
    // Функция для проверки всех задач
    fun validateAllPuzzles() {
        if (!isChessLibReady) {
            validationMessage.value = "Шахматная библиотека не инициализирована. Проверка невозможна."
            showValidationResult.value = true
            coroutineScope.launch {
                delay(3000)
                showValidationResult.value = false
            }
            return
        }
        
        var validCount = 0
        var invalidCount = 0
        var fixedCount = 0
        
        validationMessage.value = "Проверка задач..."
        showValidationResult.value = true
        
        // Проверяем каждую задачу
        coroutineScope.launch {
            for (puzzle in puzzles) {
                puzzleManager.validatePuzzle(puzzle.id) { result ->
                    if (result.isValid) {
                        validCount++
                    } else {
                        invalidCount++
                        // Пытаемся исправить задачу
                        puzzleManager.fixPuzzleSolution(puzzle.id) { fixedSolution ->
                            if (fixedSolution != null) {
                                fixedCount++
                            }
                            
                            // Обновляем сообщение после каждой проверки
                            validationMessage.value = "Проверка: ${validCount + invalidCount}/${puzzles.size}\n" +
                                                    "Корректных: $validCount\n" +
                                                    "Некорректных: $invalidCount\n" +
                                                    "Исправлено: $fixedCount"
                        }
                    }
                }
            }
            
            // Финальное сообщение
            validationMessage.value = "Проверка завершена\n" +
                                    "Корректных задач: $validCount\n" +
                                    "Некорректных задач: $invalidCount\n" +
                                    "Исправлено задач: $fixedCount"
            
            // Обновляем список задач после проверки
            if (fixedCount > 0) {
                puzzleManager.refreshPuzzles()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Шахматные задачи") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isChessLibReady) {
                        IconButton(onClick = { validateAllPuzzles() }) {
                            Icon(Icons.Default.BugReport, contentDescription = "Проверить все задачи")
                        }
                    }
                    
                    IconButton(onClick = {
                        coroutineScope.launch {
                            puzzleManager.loadTestPuzzles()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить задачи")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (puzzles.isEmpty()) {
                // Показываем сообщение, если задач нет
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Нет доступных задач",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    puzzleManager.loadTestPuzzles()
                                }
                            }
                        ) {
                            Text("Загрузить тестовые задачи")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    puzzleManager.loadSimplePuzzles()
                                }
                            }
                        ) {
                            Text("Загрузить простые задачи")
                        }
                    }
                }
            } else {
                // Показываем список задач
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(puzzles) { puzzle ->
                        PuzzleCard(
                            puzzle = puzzle,
                            onClick = { 
                                // Выбираем задачу в PuzzleManager перед переходом к экрану решения
                                puzzleManager.selectPuzzleById(puzzle.id) { success ->
                                    if (success) {
                                        onPuzzleSelected(puzzle.id)
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Показываем результат проверки задач
                if (showValidationResult.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = validationMessage.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PuzzleCard(
    puzzle: DataChessPuzzle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = puzzle.puzzleType,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = puzzle.description,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Показываем прогресс решения
            LinearProgressIndicator(
                progress = if (puzzle.solutionMoves.isEmpty()) 0f else puzzle.currentMoveIndex.toFloat() / puzzle.solutionMoves.size,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Показываем статус решения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        puzzle.currentMoveIndex >= puzzle.solutionMoves.size -> "Решено"
                        puzzle.currentMoveIndex > 0 -> "В процессе"
                        else -> "Не начато"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "${puzzle.currentMoveIndex}/${puzzle.solutionMoves.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 