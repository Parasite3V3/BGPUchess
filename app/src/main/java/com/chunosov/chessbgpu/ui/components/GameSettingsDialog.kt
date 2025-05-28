package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.GameSettings
import com.chunosov.chessbgpu.model.Opponent
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.model.TimeControl

@Composable
fun GameSettingsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (GameSettings) -> Unit
) {
    var selectedTimeControl by remember { mutableStateOf(TimeControl.NoLimit) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedOpponent by remember { mutableStateOf(Opponent.Friend) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Настройки игры") },
        text = {
            Column {
                Text(
                    text = "Контроль времени:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedTimeControl == TimeControl.NoLimit,
                        onClick = { selectedTimeControl = TimeControl.NoLimit }
                    )
                    Text("Без ограничений", modifier = Modifier.align(Alignment.CenterVertically))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedTimeControl == TimeControl.OneMinute,
                        onClick = { selectedTimeControl = TimeControl.OneMinute }
                    )
                    Text("1 минута", modifier = Modifier.align(Alignment.CenterVertically))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedTimeControl == TimeControl.FiveMinutes,
                        onClick = { selectedTimeControl = TimeControl.FiveMinutes }
                    )
                    Text("5 минут", modifier = Modifier.align(Alignment.CenterVertically))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedTimeControl == TimeControl.TenMinutes,
                        onClick = { selectedTimeControl = TimeControl.TenMinutes }
                    )
                    Text("10 минут", modifier = Modifier.align(Alignment.CenterVertically))
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Цвет фигур:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedColor == PieceColor.WHITE,
                        onClick = { selectedColor = PieceColor.WHITE }
                    )
                    Text("Белые", modifier = Modifier.align(Alignment.CenterVertically))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedColor == PieceColor.BLACK,
                        onClick = { selectedColor = PieceColor.BLACK }
                    )
                    Text("Черные", modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(GameSettings(
                        opponent = selectedOpponent,
                        timeControl = selectedTimeControl,
                        playerColor = selectedColor
                    ))
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
} 