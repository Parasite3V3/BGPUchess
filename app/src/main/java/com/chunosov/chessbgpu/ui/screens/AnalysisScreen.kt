package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import com.chunosov.chessbgpu.model.SavedGame
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

@Composable
fun AnalysisScreen(
    viewModel: ChessViewModel = viewModel(),
    onGameSelected: (String) -> Unit = {},
    onNavigateToSavedGames: () -> Unit = {}
) {
    val savedGames by viewModel.savedGames.collectAsState(emptyList())
    
    // Состояние для отслеживания выбранной вкладки
    var selectedTab by remember { mutableStateOf(AnalysisTab.SAVED_GAMES) }
    
    // Загружаем сохраненные игры при открытии экрана
    LaunchedEffect(Unit) {
        // Добавляем задержку для обеспечения корректной загрузки
        delay(300)
        println("Принудительная загрузка сохраненных игр на экране анализа")
        viewModel.loadSavedGames()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Анализ партий",
                style = MaterialTheme.typography.headlineLarge
            )
            
            // Кнопка для перехода к полному экрану истории партий
            Button(onClick = onNavigateToSavedGames) {
                Text("Все партии")
            }
        }
        
        // Табы для переключения между режимами
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnalysisTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) },
                    icon = { 
                        Icon(
                            imageVector = when(tab) {
                                AnalysisTab.BOARD_EDITOR -> Icons.Default.Edit
                                AnalysisTab.SAVED_GAMES -> Icons.Default.PlayArrow
                            },
                            contentDescription = tab.title
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Содержимое выбранной вкладки
        when (selectedTab) {
            AnalysisTab.BOARD_EDITOR -> {
                BoardEditorContent()
            }
            AnalysisTab.SAVED_GAMES -> {
                SavedGamesContent(
                    savedGames = savedGames,
                    onGameSelected = onGameSelected,
                    onGameDeleted = { viewModel.deleteSavedGame(it) }
                )
            }
        }
    }
}

/**
 * Содержимое вкладки редактора доски
 */
@Composable
private fun BoardEditorContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Редактор доски",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Для доступа к редактору доски, пожалуйста, используйте кнопку 'Редактор доски' в главном меню.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Содержимое вкладки сохраненных игр
 */
@Composable
private fun SavedGamesContent(
    savedGames: List<SavedGame>,
    onGameSelected: (String) -> Unit,
    onGameDeleted: (String) -> Unit
) {
    if (savedGames.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет сохраненных партий для анализа",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(savedGames) { game ->
                AnalysisSavedGameItem(
                    game = game,
                    onGameSelected = { onGameSelected(game.id) },
                    onGameDeleted = { onGameDeleted(game.id) }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun AnalysisSavedGameItem(
    game: SavedGame,
    onGameSelected: () -> Unit,
    onGameDeleted: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onGameSelected)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Партия от ${game.formattedDate}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ходов: ${game.moveCount}, ${
                        if (game.gameOptions.selectedColor.name == "WHITE") 
                            "Белые фигуры" 
                        else 
                            "Черные фигуры"
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                if (game.result.isNotEmpty()) {
                    Text(
                        text = "Результат: ${game.result}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(onClick = onGameSelected) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Просмотреть партию")
            }
            
            IconButton(onClick = onGameDeleted) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить партию")
            }
        }
    }
}

/**
 * Перечисление доступных вкладок в экране анализа
 */
private enum class AnalysisTab(val title: String) {
    BOARD_EDITOR("Редактор доски"),
    SAVED_GAMES("Сохраненные игры")
} 