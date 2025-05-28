package com.chunosov.chessbgpu.engine

import android.util.Log
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.chunosov.chessbgpu.model.ChessBoard
import com.chunosov.chessbgpu.model.Position
import com.chunosov.chessbgpu.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Адаптер для работы с библиотекой bhlangonijr/chesslib.
 * Предоставляет API для взаимодействия с шахматной библиотекой.
 */
class ChessLibAdapter {
    
    companion object {
        private const val TAG = "ChessLibAdapter"
        
        // Singleton для обеспечения единственного экземпляра адаптера
        @Volatile
        private var instance: ChessLibAdapter? = null
        
        fun getInstance(): ChessLibAdapter {
            return instance ?: synchronized(this) {
                instance ?: ChessLibAdapter().also { instance = it }
            }
        }
    }
    
    // Основная доска для работы с шахматной позицией
    private val board = Board()
    
    private var isInitialized = false
    
    /**
     * Инициализирует шахматную доску в начальную позицию.
     */
    fun initialize(): Boolean {
        try {
            // Стандартная начальная позиция в шахматах
            val initialPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            board.loadFromFen(initialPosition)
            isInitialized = true
            Log.d(TAG, "Доска инициализирована в начальную позицию")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации доски", e)
            return false
        }
    }
    
    /**
     * Загружает позицию из FEN нотации.
     * 
     * @param fen FEN строка с описанием позиции
     * @return true, если позиция успешно загружена
     */
    fun loadPosition(fen: String): Boolean {
        return try {
            board.loadFromFen(fen)
            Log.d(TAG, "Загружена позиция: $fen")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке позиции из FEN: $fen", e)
            false
        }
    }
    
    /**
     * Выполняет ход на доске.
     * 
     * @param moveString Ход в алгебраической нотации (например, "e2e4")
     * @return true, если ход выполнен успешно
     */
    fun makeMove(moveString: String): Boolean {
        return try {
            // Парсинг строки хода в объект Move
            val from = Square.valueOf(moveString.substring(0, 2).uppercase())
            val to = Square.valueOf(moveString.substring(2, 4).uppercase())
            
            val move = Move(from, to)
            
            // Проверка на валидность хода
            if (!board.legalMoves().contains(move)) {
                Log.e(TAG, "Недопустимый ход: $moveString")
                return false
            }
            
            // Выполнение хода
            board.doMove(move)
            Log.d(TAG, "Выполнен ход: $moveString, новая позиция: ${board.fen}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении хода: $moveString", e)
            false
        }
    }
    
    /**
     * Выполняет ход на доске с возможным превращением пешки.
     * 
     * @param moveString Ход в алгебраической нотации с указанием фигуры для превращения (например, "e7e8q")
     * @return true, если ход выполнен успешно
     */
    fun makeMoveWithPromotion(moveString: String): Boolean {
        return try {
            if (moveString.length < 5) {
                return makeMove(moveString)
            }
            
            val from = Square.valueOf(moveString.substring(0, 2).uppercase())
            val to = Square.valueOf(moveString.substring(2, 4).uppercase())
            
            // Определяем фигуру для превращения
            val promotionChar = moveString[4].lowercase()
            val promotionPiece = when (promotionChar) {
                "q" -> if (board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                "r" -> if (board.sideToMove == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                "b" -> if (board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP 
                "n" -> if (board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> null
            }
            
            val move = if (promotionPiece != null) Move(from, to, promotionPiece) else Move(from, to)
            
            // Проверка на валидность хода
            if (!board.legalMoves().contains(move)) {
                Log.e(TAG, "Недопустимый ход: $moveString")
                return false
            }
            
            // Выполнение хода
            board.doMove(move)
            Log.d(TAG, "Выполнен ход с превращением: $moveString, новая позиция: ${board.fen}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении хода с превращением: $moveString", e)
            false
        }
    }
    
    /**
     * Отменяет последний сделанный ход.
     * 
     * @return true, если отмена хода выполнена успешно
     */
    fun undoMove(): Boolean {
        return try {
            val move = board.undoMove()
            Log.d(TAG, "Отменен ход: $move, новая позиция: ${board.fen}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отмене хода", e)
            false
        }
    }
    
    /**
     * Получает текущую позицию доски в FEN нотации.
     * 
     * @return FEN строка текущей позиции
     */
    fun getFen(): String {
        return board.fen
    }
    
    /**
     * Проверяет, находится ли король под шахом.
     * 
     * @return true, если король текущей стороны под шахом
     */
    fun isKingAttacked(): Boolean {
        return board.isKingAttacked
    }
    
    /**
     * Проверяет, находится ли сторона в положении мат.
     * 
     * @return true, если текущей стороне поставлен мат
     */
    fun isMated(): Boolean {
        return board.isMated
    }
    
    /**
     * Проверяет, находится ли игра в ситуации пат.
     * 
     * @return true, если ситуация пат
     */
    fun isStaleMate(): Boolean {
        return board.isStaleMate
    }
    
    /**
     * Проверяет, объявлена ли ничья из-за правил игры.
     * 
     * @return true, если ничья
     */
    fun isDraw(): Boolean {
        return board.isDraw
    }
    
    /**
     * Проверяет, достаточно ли материала для победы.
     * 
     * @return true, если недостаточно материала для победы
     */
    fun isInsufficientMaterial(): Boolean {
        return board.isInsufficientMaterial
    }
    
    /**
     * Получает сторону, которая должна ходить.
     * 
     * @return "WHITE" или "BLACK"
     */
    fun getSideToMove(): String {
        return board.sideToMove.toString()
    }
    
    /**
     * Получает список всех возможных ходов в текущей позиции.
     * 
     * @return Список ходов в алгебраической нотации
     */
    fun getLegalMoves(): List<String> {
        return board.legalMoves().map { it.toString() }
    }
    
    /**
     * Проверяет, является ли ход допустимым в текущей позиции.
     * 
     * @param moveString Ход в алгебраической нотации
     * @return true, если ход допустим
     */
    fun isValidMove(moveString: String): Boolean {
        return try {
            val from = Square.valueOf(moveString.substring(0, 2).uppercase())
            val to = Square.valueOf(moveString.substring(2, 4).uppercase())
            
            val move = Move(from, to)
            board.legalMoves().contains(move)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке хода: $moveString", e)
            false
        }
    }
    
    /**
     * Проверяет, является ли ход допустимым в указанной позиции.
     * 
     * @param fen Позиция в формате FEN
     * @param moveString Ход в алгебраической нотации
     * @return true, если ход допустим
     */
    fun isValidMove(fen: String, moveString: String): Boolean {
        return try {
            val tempBoard = Board()
            tempBoard.loadFromFen(fen)
            
            val from = Square.valueOf(moveString.substring(0, 2).uppercase())
            val to = Square.valueOf(moveString.substring(2, 4).uppercase())
            
            val move = Move(from, to)
            tempBoard.legalMoves().contains(move)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке хода: $moveString в позиции $fen", e)
            false
        }
    }
    
    /**
     * Находит лучший ход в текущей позиции.
     * Примечание: в этой реализации возвращает первый допустимый ход, 
     * так как библиотека chesslib не имеет встроенного движка для оценки позиции.
     * 
     * @param fen FEN строка с позицией
     * @param timeMs Время на анализ (не используется)
     * @return Лучший ход или null, если ходов нет
     */
    fun findBestMove(fen: String, timeMs: Int = 1000): String? {
        try {
            val tempBoard = Board()
            tempBoard.loadFromFen(fen)
            
            val legalMoves = tempBoard.legalMoves()
            if (legalMoves.isEmpty()) {
                return null
            }
            
            // Просто возвращаем первый доступный ход, так как нет оценочной функции
            return legalMoves.first().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске лучшего хода: $fen", e)
            return null
        }
    }
    
    /**
     * Оценивает текущую позицию.
     * Примечание: в этой реализации возвращает 0.0, так как библиотека chesslib
     * не имеет встроенного движка для оценки позиции.
     * 
     * @param fen FEN строка с позицией
     * @param timeMs Время на анализ (не используется)
     * @return Оценка позиции (0.0 для нейтральной позиции)
     */
    fun evaluatePosition(fen: String, timeMs: Int = 1000): Double {
        // В данной реализации просто возвращаем 0.0, так как нет оценочной функции
        return 0.0
    }
    
    /**
     * Проверяет, находится ли король под шахом в указанной позиции.
     * 
     * @param fen FEN строка с позицией
     * @return true, если король под шахом
     */
    fun isKingInCheck(fen: String): Boolean {
        try {
            val tempBoard = Board()
            tempBoard.loadFromFen(fen)
            return tempBoard.isKingAttacked
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке шаха: $fen", e)
            return false
        }
    }
    
    /**
     * Проверяет, находится ли сторона в положении мат в указанной позиции.
     * 
     * @param fen FEN строка с позицией
     * @return true, если мат
     */
    fun isMate(fen: String): Boolean {
        try {
            val tempBoard = Board()
            tempBoard.loadFromFen(fen)
            return tempBoard.isMated
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке мата: $fen", e)
            return false
        }
    }
    
    /**
     * Проверяет корректность FEN нотации.
     * 
     * @param fen FEN строка для проверки
     * @return true, если FEN корректен
     */
    fun isValidFen(fen: String): Boolean {
        return try {
            val tempBoard = Board()
            tempBoard.loadFromFen(fen)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Некорректный FEN: $fen", e)
            false
        }
    }
    
    /**
     * Очищает ресурсы.
     */
    fun cleanup() {
        // В данной реализации нет ресурсов для очистки
        Log.d(TAG, "Ресурсы очищены")
    }
    
    /**
     * Получает подсказку для хода
     * 
     * @param fen Текущая позиция в FEN нотации
     * @param expectedMove Ожидаемый ход в алгебраической нотации
     * @return Подсказка в формате координат (например, "e2e4") или null, если подсказку получить не удалось
     */
    fun getHint(fen: String, expectedMove: String): String? {
        if (!isInitialized) {
            Log.e(TAG, "ChessLibAdapter не инициализирован")
            return null
        }

        try {
            // Загружаем позицию
            board.loadFromFen(fen)
            
            // Очищаем ход от специальных символов
            val cleanMove = expectedMove.replace("+", "").replace("#", "").replace("x", "").trim()
            
            // Для ходов в формате e2e4
            if (cleanMove.length == 4) {
                // Проверяем, что это допустимый ход
                val from = Square.valueOf(cleanMove.substring(0, 2).uppercase())
                val to = Square.valueOf(cleanMove.substring(2, 4).uppercase())
                val move = Move(from, to)
                
                if (board.legalMoves().contains(move)) {
                    return cleanMove
                }
            }
            
            // Для ходов в формате Qf7 или Qxf7
            if (cleanMove.length >= 2) {
                val destination = cleanMove.takeLast(2)
                val piece = cleanMove.firstOrNull()
                
                if (piece != null) {
                    // Ищем фигуру на доске, которая может сделать этот ход
                    val legalMoves = board.legalMoves()
                    for (move in legalMoves) {
                        val pieceAtFrom = board.getPiece(move.from)
                        if (pieceAtFrom != null && pieceAtFrom.toString().first() == piece && 
                            move.to.toString().lowercase() == destination) {
                            return move.toString()
                        }
                    }
                }
            }
            
            // Если не удалось найти ход, возвращаем null
            Log.e(TAG, "Не удалось найти допустимый ход для подсказки: $expectedMove")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении подсказки", e)
            return null
        }
    }
} 