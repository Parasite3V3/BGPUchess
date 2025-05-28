package com.chunosov.chessbgpu.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chunosov.chessbgpu.model.PieceColor

@Composable
fun ResignConfirmationDialog(
    currentTurn: PieceColor,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val playerColor = if (currentTurn == PieceColor.WHITE) "белых" else "черных"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтверждение сдачи") },
        text = { Text("Вы уверены, что хотите сдаться? Это приведет к победе соперника и завершению партии.") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Да, сдаться")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
} 