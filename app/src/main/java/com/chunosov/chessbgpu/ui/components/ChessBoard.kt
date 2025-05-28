package com.chunosov.chessbgpu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chunosov.chessbgpu.R
import com.chunosov.chessbgpu.model.ChessBoard
import com.chunosov.chessbgpu.model.ChessPiece
import com.chunosov.chessbgpu.model.PieceColor
import com.chunosov.chessbgpu.model.PieceType
import com.chunosov.chessbgpu.model.Position
import com.chunosov.chessbgpu.viewmodel.ChessViewModel
import android.util.Log

@Composable
fun ChessBoard(
    board: ChessBoard,
    selectedPosition: Position?,
    possibleMoves: Set<Position>,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    isBlackBottom: Boolean = board is com.chunosov.chessbgpu.model.BlackChangeBoard,
    kingInCheck: Position? = null,
    lightSquareColor: Color = Color(0xFFF0D9B5), // светлые клетки - бежевый
    darkSquareColor: Color = Color(0xFFB58863),   // темные клетки - коричневый
    isInteractive: Boolean = true // Параметр для указания, можно ли взаимодействовать с доской
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.9f)
        ) {
            Column {
                // Рендерим ряды доски
                for (row in 8 downTo 1) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Отображаем номера рядов слева
                        Box(
                            modifier = Modifier
                                .weight(0.1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Если черные снизу, нумерация рядов инвертируется (1 сверху, 8 снизу)
                            val displayRowNumber = if (isBlackBottom) 9 - row else row
                            Text(
                                text = displayRowNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        for (col in 0..7) {
                            // Определяем, как отображать столбцы в зависимости от ориентации доски
                            val adjustedCol = if (isBlackBottom) 7 - col else col
                            
                            // Создаем объект позиции
                            val position = Position(8 - row, adjustedCol)
                            
                            // Определяем, выделена ли текущая позиция
                            val isSelected = selectedPosition?.let { it.row == position.row && it.col == position.col } ?: false
                            val isPossibleMove = position in possibleMoves
                            
                            // Определяем, является ли сумма координат четной для определения цвета клетки
                            // НО учитываем инверсию цветов при игре за черных (isBlackBottom = true)
                            val posSumParityIsEven = (position.row + position.col) % 2 == 0
                            // При игре за черных инвертируем четность для корректного отображения цветов
                            val isSumEven = if (isBlackBottom) !posSumParityIsEven else posSumParityIsEven
                            
                            // Цвета для выделения клеток
                            val selectionColor = Color(0xFF769656).copy(alpha = 0.8f)
                            // Цвет для подсветки возможных ходов зависит от инвертированного цвета клетки
                            val possibleMoveColor = if (!isSumEven)
                                Color(0xFFBBCB44).copy(alpha = 0.7f) else
                                Color(0xFFCCDA64).copy(alpha = 0.9f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        when {
                                            isSelected -> selectionColor
                                            isPossibleMove -> possibleMoveColor
                                            isSumEven -> lightSquareColor // Четная сумма -> светлая клетка
                                            else -> darkSquareColor      // Нечетная сумма -> темная клетка
                                        }
                                    )
                                    .let { mod ->
                                        // Делаем клетки кликабельными только если доска интерактивна
                                        if (isInteractive) {
                                            mod.clickable { onSquareClick(position) }
                                        } else {
                                            mod
                                        }
                                    }
                            ) {
                                // Отображение метки возможного хода
                                if (isPossibleMove) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.Center)
                                            .background(
                                                color = Color(0xFF769656).copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                
                                // Фигура
                                val piece = board.getPiece(position)
                                piece?.let { piece ->
                                    val isKingInCheck = kingInCheck != null && 
                                                      position.row == kingInCheck.row && 
                                                      position.col == kingInCheck.col &&
                                                      piece.type == PieceType.KING
                                    
                                    ChessPieceView(
                                        piece = piece,
                                        isDarkSquare = !isSumEven, // isDarkSquare передаем инвертированную четность
                                        isKingInCheck = isKingInCheck,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                    )
                                }
                                
                                // Добавляем нумерацию в левый верхний угол для первого (левого) столбца
                                // В зависимости от isBlackBottom меняется, какой столбец слева
                                if (col == 0) { // Это самый левый столбец (a или h в зависимости от isBlackBottom)
                                    val textColor = if (isSumEven) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                                    val displayRowNumberForCell = if (isBlackBottom) 9 - row else row
                                    
                                    Text(
                                        text = displayRowNumberForCell.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(2.dp)
                                    )
                                }
                                
                                // Добавляем буквенные обозначения внизу доски для последнего ряда
                                if (row == 1) {
                                    // Определяем букву столбца в зависимости от ориентации доски
                                    // При игре за черных буквы идут в обратном порядке: справа 'a', слева 'h'
                                    val columnLetter = if (isBlackBottom) ('h' - col).toString() else ('a' + col).toString()
                                    
                                    // Определяем цвет текста в зависимости от цвета клетки
                                    val textColor = if (isSumEven) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                                    
                                    Text(
                                        text = columnLetter,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessPieceView(
    piece: ChessPiece,
    isDarkSquare: Boolean, // Теперь это означает "является ли клетка визуально темной"
    isKingInCheck: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pieceColor = if (piece.color == PieceColor.WHITE) {
        Color.White
    } else {
        Color.Black
    }
    
    // Определяем цвет фона для фигуры для лучшего контраста
    val backgroundCircleColor = if (isDarkSquare) { // Если клетка темная
        if (piece.color == PieceColor.WHITE) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    } else { // Если клетка светлая
        if (piece.color == PieceColor.WHITE) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f)
    }
    
    // Определяем цвет тени для фигуры
    val shadowColor = if (piece.color == PieceColor.WHITE) {
        Color.Black.copy(alpha = 0.5f) // Увеличиваем непрозрачность тени для белых фигур
    } else {
        Color.Black.copy(alpha = 0.3f)
    }
    
    // Добавляем красную подсветку для короля под шахом
    val modifierWithCheckHighlight = if (isKingInCheck) {
        modifier.drawBehind {
            drawCircle(
                color = Color(0xFFE57373).copy(alpha = 0.5f), // Полупрозрачный красноватый цвет
                radius = size.minDimension / 2
            )
        }
    } else {
        modifier
    }
    
    Box(
        modifier = modifierWithCheckHighlight,
        contentAlignment = Alignment.Center
    ) {
        // Фон для фигуры для лучшего контраста
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = backgroundCircleColor, shape = CircleShape)
        )
        
        // Если король под шахом, рисуем красную подсветку
        if (isKingInCheck) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = 3.dp,
                        color = Color.Red.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
        
        // Тень для фигуры - делаем ее более заметной
        Text(
            text = piece.toUnicode(),
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = shadowColor,
            modifier = Modifier
                .offset(x = 1.dp, y = 1.dp)
        )
        
        // Сама фигура
        Text(
            text = piece.toUnicode(),
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = pieceColor
        )
    }
}

private fun formatTime(timeMillis: Long): String {
    val minutes = timeMillis / 60000
    val seconds = (timeMillis % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
} 