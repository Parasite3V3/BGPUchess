package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.model.PieceType

@Composable
fun PawnPromotionDialog(
    pawnColor: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Выберите фигуру",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Ферзь
                    PromotionPieceItem(
                        pawnColor = pawnColor,
                        pieceType = PieceType.QUEEN,
                        onClick = { onPieceSelected(PieceType.QUEEN) }
                    )
                    
                    // Ладья
                    PromotionPieceItem(
                        pawnColor = pawnColor,
                        pieceType = PieceType.ROOK,
                        onClick = { onPieceSelected(PieceType.ROOK) }
                    )
                    
                    // Слон
                    PromotionPieceItem(
                        pawnColor = pawnColor,
                        pieceType = PieceType.BISHOP,
                        onClick = { onPieceSelected(PieceType.BISHOP) }
                    )
                    
                    // Конь
                    PromotionPieceItem(
                        pawnColor = pawnColor,
                        pieceType = PieceType.KNIGHT,
                        onClick = { onPieceSelected(PieceType.KNIGHT) }
                    )
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun PromotionPieceItem(
    pawnColor: PieceColor,
    pieceType: PieceType,
    onClick: () -> Unit
) {
    val pieceSymbol = when (pieceType) {
        PieceType.QUEEN -> if (pawnColor == PieceColor.WHITE) "♕" else "♛"
        PieceType.ROOK -> if (pawnColor == PieceColor.WHITE) "♖" else "♜"
        PieceType.BISHOP -> if (pawnColor == PieceColor.WHITE) "♗" else "♝"
        PieceType.KNIGHT -> if (pawnColor == PieceColor.WHITE) "♘" else "♞"
        else -> ""
    }
    
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(Color.LightGray)
            .border(width = 1.dp, color = Color.Gray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pieceSymbol,
            fontSize = 40.sp,
            color = if (pawnColor == PieceColor.WHITE) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )
    }
} 