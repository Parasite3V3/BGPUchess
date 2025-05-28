package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chunosov.chessbgpu.viewmodel.GameState
import com.chunosov.chessbgpu.model.PieceColor

/**
 * Диалоговое окно, которое отображается при завершении игры (мат, пат, истечение времени)
 */
@Composable
fun GameEndDialog(
    gameState: GameState,
    onSaveGame: () -> Unit,
    onBackToMenu: () -> Unit,
    isSaved: Boolean = false
) {
    Dialog(
        onDismissRequest = { }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Игра окончена",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val resultText = when (gameState) {
                    is GameState.Checkmate -> {
                        val loser = gameState.loser
                        if (loser == com.chunosov.chessbgpu.model.PieceColor.WHITE) "Мат! Черные выиграли!" else "Мат! Белые выиграли!"
                    }
                    is GameState.Stalemate -> "Пат! Ничья"
                    GameState.WHITE_WINS_BY_TIME -> "Белые выиграли по времени"
                    GameState.BLACK_WINS_BY_TIME -> "Черные выиграли по времени"
                    GameState.Draw -> "Ничья!"
                    is GameState.Resigned -> {
                        val resignedColor = gameState.resignedColor
                        if (resignedColor == com.chunosov.chessbgpu.model.PieceColor.WHITE) "Белые сдались! Черные выиграли!" else "Черные сдались! Белые выиграли!"
                    }
                    else -> "Игра завершена"
                }
                
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onSaveGame,
                        enabled = !isSaved,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isSaved) "Партия сохранена" else "Сохранить партию")
                    }
                    
                    Button(
                        onClick = onBackToMenu
                    ) {
                        Text("В меню")
                    }
                }
            }
        }
    }
}

/**
 * Возвращает заголовок диалогового окна в зависимости от состояния игры
 */
@Composable
private fun getDialogTitle(gameState: GameState): String {
    return when (gameState) {
        is GameState.Checkmate -> {
            if (gameState.loser == PieceColor.WHITE) "Поражение!" else "Победа!"
        }
        GameState.Stalemate -> "Пат! Ничья!"
        GameState.WHITE_WINS_BY_TIME, GameState.BLACK_WINS_BY_TIME -> "Победа по времени!"
        GameState.Draw -> "Ничья!"
        is GameState.Resigned -> "Игрок сдался!"
        else -> "Игра завершена!"
    }
}

/**
 * Возвращает цвет заголовка диалогового окна в зависимости от состояния игры
 */
@Composable
private fun getDialogTitleColor(gameState: GameState): Color {
    return when (gameState) {
        is GameState.Checkmate -> Color(0xFFFF6347) // Томатный для мата
        GameState.WHITE_WINS_BY_TIME, GameState.BLACK_WINS_BY_TIME -> Color(0xFFFF6347)
        GameState.Stalemate, GameState.Draw -> Color(0xFF4CAF50) // Зеленый для ничьи
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Возвращает цвет кнопки в зависимости от состояния игры
 */
@Composable
private fun getButtonColor(gameState: GameState): Color {
    return when (gameState) {
        is GameState.Checkmate -> Color(0xFFFF6347) // Томатный для мата
        GameState.WHITE_WINS_BY_TIME, GameState.BLACK_WINS_BY_TIME -> Color(0xFFFF6347)
        GameState.Stalemate, GameState.Draw -> Color(0xFF4CAF50) // Зеленый для ничьи
        else -> MaterialTheme.colorScheme.primary
    }
} 