package com.chunosov.chessbgpu.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Диалог подтверждения выхода из игры
 *
 * @param onDismiss Функция, вызываемая при отмене выхода
 * @param onConfirm Функция, вызываемая при подтверждении выхода
 */
@Composable
fun ExitGameDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Завершить игру?") },
        text = { 
            Text(
                "Вы действительно хотите завершить текущую игру? " +
                "Если вы подтвердите выход, партия будет сохранена для последующего просмотра " +
                "в разделе аналитики. Сохраненные партии останутся доступны даже после " +
                "закрытия приложения."
            ) 
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Да, сохранить и выйти")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
} 