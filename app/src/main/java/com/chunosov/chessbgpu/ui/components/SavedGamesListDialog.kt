package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.SavedGame

/**
 * Диалог для отображения списка сохраненных игр
 *
 * @param savedGames Список сохраненных игр
 * @param onDismiss Обработчик закрытия диалога
 * @param onGameSelected Обработчик выбора игры для просмотра
 * @param onGameDeleted Обработчик удаления игры
 */
@Composable
fun SavedGamesListDialog(
    savedGames: List<SavedGame>,
    onDismiss: () -> Unit,
    onGameSelected: (String) -> Unit,
    onGameDeleted: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("История партий") },
        text = {
            if (savedGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет истории партий")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(savedGames) { game ->
                        SavedGameItem(
                            game = game,
                            onGameSelected = onGameSelected,
                            onGameDeleted = onGameDeleted
                        )
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

/**
 * Элемент списка сохраненных игр
 */
@Composable
private fun SavedGameItem(
    game: SavedGame,
    onGameSelected: (String) -> Unit,
    onGameDeleted: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
        
        IconButton(onClick = { onGameSelected(game.id) }) {
            Text("Просмотр")
        }
        
        IconButton(onClick = { onGameDeleted(game.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить партию")
        }
    }
} 