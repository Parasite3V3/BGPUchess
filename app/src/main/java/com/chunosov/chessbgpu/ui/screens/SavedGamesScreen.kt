package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.SavedGame
import com.chunosov.chessbgpu.model.TimeControl
import com.chunosov.chessbgpu.viewmodel.ChessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGamesScreen(
    viewModel: ChessViewModel,
    onNavigateToGameReview: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val savedGames by viewModel.savedGames.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        // Загружаем сохраненные игры при открытии экрана
        println("SavedGamesScreen: LaunchedEffect - загружаем сохраненные игры")
        viewModel.loadSavedGames()
    }
    
    // Отслеживаем изменения в списке сохраненных игр
    LaunchedEffect(savedGames) {
        println("SavedGamesScreen: LaunchedEffect(savedGames) - обновлен список игр, размер: ${savedGames.size}")
        savedGames.forEach { game ->
            println("SavedGamesScreen: игра ${game.id}, результат: ${game.result}, ходов: ${game.moveCount}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История партий") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (savedGames.isEmpty()) {
            // Если нет сохраненных партий, показываем сообщение
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "У вас еще нет сохраненных партий",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Сыграйте партию до конца, чтобы она появилась в истории",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Показываем список сохраненных партий
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(savedGames) { game ->
                    SavedGameItem(
                        game = game,
                        onReviewClick = {
                            viewModel.viewSavedGame(game.id)
                            onNavigateToGameReview()
                        },
                        onDeleteClick = {
                            showDeleteConfirmDialog = game.id
                        }
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения удаления партии
    showDeleteConfirmDialog?.let { gameId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Удаление партии") },
            text = { Text("Вы уверены, что хотите удалить эту партию из истории?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedGame(gameId)
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun SavedGameItem(
    game: SavedGame,
    onReviewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onReviewClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Верхняя часть с результатом и датой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.result,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = game.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Детали игры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ходов: ${game.moveCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = if (game.gameOptions.isAgainstBot) "Против компьютера" else "Игра с другом",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Цвет: ${if (game.gameOptions.selectedColor.toString() == "WHITE") "белые" else "черные"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Контроль времени: ${
                            when (game.gameOptions.timeControl) {
                                TimeControl.NoLimit -> "Без ограничения"
                                TimeControl.OneMinute -> "1 минута"
                                TimeControl.FiveMinutes -> "5 минут"
                                TimeControl.TenMinutes -> "10 минут"
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Кнопки действий
                Row {
                    IconButton(onClick = onReviewClick) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Просмотр",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
} 