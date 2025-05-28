package com.chunosov.chessbgpu.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Диалог подтверждения начала новой игры
 *
 * @param onDismiss Функция, вызываемая при отмене начала новой игры
 * @param onConfirm Функция, вызываемая при подтверждении начала новой игры
 */
@Composable
fun NewGameConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Начать новую игру?") },
        text = { 
            Text(
                "Вы действительно хотите начать новую игру? " +
                "Текущая партия будет сохранена для последующего просмотра " +
                "в разделе аналитики. Начать новую игру?"
            ) 
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Да, начать новую игру")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
} 