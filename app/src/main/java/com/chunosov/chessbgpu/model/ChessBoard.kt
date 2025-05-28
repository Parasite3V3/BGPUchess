package com.chunosov.chessbgpu.model

import kotlin.math.abs
import android.util.Log

/**
 * Класс для хранения позиций на шахматной доске (0..7, 0..7)
 */
data class Position(val row: Int, val col: Int) {
    
    /**
     * Проверяет, находится ли позиция в пределах доски
     * @return true, если позиция валидна (0-7, 0-7)
     */
    fun isValid(): Boolean {
        return row in 0..7 && col in 0..7
    }
    
    /**
     * Преобразует позицию в алгебраическую нотацию (например, "e4")
     * @return строка в алгебраической нотации
     */
    fun toAlgebraicNotation(): String {
        val file = ('a' + col).toString()
        val rank = (8 - row).toString()
        return "$file$rank"
    }
    
    companion object {
        /**
         * Создает объект Position из алгебраической нотации
         * @param notation строка в алгебраической нотации (например, "e4")
         * @return объект Position или null, если нотация некорректна
         */
        fun fromAlgebraicNotation(notation: String): Position? {
            if (notation.length != 2) return null
            
            val file = notation[0] - 'a'
            val rank = 8 - (notation[1] - '0')
            
            if (file !in 0..7 || rank !in 0..7) return null
            
            return Position(rank, file)
        }
    }
}

/**
 * Базовый класс шахматной доски, представляющий игровое поле и логику передвижения фигур
 */
open class ChessBoard {
    // Двумерный массив для хранения фигур на доске
    protected val board = Array(8) { Array<ChessPiece?>(8) { null } }
    
    // Информация о последнем двойном ходе пешки для правила взятия на проходе
    protected var lastPawnDoubleMove: Position? = null
    
    // Позиция для взятия на проходе, если есть возможность
    var enPassantTarget: Position? = null
        protected set
    
    /**
     * Очищает доску, удаляя все фигуры
     */
    fun clearBoard() {
        for (row in 0..7) {
            for (col in 0..7) {
                setPiece(Position(row, col), null)
            }
        }
        lastPawnDoubleMove = null
        enPassantTarget = null
    }
    
    /**
     * Получает фигуру на указанной позиции
     * @param position позиция на доске
     * @return фигура или null, если клетка пуста или позиция недопустима
     */
    fun getPiece(position: Position): ChessPiece? {
        if (!position.isValid()) return null
        return board[position.row][position.col]
    }
    
    /**
     * Устанавливает фигуру на указанную позицию
     * @param position позиция на доске
     * @param piece фигура или null для удаления фигуры
     */
    fun setPiece(position: Position, piece: ChessPiece?) {
        if (!position.isValid()) return
        board[position.row][position.col] = piece
    }
    
    /**
     * Выполняет ход фигурой
     * @param from начальная позиция
     * @param to конечная позиция
     * @param isBlackBottom true, если черные фигуры внизу доски
     * @return true, если ход был успешно выполнен
     */
    open fun movePiece(from: Position, to: Position, isBlackBottom: Boolean = false): Boolean {
        if (!from.isValid() || !to.isValid()) return false
        
        val piece = getPiece(from) ?: return false
        
        // Определяем, является ли доска с черными внизу
        val isActuallyBlackBottom = this is BlackChangeBoard || isBlackBottom
        
        // Обнуляем цель для взятия на проходе при каждом ходе
        val wasEnPassantMove = isEnPassantMove(from, to, piece)
        
        // Сохраняем информацию о ходе пешки на два поля для возможного взятия на проходе
        val isPawnDoubleMove = piece.type == PieceType.PAWN && abs(to.row - from.row) == 2
        if (isPawnDoubleMove) {
            // Определяем направление движения в зависимости от типа доски и цвета пешки
            val direction = if (isActuallyBlackBottom) {
                // Для BlackChangeBoard (черные внизу)
                if (piece.color == PieceColor.WHITE) 1 else -1
            } else {
                // Для WhiteChangeBoard и стандартной доски (белые внизу)
                if (piece.color == PieceColor.WHITE) -1 else 1
            }
                
            enPassantTarget = Position(from.row + direction, from.col)
            lastPawnDoubleMove = to
        } else {
            enPassantTarget = null
        }
        
        // Обработка взятия на проходе
        if (wasEnPassantMove) {
            val capturedPawnRow = from.row
            val capturedPawnCol = to.col
            setPiece(Position(capturedPawnRow, capturedPawnCol), null)
        }
        
        // Обработка рокировки
        if (piece.type == PieceType.KING && abs(to.col - from.col) == 2) {
            val isKingSide = to.col > from.col
            val rookFromCol = if (isKingSide) 7 else 0
            val rookToCol = if (isKingSide) 5 else 3
            val rookRow = from.row
            val rookFromPos = Position(rookRow, rookFromCol)
            val rookToPos = Position(rookRow, rookToCol)
            
            // Получаем ладью и перемещаем её
            val rook = getPiece(rookFromPos)
            if (rook != null && rook.type == PieceType.ROOK) {
                // Сначала перемещаем короля
                setPiece(from, null)
                setPiece(to, piece)
                piece.hasMoved = true
                
                // Затем перемещаем ладью
                setPiece(rookFromPos, null)
                setPiece(rookToPos, rook)
                rook.hasMoved = true
                
                return true
            }
            return false
        }
        
        // Обычное перемещение фигуры
        val targetPiece = getPiece(to)
        
        // Обработка превращения пешки
        if (piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
            // Превращение пешки в ферзя по умолчанию
            // В реальной игре здесь будет запрос к пользователю
            setPiece(from, null)
            setPiece(to, Queen(piece.color))
        } else {
            // Обычный ход
            setPiece(from, null)
            setPiece(to, piece)
            piece.hasMoved = true
        }
        
        return true
    }
    
    /**
     * Проверяет, является ли движение взятием на проходе
     * @param from начальная позиция
     * @param to конечная позиция
     * @param piece передвигаемая фигура
     * @return true, если это взятие на проходе
     */
    private fun isEnPassantMove(from: Position, to: Position, piece: ChessPiece): Boolean {
        if (piece.type != PieceType.PAWN) return false
        if (from.col == to.col) return false // Не диагональный ход
        
        val targetPiece = getPiece(to)
        // Если на целевой клетке нет фигуры, и ход по диагонали - возможно взятие на проходе
        if (targetPiece == null && enPassantTarget != null && to == enPassantTarget) {
            return true
        }
        
        return false
    }
    
    /**
     * Проверяет, является ли ход допустимым согласно правилам шахмат
     * @param from начальная позиция
     * @param to конечная позиция
     * @param isBlackBottom true, если черные фигуры внизу доски
     * @return true, если ход допустим
     */
    fun isValidMove(from: Position, to: Position, isBlackBottom: Boolean = true): Boolean {
        if (!from.isValid() || !to.isValid()) return false
        
        val piece = getPiece(from) ?: return false
        val targetPiece = getPiece(to)
        
        // Проверяем, что целевая клетка либо пуста, либо содержит фигуру противоположного цвета
        if (targetPiece != null && targetPiece.color == piece.color) {
            return false
        }
        
        // Проверяем базовые правила движения для каждого типа фигур
        val isValidBasicMove = when (piece.type) {
            PieceType.PAWN -> isPawnMoveValid(from, to, piece.color, isBlackBottom)
            PieceType.KNIGHT -> isKnightMoveValid(from, to)
            PieceType.BISHOP -> isBishopMoveValid(from, to)
            PieceType.ROOK -> isRookMoveValid(from, to)
            PieceType.QUEEN -> isQueenMoveValid(from, to)
            PieceType.KING -> isKingMoveValid(from, to) || isCastlingValid(from, to, isBlackBottom)
        }
        
        if (!isValidBasicMove) return false
        
        // Проверяем, не перепрыгивает ли фигура через другие фигуры
        return when (piece.type) {
            PieceType.KNIGHT -> true // Конь может перепрыгивать
            PieceType.PAWN -> true // У пешки отдельная проверка в isPawnMoveValid
            else -> isPathClear(from, to)
        }
    }
    
    /**
     * Проверяет, не перекрыт ли путь между двумя позициями другими фигурами
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если путь свободен
     */
    protected fun isPathClear(from: Position, to: Position): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        
        // Направление движения
        val rowDir = when {
            rowDiff > 0 -> 1
            rowDiff < 0 -> -1
            else -> 0
        }
        
        val colDir = when {
            colDiff > 0 -> 1
            colDiff < 0 -> -1
            else -> 0
        }
        
        var currentRow = from.row + rowDir
        var currentCol = from.col + colDir
        
        // Проверяем все клетки на пути
        while (currentRow != to.row || currentCol != to.col) {
            if (getPiece(Position(currentRow, currentCol)) != null) {
                return false // Путь перекрыт
            }
            currentRow += rowDir
            currentCol += colDir
        }
        
        return true
    }
    
    /**
     * Проверяет валидность хода пешки
     * @param from начальная позиция
     * @param to конечная позиция
     * @param color цвет пешки
     * @param isBlackBottom true, если черные фигуры внизу доски
     * @return true, если ход допустим
     */
    protected open fun isPawnMoveValid(from: Position, to: Position, color: PieceColor, isBlackBottom: Boolean): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val piece = getPiece(from) ?: return false
        
        // Направление движения пешек определяется цветом пешки и типом доски
        // Белые пешки на WhiteChangeBoard идут вверх (row уменьшается), а на BlackChangeBoard - вниз (row увеличивается)
        // Черные пешки на WhiteChangeBoard идут вниз (row увеличивается), а на BlackChangeBoard - вверх (row уменьшается)
        val direction = if (this is BlackChangeBoard) {
            // Для BlackChangeBoard (черные внизу)
            if (color == PieceColor.WHITE) 1 else -1
        } else {
            // Для WhiteChangeBoard и стандартной доски (белые внизу)
            if (color == PieceColor.WHITE) -1 else 1
        }
        
        // Определяем начальный ряд пешек
        val startRank = if (this is BlackChangeBoard) {
            // Для BlackChangeBoard (черные внизу)
            if (color == PieceColor.WHITE) 1 else 6
        } else {
            // Для WhiteChangeBoard и стандартной доски (белые внизу)
            if (color == PieceColor.WHITE) 6 else 1
        }
        
        // Обычный ход на одну клетку вперед
        if (colDiff == 0 && rowDiff == direction && getPiece(to) == null) {
            return true
        }
        
        // Ход на две клетки вперед с начальной позиции
        if (colDiff == 0 && 
            from.row == startRank && 
            rowDiff == 2 * direction && 
            getPiece(to) == null && 
            getPiece(Position(from.row + direction, from.col)) == null) {
            return true
        }
        
        // Взятие по диагонали
        if (abs(colDiff) == 1 && rowDiff == direction) {
            val targetPiece = getPiece(to)
            
            // Обычное взятие
            if (targetPiece != null && targetPiece.color != color) {
                return true
            }
            
            // Взятие на проходе
            if (targetPiece == null && enPassantTarget != null && to == enPassantTarget) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Проверяет валидность хода коня
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если ход допустим
     */
    private fun isKnightMoveValid(from: Position, to: Position): Boolean {
        val rowDiff = abs(to.row - from.row)
        val colDiff = abs(to.col - from.col)
        
        // Конь ходит буквой "Г": 2 клетки в одном направлении и 1 в другом
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }
    
    /**
     * Проверяет валидность хода слона
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если ход допустим
     */
    private fun isBishopMoveValid(from: Position, to: Position): Boolean {
        val rowDiff = abs(to.row - from.row)
        val colDiff = abs(to.col - from.col)
        
        // Слон ходит только по диагонали (равное изменение по рядам и колонкам)
        return rowDiff == colDiff && rowDiff > 0
    }
    
    /**
     * Проверяет валидность хода ладьи
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если ход допустим
     */
    private fun isRookMoveValid(from: Position, to: Position): Boolean {
        // Ладья ходит только по горизонтали или вертикали
        return (from.row == to.row && from.col != to.col) || 
               (from.col == to.col && from.row != to.row)
    }
    
    /**
     * Проверяет валидность хода ферзя
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если ход допустим
     */
    private fun isQueenMoveValid(from: Position, to: Position): Boolean {
        // Ферзь может ходить как слон или как ладья
        return isBishopMoveValid(from, to) || isRookMoveValid(from, to)
    }
    
    /**
     * Проверяет валидность хода короля
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если ход допустим
     */
    private fun isKingMoveValid(from: Position, to: Position): Boolean {
        val rowDiff = abs(to.row - from.row)
        val colDiff = abs(to.col - from.col)
        
        // Король может ходить на одну клетку в любом направлении
        return rowDiff <= 1 && colDiff <= 1 && (rowDiff > 0 || colDiff > 0)
    }
    
    /**
     * Проверяет возможность выполнения рокировки
     * @param from начальная позиция короля
     * @param to конечная позиция короля
     * @param isBlackBottom true, если черные фигуры внизу доски
     * @return true, если рокировка допустима
     */
    protected open fun isCastlingValid(from: Position, to: Position, isBlackBottom: Boolean = true): Boolean {
        val piece = getPiece(from) ?: return false
        
        // Рокировка возможна только для короля, который еще не ходил
        if (piece.type != PieceType.KING || piece.hasMoved) return false
        
        val row = from.row
        val colDiff = to.col - from.col
        
        // Король при рокировке двигается на 2 клетки по горизонтали
        if (to.row != row || abs(colDiff) != 2) return false
        
        // Определяем тип рокировки (короткая или длинная)
        val isKingSide = colDiff > 0
        val rookCol = if (isKingSide) 7 else 0
        val rook = getPiece(Position(row, rookCol))
        
        // Проверяем наличие ладьи, которая еще не ходила
        if (rook?.type != PieceType.ROOK || rook.hasMoved) return false
        
        // Проверяем, что между королем и ладьей нет фигур
        val direction = if (isKingSide) 1 else -1
        var col = from.col + direction
        
        while (col != rookCol) {
            if (getPiece(Position(row, col)) != null) return false
            col += direction
        }
        
        // Проверяем, что король не под шахом и поля, через которые он проходит, не под ударом
        val kingColor = piece.color
        if (isKingInCheck(kingColor)) return false
        
        // Король не должен проходить через битое поле
        val intermediatePos = Position(row, from.col + direction)
        if (isSquareUnderAttack(intermediatePos, kingColor.opposite())) return false
        
        // Конечная позиция короля не должна быть под ударом
        if (isSquareUnderAttack(to, kingColor.opposite())) return false
        
        return true
    }
    
    /**
     * Проверяет, находится ли клетка под ударом фигур указанного цвета
     * @param position проверяемая позиция
     * @param attackingColor цвет атакующих фигур
     * @return true, если клетка находится под ударом
     */
    fun isSquareUnderAttack(position: Position, attackingColor: PieceColor): Boolean {
        for (row in 0..7) {
            for (col in 0..7) {
                val fromPos = Position(row, col)
                val piece = getPiece(fromPos)
                
                if (piece?.color == attackingColor) {
                    // Для пешек особая логика проверки атаки
                    if (piece.type == PieceType.PAWN) {
                        if (canPawnAttack(fromPos, position, attackingColor)) {
                            return true
                        }
                    } 
                    // Для других фигур используем стандартную проверку хода
                    else if (isValidMove(fromPos, position)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    /**
     * Проверяет, может ли пешка атаковать указанную клетку
     * @param pawnPos позиция пешки
     * @param targetPos целевая позиция
     * @param pawnColor цвет пешки
     * @return true, если пешка может атаковать клетку
     */
    private fun canPawnAttack(pawnPos: Position, targetPos: Position, pawnColor: PieceColor): Boolean {
        val rowDiff = targetPos.row - pawnPos.row
        val colDiff = abs(targetPos.col - pawnPos.col)
        
        // Пешка атакует по диагонали на одну клетку вперед
        val direction = if (pawnColor == PieceColor.WHITE) -1 else 1
        
        return colDiff == 1 && rowDiff == direction
    }
    
    /**
     * Проверяет, находится ли король указанного цвета под шахом
     * @param kingColor цвет короля
     * @return true, если король под шахом
     */
    fun isKingInCheck(kingColor: PieceColor): Boolean {
        // Найдем позицию короля
        var kingPosition: Position? = null
        
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = getPiece(pos)
                
                if (piece?.type == PieceType.KING && piece.color == kingColor) {
                    kingPosition = pos
                    break
                }
            }
            if (kingPosition != null) break
        }
        
        // Если король не найден, считаем, что шаха нет
        if (kingPosition == null) return false
        
        // Проверяем, находится ли король под ударом фигур противоположного цвета
        return isSquareUnderAttack(kingPosition, kingColor.opposite())
    }
    
    /**
     * Проверяет, находится ли король указанного цвета под шахом после выполнения заданного хода
     * @param from начальная позиция
     * @param to конечная позиция
     * @param kingColor цвет короля
     * @return true, если король будет под шахом после хода
     */
    fun isKingInCheckAfterMove(from: Position, to: Position, kingColor: PieceColor): Boolean {
        // Сохраняем текущее состояние доски
        val piece = getPiece(from)
        val targetPiece = getPiece(to)
        
        // Если ход невозможен, король не может быть под шахом после него
        if (piece == null) return false
        
        // Создаем временную копию доски для симуляции хода
        val tempBoard = this.copy()
        
        // Проверяем особые типы ходов
        
        // 1. Взятие на проходе
        val wasEnPassant = piece.type == PieceType.PAWN && 
                           from.col != to.col && 
                           targetPiece == null && 
                           enPassantTarget == to
        
        if (wasEnPassant) {
            val capturedPawnPos = Position(from.row, to.col)
            tempBoard.setPiece(capturedPawnPos, null)
        }
        
        // 2. Рокировка
        val isCastling = piece.type == PieceType.KING && 
                         abs(from.col - to.col) == 2
        
        if (isCastling) {
            // Переместить ладью при рокировке
            val rookCol = if (to.col > from.col) 7 else 0
            val rookPos = Position(from.row, rookCol)
            val rook = tempBoard.getPiece(rookPos)
            
            if (rook != null) {
                val newRookCol = if (to.col > from.col) to.col - 1 else to.col + 1
                tempBoard.setPiece(rookPos, null)
                tempBoard.setPiece(Position(from.row, newRookCol), rook)
            }
        }
        
        // Стандартное перемещение фигуры
        tempBoard.setPiece(from, null)
        tempBoard.setPiece(to, piece)
        
        // Проверяем, под шахом ли король после хода
        return tempBoard.isKingInCheck(kingColor)
    }
    
    /**
     * Расставляет фигуры в начальную позицию
     */
    open fun resetBoard() {
        // Очищаем доску
        clearBoard()
        
        // Расставляем белые фигуры
        setPiece(Position(7, 0), Rook(PieceColor.WHITE))
        setPiece(Position(7, 1), Knight(PieceColor.WHITE))
        setPiece(Position(7, 2), Bishop(PieceColor.WHITE))
        setPiece(Position(7, 3), Queen(PieceColor.WHITE))
        setPiece(Position(7, 4), King(PieceColor.WHITE))
        setPiece(Position(7, 5), Bishop(PieceColor.WHITE))
        setPiece(Position(7, 6), Knight(PieceColor.WHITE))
        setPiece(Position(7, 7), Rook(PieceColor.WHITE))
        
        for (col in 0..7) {
            setPiece(Position(6, col), Pawn(PieceColor.WHITE))
        }
        
        // Расставляем черные фигуры
        setPiece(Position(0, 0), Rook(PieceColor.BLACK))
        setPiece(Position(0, 1), Knight(PieceColor.BLACK))
        setPiece(Position(0, 2), Bishop(PieceColor.BLACK))
        setPiece(Position(0, 3), Queen(PieceColor.BLACK))
        setPiece(Position(0, 4), King(PieceColor.BLACK))
        setPiece(Position(0, 5), Bishop(PieceColor.BLACK))
        setPiece(Position(0, 6), Knight(PieceColor.BLACK))
        setPiece(Position(0, 7), Rook(PieceColor.BLACK))
        
        for (col in 0..7) {
            setPiece(Position(1, col), Pawn(PieceColor.BLACK))
        }
        
        // Сбрасываем флаг hasMoved для всех фигур
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(Position(row, col))
                if (piece != null) {
                    piece.hasMoved = false
                }
            }
        }
    }
    
    /**
     * Создает копию текущей доски
     * @return новый экземпляр ChessBoard с той же позицией
     */
    fun copy(): ChessBoard {
        val newBoard = ChessBoard()
        
        // Копируем все фигуры
        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)
                val piece = getPiece(position)
                if (piece != null) {
                    // Создаем копию фигуры
                    val newPiece = when (piece.type) {
                        PieceType.PAWN -> Pawn(piece.color)
                        PieceType.KNIGHT -> Knight(piece.color)
                        PieceType.BISHOP -> Bishop(piece.color)
                        PieceType.ROOK -> Rook(piece.color)
                        PieceType.QUEEN -> Queen(piece.color)
                        PieceType.KING -> King(piece.color)
                    }
                    
                    // Копируем состояние фигуры
                    newPiece.hasMoved = piece.hasMoved
                    
                    // Устанавливаем фигуру на новую доску
                    newBoard.setPiece(position, newPiece)
                }
            }
        }
        
        // Копируем информацию о взятии на проходе
        newBoard.enPassantTarget = this.enPassantTarget
        newBoard.lastPawnDoubleMove = this.lastPawnDoubleMove
        
        return newBoard
    }
    
    /**
     * Рассчитывает все возможные легальные ходы для фигуры на указанной позиции
     * @param position позиция фигуры
     * @return набор позиций, куда фигура может сходить
     */
    fun calculateLegalMoves(position: Position): Set<Position> {
        val piece = getPiece(position) ?: return emptySet()
        val legalMoves = mutableSetOf<Position>()
        
        // Перебираем все клетки на доске
        for (row in 0..7) {
            for (col in 0..7) {
                val targetPos = Position(row, col)
                
                // Пропускаем текущую позицию
                if (targetPos == position) continue
                
                // Проверяем обычный ход
                var isValidMove = false
                
                // Базовая проверка хода по правилам
                if (isValidMove(position, targetPos)) {
                    isValidMove = true
                }
                
                // Проверка рокировки для короля
                if (piece.type == PieceType.KING && !piece.hasMoved) {
                    if (canCastle(position, targetPos, piece.color)) {
                        isValidMove = true
                    }
                }
                
                // Проверка взятия на проходе для пешки
                if (piece.type == PieceType.PAWN && 
                    position.col != targetPos.col && 
                    getPiece(targetPos) == null &&
                    targetPos == enPassantTarget) {
                    isValidMove = true
                }
                
                if (isValidMove) {
                    // Проверяем, не оставит ли этот ход собственного короля под шахом
                    if (!isKingInCheckAfterMove(position, targetPos, piece.color)) {
                        legalMoves.add(targetPos)
                    }
                }
            }
        }
        
        return legalMoves
    }
    
    /**
     * Проверяет возможность рокировки
     */
    private fun canCastle(kingPos: Position, targetPos: Position, kingColor: PieceColor): Boolean {
        // Король уже ходил - рокировка невозможна
        if (getPiece(kingPos)?.hasMoved == true) return false
        
        // Король должен двигаться на 2 клетки по горизонтали
        if (kingPos.row != targetPos.row || abs(kingPos.col - targetPos.col) != 2) return false
        
        // Определяем тип рокировки (короткая или длинная)
        val isKingSide = targetPos.col > kingPos.col
        val rookCol = if (isKingSide) 7 else 0
        val rookPos = Position(kingPos.row, rookCol)
        
        // Проверяем наличие ладьи и что она не ходила
        val rook = getPiece(rookPos)
        if (rook?.type != PieceType.ROOK || rook.color != kingColor || rook.hasMoved) return false
        
        // Проверяем, что между королем и ладьей нет фигур
        val step = if (isKingSide) 1 else -1
        var col = kingPos.col + step
        
        while (col != rookCol) {
            if (getPiece(Position(kingPos.row, col)) != null) return false
            col += step
        }
        
        // Проверяем, что король не под шахом и не пройдет через битое поле
        if (isKingInCheck(kingColor)) return false
        
        // Проверяем, что промежуточная клетка не под ударом
        val midPos = Position(kingPos.row, kingPos.col + step)
        if (isSquareUnderAttack(midPos, kingColor.opposite())) return false
        
        // Проверяем, что конечная позиция не под ударом
        if (isSquareUnderAttack(targetPos, kingColor.opposite())) return false
        
        return true
    }
    
    /**
     * Проверяет, есть ли у игрока указанного цвета хотя бы один легальный ход
     * @param color цвет игрока
     * @return true, если есть хотя бы один легальный ход
     */
    fun hasLegalMoves(color: PieceColor): Boolean {
        // Оптимизированная версия - сначала проверяем короля
        // Находим позицию короля
        var kingPosition: Position? = null
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = getPiece(pos)
                if (piece?.type == PieceType.KING && piece.color == color) {
                    kingPosition = pos
                    break
                }
            }
            if (kingPosition != null) break
        }
        
        // Проверяем, может ли король сделать ход
        if (kingPosition != null && calculateLegalMoves(kingPosition).isNotEmpty()) {
            return true
        }
        
        // Проверяем все остальные фигуры
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = getPiece(pos)
                
                if (piece?.color == color && piece.type != PieceType.KING) {
                    // Если у фигуры есть хотя бы один легальный ход, возвращаем true
                    if (calculateLegalMoves(pos).isNotEmpty()) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Проверяет, находится ли игрок указанного цвета в положении мата
     * @param color цвет игрока
     * @return true, если игрок в мате
     */
    fun isCheckmate(color: PieceColor): Boolean {
        // Мат - это шах и отсутствие легальных ходов
        return isKingInCheck(color) && !hasLegalMoves(color)
    }
    
    /**
     * Проверяет, находится ли игрок указанного цвета в положении пата
     * @param color цвет игрока
     * @return true, если игрок в пате
     */
    fun isStalemate(color: PieceColor): Boolean {
        // Пат - это отсутствие шаха и легальных ходов
        return !isKingInCheck(color) && !hasLegalMoves(color)
    }
    
    /**
     * Устанавливает позицию на доске из FEN-нотации
     * @param fen FEN-строка, описывающая позицию
     * @return true, если позиция успешно установлена
     */
    fun setPositionFromFen(fen: String): Boolean {
        try {
            // Очищаем доску
            clearBoard()
            
            // Разбиваем FEN на части
            val parts = fen.trim().split(" ")
            val position = parts[0]
            
            // Устанавливаем фигуры на доске
            var row = 0
            var col = 0
            
            for (c in position) {
                when {
                    c == '/' -> {
                        row++
                        col = 0
                    }
                    c.isDigit() -> {
                        col += c.toString().toInt()
                    }
                    else -> {
                        val piece = when (c) {
                            'P' -> Pawn(PieceColor.WHITE)
                            'N' -> Knight(PieceColor.WHITE)
                            'B' -> Bishop(PieceColor.WHITE)
                            'R' -> Rook(PieceColor.WHITE)
                            'Q' -> Queen(PieceColor.WHITE)
                            'K' -> King(PieceColor.WHITE)
                            'p' -> Pawn(PieceColor.BLACK)
                            'n' -> Knight(PieceColor.BLACK)
                            'b' -> Bishop(PieceColor.BLACK)
                            'r' -> Rook(PieceColor.BLACK)
                            'q' -> Queen(PieceColor.BLACK)
                            'k' -> King(PieceColor.BLACK)
                            else -> null
                        }
                        
                        if (piece != null) {
                            setPiece(Position(row, col), piece)
                            col++
                        }
                    }
                }
            }
            
            // Обрабатываем дополнительную информацию из FEN
            if (parts.size > 1) {
                // Чей ход
                val activeColor = parts[1]
                
                // Права на рокировку
                if (parts.size > 2) {
                    val castlingRights = parts[2]
                    
                    // Устанавливаем флаги hasMoved для королей и ладей
                    // в зависимости от прав на рокировку
                    val whiteKingPos = findKing(PieceColor.WHITE)
                    val blackKingPos = findKing(PieceColor.BLACK)
                    
                    if (whiteKingPos != null) {
                        val whiteKing = getPiece(whiteKingPos) as? King
                        whiteKing?.hasMoved = !castlingRights.contains('K') && !castlingRights.contains('Q')
                    }
                    
                    if (blackKingPos != null) {
                        val blackKing = getPiece(blackKingPos) as? King
                        blackKing?.hasMoved = !castlingRights.contains('k') && !castlingRights.contains('q')
                    }
                    
                    // Устанавливаем флаги hasMoved для ладей
                    val whiteKingsideRook = getPiece(Position(7, 7)) as? Rook
                    whiteKingsideRook?.hasMoved = !castlingRights.contains('K')
                    
                    val whiteQueensideRook = getPiece(Position(7, 0)) as? Rook
                    whiteQueensideRook?.hasMoved = !castlingRights.contains('Q')
                    
                    val blackKingsideRook = getPiece(Position(0, 7)) as? Rook
                    blackKingsideRook?.hasMoved = !castlingRights.contains('k')
                    
                    val blackQueensideRook = getPiece(Position(0, 0)) as? Rook
                    blackQueensideRook?.hasMoved = !castlingRights.contains('q')
                }
                
                // Поле для взятия на проходе
                if (parts.size > 3 && parts[3] != "-") {
                    enPassantTarget = Position.fromAlgebraicNotation(parts[3])
                }
            }
            
            return true
        } catch (e: Exception) {
            // В случае ошибки при разборе FEN
            return false
        }
    }
    
    /**
     * Находит позицию короля указанного цвета
     * @param color цвет короля
     * @return позиция короля или null, если король не найден
     */
    private fun findKing(color: PieceColor): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)
                val piece = getPiece(position)
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return position
                }
            }
        }
        return null
    }
    
    /**
     * Возвращает текущую позицию на доске в формате FEN
     * 
     * @return FEN-строка, представляющая текущую позицию
     */
    fun getFen(): String {
        val sb = StringBuilder()
        
        // Добавляем позиции фигур
        for (row in 0..7) {
            var emptyCount = 0
            
            for (col in 0..7) {
                val piece = getPiece(Position(row, col))
                
                if (piece == null) {
                    // Увеличиваем счетчик пустых клеток
                    emptyCount++
                } else {
                    // Если перед этим были пустые клетки, записываем их количество
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    
                    // Добавляем символ фигуры
                    val pieceChar = when (piece.type) {
                        PieceType.PAWN -> 'p'
                        PieceType.ROOK -> 'r'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                    }
                    
                    // Если фигура белая, то символ в верхнем регистре
                    sb.append(if (piece.color == PieceColor.WHITE) pieceChar.uppercaseChar() else pieceChar)
                }
            }
            
            // Если в конце ряда остались пустые клетки, записываем их количество
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            
            // Добавляем разделитель рядов, кроме последнего ряда
            if (row < 7) {
                sb.append('/')
            }
        }
        
        // Добавляем остальные части FEN (активный цвет, права рокировки, возможность взятия на проходе и т.д.)
        // По умолчанию - белые ходят, никаких прав рокировки, нет взятия на проходе
        sb.append(" w - - 0 1")
        
        return sb.toString()
    }
    
    /**
     * Находит первую фигуру указанного типа и цвета на доске
     * @param type тип фигуры
     * @param color цвет фигуры
     * @return позиция фигуры или null, если фигура не найдена
     */
    private fun findPiece(type: PieceType, color: PieceColor): ChessPiece? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(Position(row, col))
                if (piece != null && piece.type == type && piece.color == color) {
                    return piece
                }
            }
        }
        return null
    }
    
    /**
     * Находит позицию фигуры указанного типа, которая может пойти на указанную позицию
     * 
     * @param pieceType Тип фигуры для поиска
     * @param toPosition Позиция назначения
     * @return Позиция фигуры или null, если такой фигуры нет
     */
    fun findPiecePositionForMove(pieceType: PieceType?, toPosition: Position?): Position? {
        if (pieceType == null || toPosition == null) return null
        
        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)
                val piece = getPiece(position)
                
                if (piece != null && piece.type == pieceType) {
                    // Проверяем, может ли эта фигура сделать ход на указанную позицию
                    val possibleMoves = getPossibleMoves(position)
                    if (possibleMoves.contains(toPosition)) {
                        return position
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Возвращает множество возможных ходов для фигуры на указанной позиции
     * 
     * @param position Позиция фигуры
     * @return Множество возможных позиций для хода
     */
    fun getPossibleMoves(position: Position): Set<Position> {
        val piece = getPiece(position) ?: return emptySet()
        val moves = mutableSetOf<Position>()
        
        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(position, piece.color, moves)
            PieceType.KNIGHT -> getKnightMoves(position, piece.color, moves)
            PieceType.BISHOP -> getBishopMoves(position, piece.color, moves)
            PieceType.ROOK -> getRookMoves(position, piece.color, moves)
            PieceType.QUEEN -> getQueenMoves(position, piece.color, moves)
            PieceType.KING -> getKingMoves(position, piece.color, moves)
        }
        
        return moves
    }
    
    /**
     * Добавляет возможные ходы пешки в список
     */
    private fun getPawnMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        val direction = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1
        
        // Ход на одну клетку вперед
        val oneStep = Position(position.row + direction, position.col)
        if (oneStep.isValid() && getPiece(oneStep) == null) {
            moves.add(oneStep)
            
            // Ход на две клетки вперед с начальной позиции
            if (position.row == startRow) {
                val twoStep = Position(position.row + 2 * direction, position.col)
                if (twoStep.isValid() && getPiece(twoStep) == null) {
                    moves.add(twoStep)
                }
            }
        }
        
        // Взятие по диагонали
        val captureLeft = Position(position.row + direction, position.col - 1)
        if (captureLeft.isValid()) {
            val pieceLeft = getPiece(captureLeft)
            if (pieceLeft != null && pieceLeft.color != color) {
                moves.add(captureLeft)
            }
        }
        
        val captureRight = Position(position.row + direction, position.col + 1)
        if (captureRight.isValid()) {
            val pieceRight = getPiece(captureRight)
            if (pieceRight != null && pieceRight.color != color) {
                moves.add(captureRight)
            }
        }
        
        // Взятие на проходе
        if (enPassantTarget != null) {
            if ((position.row == enPassantTarget!!.row) && 
                (Math.abs(position.col - enPassantTarget!!.col) == 1)) {
                moves.add(Position(position.row + direction, enPassantTarget!!.col))
            }
        }
    }
    
    /**
     * Добавляет возможные ходы коня в список
     */
    private fun getKnightMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        val knightOffsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for (offset in knightOffsets) {
            val newRow = position.row + offset.first
            val newCol = position.col + offset.second
            val newPosition = Position(newRow, newCol)
            
            if (newPosition.isValid()) {
                val piece = getPiece(newPosition)
                if (piece == null || piece.color != color) {
                    moves.add(newPosition)
                }
            }
        }
    }
    
    /**
     * Добавляет возможные ходы слона в список
     */
    private fun getBishopMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        val directions = listOf(
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        )
        
        for (direction in directions) {
            var newRow = position.row + direction.first
            var newCol = position.col + direction.second
            
            while (true) {
                val newPosition = Position(newRow, newCol)
                if (!newPosition.isValid()) break
                
                val piece = getPiece(newPosition)
                if (piece == null) {
                    moves.add(newPosition)
                } else {
                    if (piece.color != color) {
                        moves.add(newPosition)
                    }
                    break
                }
                
                newRow += direction.first
                newCol += direction.second
            }
        }
    }
    
    /**
     * Добавляет возможные ходы ладьи в список
     */
    private fun getRookMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        val directions = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )
        
        for (direction in directions) {
            var newRow = position.row + direction.first
            var newCol = position.col + direction.second
            
            while (true) {
                val newPosition = Position(newRow, newCol)
                if (!newPosition.isValid()) break
                
                val piece = getPiece(newPosition)
                if (piece == null) {
                    moves.add(newPosition)
                } else {
                    if (piece.color != color) {
                        moves.add(newPosition)
                    }
                    break
                }
                
                newRow += direction.first
                newCol += direction.second
            }
        }
    }
    
    /**
     * Добавляет возможные ходы ферзя в список
     */
    private fun getQueenMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        // Ферзь = ладья + слон
        getRookMoves(position, color, moves)
        getBishopMoves(position, color, moves)
    }
    
    /**
     * Добавляет возможные ходы короля в список
     */
    private fun getKingMoves(position: Position, color: PieceColor, moves: MutableSet<Position>) {
        val directions = listOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1), Pair(0, 1),
            Pair(1, -1), Pair(1, 0), Pair(1, 1)
        )
        
        for (direction in directions) {
            val newRow = position.row + direction.first
            val newCol = position.col + direction.second
            val newPosition = Position(newRow, newCol)
            
            if (newPosition.isValid()) {
                val piece = getPiece(newPosition)
                if (piece == null || piece.color != color) {
                    moves.add(newPosition)
                }
            }
        }
        
        // TODO: добавить рокировку
    }
    
    /**
     * Получает текущее состояние доски в виде карты позиций и фигур
     * @return карта, где ключ - позиция, значение - фигура
     */
    fun getBoardState(): Map<Position, ChessPiece> {
        val state = mutableMapOf<Position, ChessPiece>()
        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)
                val piece = getPiece(position)
                if (piece != null) {
                    state[position] = piece
                }
            }
        }
        return state
    }
}

/**
 * Доска с ориентацией черных фигур внизу
 */
class BlackChangeBoard : ChessBoard() {
    /**
     * Переопределяем расстановку фигур для ориентации с черными внизу
     */
    override fun resetBoard() {
        clearBoard()
        
        // Расставляем черные фигуры внизу
        setPiece(Position(7, 0), Rook(PieceColor.BLACK))
        setPiece(Position(7, 1), Knight(PieceColor.BLACK))
        setPiece(Position(7, 2), Bishop(PieceColor.BLACK))
        setPiece(Position(7, 3), Queen(PieceColor.BLACK))
        setPiece(Position(7, 4), King(PieceColor.BLACK))
        setPiece(Position(7, 5), Bishop(PieceColor.BLACK))
        setPiece(Position(7, 6), Knight(PieceColor.BLACK))
        setPiece(Position(7, 7), Rook(PieceColor.BLACK))
        
        for (col in 0..7) {
            setPiece(Position(6, col), Pawn(PieceColor.BLACK))
        }
        
        // Расставляем белые фигуры вверху
        setPiece(Position(0, 0), Rook(PieceColor.WHITE))
        setPiece(Position(0, 1), Knight(PieceColor.WHITE))
        setPiece(Position(0, 2), Bishop(PieceColor.WHITE))
        setPiece(Position(0, 3), Queen(PieceColor.WHITE))
        setPiece(Position(0, 4), King(PieceColor.WHITE))
        setPiece(Position(0, 5), Bishop(PieceColor.WHITE))
        setPiece(Position(0, 6), Knight(PieceColor.WHITE))
        setPiece(Position(0, 7), Rook(PieceColor.WHITE))
        
        for (col in 0..7) {
            setPiece(Position(1, col), Pawn(PieceColor.WHITE))
        }
        
        // Сбрасываем флаг hasMoved для всех фигур
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(Position(row, col))
                if (piece != null) {
                    piece.hasMoved = false
                }
            }
        }
    }
    
    /**
     * Создает копию доски с сохранением типа
     */
    fun createCopy(): BlackChangeBoard {
        val newBoard = BlackChangeBoard()
        
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = getPiece(pos)
                
                if (piece != null) {
                    when (piece.type) {
                        PieceType.PAWN -> newBoard.setPiece(pos, Pawn(piece.color))
                        PieceType.ROOK -> newBoard.setPiece(pos, Rook(piece.color))
                        PieceType.KNIGHT -> newBoard.setPiece(pos, Knight(piece.color))
                        PieceType.BISHOP -> newBoard.setPiece(pos, Bishop(piece.color))
                        PieceType.QUEEN -> newBoard.setPiece(pos, Queen(piece.color))
                        PieceType.KING -> newBoard.setPiece(pos, King(piece.color))
                    }
                    newBoard.getPiece(pos)?.hasMoved = piece.hasMoved
                }
            }
        }
        
        // Копируем состояние для взятия на проходе
        newBoard.enPassantTarget = this.enPassantTarget
        newBoard.lastPawnDoubleMove = this.lastPawnDoubleMove
        
        return newBoard
    }
}

/**
 * Доска с ориентацией белых фигур внизу
 */
class WhiteChangeBoard : ChessBoard() {
    /**
     * Переопределяем расстановку фигур для ориентации с белыми внизу
     */
    override fun resetBoard() {
        clearBoard()
        
        // Расставляем белые фигуры внизу
        setPiece(Position(7, 0), Rook(PieceColor.WHITE))
        setPiece(Position(7, 1), Knight(PieceColor.WHITE))
        setPiece(Position(7, 2), Bishop(PieceColor.WHITE))
        setPiece(Position(7, 3), Queen(PieceColor.WHITE))
        setPiece(Position(7, 4), King(PieceColor.WHITE))
        setPiece(Position(7, 5), Bishop(PieceColor.WHITE))
        setPiece(Position(7, 6), Knight(PieceColor.WHITE))
        setPiece(Position(7, 7), Rook(PieceColor.WHITE))
        
        for (col in 0..7) {
            setPiece(Position(6, col), Pawn(PieceColor.WHITE))
        }
        
        // Расставляем черные фигуры вверху
        setPiece(Position(0, 0), Rook(PieceColor.BLACK))
        setPiece(Position(0, 1), Knight(PieceColor.BLACK))
        setPiece(Position(0, 2), Bishop(PieceColor.BLACK))
        setPiece(Position(0, 3), Queen(PieceColor.BLACK))
        setPiece(Position(0, 4), King(PieceColor.BLACK))
        setPiece(Position(0, 5), Bishop(PieceColor.BLACK))
        setPiece(Position(0, 6), Knight(PieceColor.BLACK))
        setPiece(Position(0, 7), Rook(PieceColor.BLACK))
        
        for (col in 0..7) {
            setPiece(Position(1, col), Pawn(PieceColor.BLACK))
        }
        
        // Сбрасываем флаг hasMoved для всех фигур
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = getPiece(Position(row, col))
                if (piece != null) {
                    piece.hasMoved = false
                }
            }
        }
    }
    
    /**
     * Создает копию доски с сохранением типа
     */
    fun createCopy(): WhiteChangeBoard {
        val newBoard = WhiteChangeBoard()
        
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = getPiece(pos)
                
                if (piece != null) {
                    when (piece.type) {
                        PieceType.PAWN -> newBoard.setPiece(pos, Pawn(piece.color))
                        PieceType.ROOK -> newBoard.setPiece(pos, Rook(piece.color))
                        PieceType.KNIGHT -> newBoard.setPiece(pos, Knight(piece.color))
                        PieceType.BISHOP -> newBoard.setPiece(pos, Bishop(piece.color))
                        PieceType.QUEEN -> newBoard.setPiece(pos, Queen(piece.color))
                        PieceType.KING -> newBoard.setPiece(pos, King(piece.color))
                    }
                    newBoard.getPiece(pos)?.hasMoved = piece.hasMoved
                }
            }
        }
        
        // Копируем состояние для взятия на проходе
        newBoard.enPassantTarget = this.enPassantTarget
        newBoard.lastPawnDoubleMove = this.lastPawnDoubleMove
        
        return newBoard
    }
} 