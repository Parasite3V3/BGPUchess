package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.GameOptions
import com.chunosov.chessbgpu.model.Opponent
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.model.TimeControl
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

@Composable
fun GameOptionsScreen(
    viewModel: ChessViewModel,
    onStartGame: () -> Unit
) {
    var selectedOpponent by remember { mutableStateOf(Opponent.Bot) }
    var selectedTimeControl by remember { mutableStateOf(TimeControl.NoLimit) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var isAgainstBot by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Настройки игры",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Выбор противника
        Text(
            text = "Выберите противника:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { selectedOpponent = Opponent.Bot },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOpponent == Opponent.Bot) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (selectedOpponent == Opponent.Bot)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Играть с ботом")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { selectedOpponent = Opponent.Friend },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOpponent == Opponent.Friend) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (selectedOpponent == Opponent.Friend)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Играть с другом")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Выбор контроля времени
        Text(
            text = "Выберите контроль времени:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Улучшенное отображение контроля времени
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Первый ряд кнопок времени
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeControlButton(
                        label = "∞",
                        isSelected = selectedTimeControl == TimeControl.NoLimit,
                        onClick = { selectedTimeControl = TimeControl.NoLimit },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TimeControlButton(
                        label = "1 мин.",
                        isSelected = selectedTimeControl == TimeControl.OneMinute,
                        onClick = { selectedTimeControl = TimeControl.OneMinute },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TimeControlButton(
                        label = "5 мин.",
                        isSelected = selectedTimeControl == TimeControl.FiveMinutes,
                        onClick = { selectedTimeControl = TimeControl.FiveMinutes },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Второй ряд кнопок времени
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeControlButton(
                        label = "10 мин.",
                        isSelected = selectedTimeControl == TimeControl.TenMinutes,
                        onClick = { selectedTimeControl = TimeControl.TenMinutes },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Добавляем пустые место для выравнивания
                    Box(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    // Пустое пространство для выравнивания
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Выбор цвета фигур
        Text(
            text = "Выберите цвет фигур:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { selectedColor = PieceColor.WHITE },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedColor == PieceColor.WHITE) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selectedColor == PieceColor.WHITE)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Белые")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { selectedColor = PieceColor.BLACK },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedColor == PieceColor.BLACK) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selectedColor == PieceColor.BLACK)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Черные")
                }
            }
        }
        
        // Информация о том, что будет изменена ориентация доски
        AnimatedVisibility(
            visible = selectedColor == PieceColor.BLACK,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Доска будет повернута так, чтобы черные фигуры были внизу",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка начала игры
        Button(
            onClick = {
                viewModel.setGameOptions(
                    selectedOpponent,
                    selectedTimeControl,
                    selectedColor
                )
                viewModel.resetGame()
                viewModel.applyGameSettings()
                onStartGame()
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Начать игру",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // Дополнительное пространство внизу, чтобы кнопка не прижималась к краю экрана
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TimeControlButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
} 