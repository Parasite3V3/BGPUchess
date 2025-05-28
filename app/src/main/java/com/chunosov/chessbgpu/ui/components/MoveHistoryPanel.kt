package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chunosov.chessbgpu.model.ChessBoard
import com.chunosov.chessbgpu.util.ChessNotationUtil
import com.chunosov.chessbgpu.viewmodel.Move

/**
 * Компонент для отображения истории ходов в шахматной партии
 */
@Composable
fun MoveHistoryPanel(
    moves: List<Move>,
    modifier: Modifier = Modifier,
    onMoveClicked: (Int) -> Unit = {},
    board: ChessBoard? = null,
    currentMoveIndex: Int = moves.size - 1,
    clickable: Boolean = true
) {
    val listState = rememberLazyListState()
    
    // Автоматическая прокрутка к последнему элементу при добавлении новых ходов
    // или к текущему выбранному ходу
    LaunchedEffect(moves.size, currentMoveIndex) {
        if (moves.isNotEmpty()) {
            val targetIndex = currentMoveIndex / 2 // Делим на 2, т.к. отображаем по 2 хода в строке
            listState.animateScrollToItem(targetIndex.coerceAtMost(moves.size / 2))
        }
    }
    
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            // Заголовок панели
            Text(
                text = "История ходов",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Прокручиваемый список с историей ходов
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Группируем ходы по парам (белые и черные)
                var moveNumber = 1
                var i = 0
                
                while (i < moves.size) {
                    val moveNumberFinal = moveNumber
                    val whiteIndex = i
                    val blackIndex = i + 1
                    
                    item {
                        MoveHistoryRow(
                            moveNumber = moveNumberFinal,
                            whiteIndex = whiteIndex,
                            blackIndex = blackIndex,
                            moves = moves,
                            board = board,
                            currentMoveIndex = currentMoveIndex,
                            onMoveClicked = onMoveClicked,
                            clickable = clickable
                        )
                    }
                    
                    i += 2
                    moveNumber++
                }
            }
        }
    }
}

@Composable
private fun MoveHistoryRow(
    moveNumber: Int,
    whiteIndex: Int,
    blackIndex: Int,
    moves: List<Move>,
    board: ChessBoard?,
    currentMoveIndex: Int,
    onMoveClicked: (Int) -> Unit,
    clickable: Boolean = true
) {
    // Получаем ходы в нотации
    val whiteMove = if (whiteIndex < moves.size) ChessNotationUtil.moveToNotation(moves[whiteIndex], board) else ""
    val blackMove = if (blackIndex < moves.size) ChessNotationUtil.moveToNotation(moves[blackIndex], board) else ""
    
    // Определяем, выделены ли ходы
    val isWhiteHighlighted = whiteIndex == currentMoveIndex
    val isBlackHighlighted = blackIndex == currentMoveIndex
    
    // Определяем цвета из текущей темы
    val rowBackgroundColor = if (moveNumber % 2 == 0) 
        MaterialTheme.colorScheme.surfaceVariant
    else 
        MaterialTheme.colorScheme.surface
    
    // Цвет подсветки активного хода
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // Номер хода
        Text(
            text = "$moveNumber.",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Ход белых
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (isWhiteHighlighted) highlightColor
                    else Color.Transparent
                )
                .let { 
                    // Проверяем, нужно ли делать элемент кликабельным
                    if (whiteIndex < moves.size && clickable) {
                        it.clickable { onMoveClicked(whiteIndex) }
                    } else {
                        it
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = whiteMove,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isWhiteHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Ход черных
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (isBlackHighlighted) highlightColor
                    else Color.Transparent
                )
                .let { 
                    // Проверяем, нужно ли делать элемент кликабельным
                    if (blackIndex < moves.size && clickable) {
                        it.clickable { onMoveClicked(blackIndex) }
                    } else {
                        it
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = blackMove,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBlackHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
} 