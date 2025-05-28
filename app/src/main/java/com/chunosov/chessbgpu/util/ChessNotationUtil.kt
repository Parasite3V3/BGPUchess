package com.chunosov.chessbgpu.util

import com.chunosov.chessbgpu.model.BlackChangeBoard
import com.chunosov.chessbgpu.model.ChessBoard
import com.chunosov.chessbgpu.model.ChessPiece
import com.chunosov.chessbgpu.model.PieceType
import com.chunosov.chessbgpu.model.Position
import com.chunosov.chessbgpu.model.WhiteChangeBoard
import com.chunosov.chessbgpu.viewmodel.Move
import kotlin.math.abs

/**
 * Утилитарный класс для работы с шахматной нотацией
 */
object ChessNotationUtil {
    
    /**
     * Преобразует ход в стандартную шахматную нотацию
     */
    fun moveToNotation(move: Move, board: ChessBoard? = null): String {
        val piece = move.piece
        val from = move.from
        val to = move.to
        val isCapture = move.capturedPiece != null || move.isEnPassant
        
        // Базовая строка хода
        val notation = StringBuilder()
        
        // Особый случай - рокировка
        if (piece.type == PieceType.KING && abs(from.col - to.col) == 2) {
            return if (to.col > from.col) "O-O" + getCheckAnnotation(move) else "O-O-O" + getCheckAnnotation(move)
        }
        
        // Добавляем символ фигуры (кроме пешки)
        if (piece.type != PieceType.PAWN) {
            notation.append(getPieceNotation(piece.type))
        }
        
        // Для взятия пешкой указываем вертикаль исходной позиции
        if (piece.type == PieceType.PAWN && isCapture) {
            notation.append(fileToLetter(from.col, board))
        }
        
        // Добавляем символ взятия
        if (isCapture) {
            notation.append("x")
        }
        
        // Добавляем клетку назначения
        notation.append(positionToNotation(to, board))
        
        // Для превращения пешки добавляем =<фигура>
        if (move.isPromotion && move.promotedTo != null) {
            notation.append("=")
            notation.append(getPieceNotation(move.promotedTo))
        }
        
        // Добавляем шах/мат
        notation.append(getCheckAnnotation(move))
        
        return notation.toString()
    }
    
    /**
     * Возвращает аннотацию шаха или мата для хода
     */
    private fun getCheckAnnotation(move: Move): String {
        return when {
            move.isCheckmate -> "#"
            move.isCheck -> "+"
            else -> ""
        }
    }
    
    /**
     * Форматирует список ходов в шахматную партию
     */
    fun formatMovesList(moves: List<Move>, board: ChessBoard? = null): String {
        val result = StringBuilder()
        
        // Группируем ходы по парам (белые и черные)
        var moveNumber = 1
        var i = 0
        while (i < moves.size) {
            result.append("$moveNumber. ")
            
            // Ход белых
            if (i < moves.size) {
                result.append(moveToNotation(moves[i], board))
                i++
            }
            
            // Ход черных
            if (i < moves.size) {
                result.append(" ")
                result.append(moveToNotation(moves[i], board))
                i++
            }
            
            result.append("\n")
            moveNumber++
        }
        
        return result.toString()
    }
    
    /**
     * Получает обозначение фигуры для нотации
     */
    private fun getPieceNotation(type: PieceType): String {
        return when (type) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            else -> ""
        }
    }
    
    /**
     * Преобразует позицию в шахматную нотацию (например, e4)
     */
    private fun positionToNotation(position: Position, board: ChessBoard? = null): String {
        val file = fileToLetter(position.col, board)
        val rank = getRankNumber(position.row, board)
        return "$file$rank"
    }
    
    /**
     * Преобразует номер ряда в номер ранга для нотации
     */
    private fun getRankNumber(row: Int, board: ChessBoard? = null): Int {
        return when (board) {
            is BlackChangeBoard -> row + 1      // 0->1, 1->2, ..., 7->8
            is WhiteChangeBoard -> 8 - row      // 0->8, 1->7, ..., 7->1
            else -> 8 - row                     // 0->8, 1->7, ..., 7->1 (стандартная доска)
        }
    }
    
    /**
     * Преобразует индекс столбца в букву файла
     */
    private fun fileToLetter(col: Int, board: ChessBoard? = null): Char {
        return when (board) {
            is BlackChangeBoard -> 'h' - col    // 0->h, 1->g, ..., 7->a
            is WhiteChangeBoard -> 'a' + col    // 0->a, 1->b, ..., 7->h
            else -> 'a' + col                   // 0->a, 1->b, ..., 7->h (стандартная доска)
        }
    }
} 