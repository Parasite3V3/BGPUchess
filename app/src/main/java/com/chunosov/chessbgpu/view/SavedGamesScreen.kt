package com.chunosov.chessbgpu.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.SavedGame
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGamesScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit,
    onSelectGame: (SavedGame) -> Unit
) {
    val savedGames by viewModel.savedGames.collectAsState()
    var gameToDelete by remember { mutableStateOf<SavedGame?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История партий") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            if (savedGames.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "У вас пока нет истории партий",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedGames) { game ->
                        SavedGameItem(
                            game = game,
                            onGameClick = { onSelectGame(game) },
                            onDeleteClick = { gameToDelete = game }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    gameToDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = { Text("Удалить партию?") },
            text = { 
                Text("Вы уверены, что хотите удалить эту партию? Это действие невозможно отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavedGame(game.id)
                        gameToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { gameToDelete = null }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun SavedGameItem(
    game: SavedGame,
    onGameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(game.timestamp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGameClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Ходов: ${game.moveCount}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (game.result.isNotEmpty()) {
                Text(
                    text = "Результат: ${game.result}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
} 