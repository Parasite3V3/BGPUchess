package com.chunosov.chessbgpu.engine

import android.content.Context
import android.util.Log
import com.chunosov.chessbgpu.model.ChessPuzzle
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Класс для валидации шахматных задач и их решений с использованием библиотеки chesslib.
 */
class PuzzleValidator(private val context: Context) {
    
    companion object {
        private const val TAG = "PuzzleValidator"
        
        // Singleton
        private var instance: PuzzleValidator? = null
        
        fun getInstance(context: Context): PuzzleValidator {
            if (instance == null) {
                instance = PuzzleValidator(context.applicationContext)
            }
            return instance!!
        }
    }
    
    /**
     * Проверяет корректность шахматной задачи
     * 
     * @param puzzle Задача для проверки
     * @return Результат валидации
     */
    suspend fun validatePuzzle(puzzle: ChessPuzzle): ValidationResult {
        Log.d(TAG, "Начинаем валидацию задачи: ${puzzle.id}")
        
        // Проверяем корректность начальной позиции
        Log.d(TAG, "Проверяем начальную позицию: ${puzzle.initialFen}")
        if (!isValidPosition(puzzle.initialFen)) {
            Log.e(TAG, "Некорректная начальная позиция: ${puzzle.initialFen}")
            return ValidationResult(
                isValid = false,
                errorMessage = "Некорректная начальная позиция"
            )
        }
        
        // Создаем доску и загружаем начальную позицию
        val board = Board()
        try {
            board.loadFromFen(puzzle.initialFen)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить позицию: ${puzzle.initialFen}", e)
            return ValidationResult(
                isValid = false,
                errorMessage = "Не удалось загрузить начальную позицию"
            )
        }
        
        var moveIndex = 0
        
        // Проверяем каждый ход в решении
        Log.d(TAG, "Начинаем проверку ходов. Всего ходов: ${puzzle.solutionMoves.size}")
        for (move in puzzle.solutionMoves) {
            // Пропускаем аннотации к ходам
            val cleanMove = move.replace("+", "").replace("#", "")
            
            Log.d(TAG, "Проверяем ход #${moveIndex + 1}: $move (очищенный: $cleanMove) для позиции ${board.fen}")
            
            // Проверяем, является ли ход допустимым
            val isValid = isValidMove(board, cleanMove)
            if (!isValid) {
                Log.e(TAG, "Недопустимый ход: $move (шаг $moveIndex)")
                return ValidationResult(
                    isValid = false,
                    errorMessage = "Недопустимый ход: $move (шаг $moveIndex)"
                )
            }
            
            // Делаем ход
            try {
                // Преобразуем алгебраическую нотацию в формат UCI
                val from = Square.valueOf(cleanMove.substring(0, 2).uppercase())
                val to = Square.valueOf(cleanMove.substring(2, 4).uppercase())
                
                // Создаем объект хода
                val moveObj = Move(from, to)
                
                // Выполняем ход
                board.doMove(moveObj)
                Log.d(TAG, "Позиция после хода $move: ${board.fen}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при выполнении хода: $move (шаг $moveIndex)", e)
                return ValidationResult(
                    isValid = false, 
                    errorMessage = "Ошибка при выполнении хода: $move (шаг $moveIndex)"
                )
            }
            
            moveIndex++
        }
        
        // Проверяем, завершается ли решение матом, если задача - мат в N ходов
        if (puzzle.puzzleType.startsWith("Мат в")) {
            Log.d(TAG, "Проверяем, завершается ли задача матом. Тип задачи: ${puzzle.puzzleType}")
            val isMate = board.isMated
            if (!isMate) {
                Log.e(TAG, "Решение не заканчивается матом для позиции: ${board.fen}")
                return ValidationResult(
                    isValid = false,
                    errorMessage = "Решение не заканчивается матом"
                )
            }
            Log.d(TAG, "Подтверждено: решение заканчивается матом")
        }
        
        Log.d(TAG, "Задача ${puzzle.id} прошла валидацию успешно")
        return ValidationResult(
            isValid = true,
            errorMessage = ""
        )
    }
    
    /**
     * Проверяет корректность FEN-нотации.
     * 
     * @param fen FEN-строка для проверки
     * @return true, если FEN корректен
     */
    private fun isValidPosition(fen: String): Boolean {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке FEN: $fen", e)
            false
        }
    }
    
    /**
     * Проверяет корректность хода в текущей позиции.
     * 
     * @param board Шахматная доска
     * @param moveStr Ход в алгебраической нотации (например, "e2e4")
     * @return true, если ход допустим
     */
    private fun isValidMove(board: Board, moveStr: String): Boolean {
        return try {
            val from = Square.valueOf(moveStr.substring(0, 2).uppercase())
            val to = Square.valueOf(moveStr.substring(2, 4).uppercase())
            
            val move = Move(from, to)
            
            // Проверяем, что ход есть в списке легальных ходов
            val legalMoves = board.legalMoves()
            legalMoves.contains(move)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке хода: $moveStr", e)
            false
        }
    }
    
    /**
     * Делает ход и возвращает новую позицию в FEN нотации.
     * 
     * @param fen Исходная позиция в FEN
     * @param moveStr Ход в алгебраической нотации
     * @return Новая позиция в FEN или null в случае ошибки
     */
    private fun makeMove(fen: String, moveStr: String): String? {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            
            val from = Square.valueOf(moveStr.substring(0, 2).uppercase())
            val to = Square.valueOf(moveStr.substring(2, 4).uppercase())
            
            val move = Move(from, to)
            
            // Проверяем, что ход легальный
            if (!board.legalMoves().contains(move)) {
                return null
            }
            
            // Делаем ход
            board.doMove(move)
            
            // Возвращаем новую позицию
            board.fen
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении хода: $moveStr", e)
            null
        }
    }
    
    /**
     * Проверяет, является ли позиция матовой.
     * 
     * @param fen FEN-строка с позицией
     * @return true, если позиция матовая
     */
    private fun isMatePosition(fen: String): Boolean {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            
            board.isMated
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке мата для позиции: $fen", e)
            false
        }
    }
    
    /**
     * Очищает ресурсы.
     */
    fun cleanup() {
        // No resources to clean up
    }
    
    /**
     * Генерирует подсказку для следующего хода в задаче.
     * 
     * @param puzzle Задача
     * @param currentMoveIndex Текущий индекс хода
     * @return Подсказка или null, если подсказка недоступна
     */
    suspend fun getHint(puzzle: ChessPuzzle, currentMoveIndex: Int): String? = withContext(Dispatchers.IO) {
        try {
            if (currentMoveIndex >= puzzle.solutionMoves.size) {
                Log.d(TAG, "Нет подсказки: текущий индекс хода ($currentMoveIndex) превышает размер решения (${puzzle.solutionMoves.size})")
                return@withContext null
            }
            
            // Получаем правильный ход из решения
            val nextMove = puzzle.solutionMoves[currentMoveIndex]
            Log.d(TAG, "Получена подсказка для хода $currentMoveIndex: $nextMove")
            
            // Очищаем ход от аннотаций (+ и #)
            val cleanMove = nextMove.replace("+", "").replace("#", "").trim()
            
            // Проверяем, что ход имеет правильный формат (например, "e2e4")
            if (cleanMove.length >= 4) {
                return@withContext cleanMove
            } else {
                Log.e(TAG, "Некорректный формат хода в подсказке: $cleanMove")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении подсказки", e)
            return@withContext null
        }
    }
    
    /**
     * Пытается исправить решение задачи, проверяя каждый ход.
     * 
     * @param puzzle Задача для исправления
     * @return Исправленное решение или null, если исправить не удалось
     */
    suspend fun fixPuzzleSolution(puzzle: ChessPuzzle): List<String>? = withContext(Dispatchers.IO) {
        try {
            // Создаем доску с начальной позицией
            val board = Board()
            try {
                board.loadFromFen(puzzle.initialFen)
            } catch (e: Exception) {
                Log.e(TAG, "Не удалось загрузить начальную позицию: ${puzzle.initialFen}", e)
                return@withContext null
            }
            
            val fixedSolution = mutableListOf<String>()
            
            // Проверяем каждый ход в решении
            for (move in puzzle.solutionMoves) {
                // Очищаем аннотации ходов
                val cleanMove = move.replace("+", "").replace("#", "")
                
                Log.d(TAG, "Проверяем ход: $move (очищенный: $cleanMove)")
                
                // Проверяем, является ли ход допустимым
                if (isValidMove(board, cleanMove)) {
                    // Добавляем в исправленное решение
                    fixedSolution.add(cleanMove)
                    
                    // Выполняем ход
                    val from = Square.valueOf(cleanMove.substring(0, 2).uppercase())
                    val to = Square.valueOf(cleanMove.substring(2, 4).uppercase())
                    val moveObj = Move(from, to)
                    board.doMove(moveObj)
                } else {
                    Log.e(TAG, "Недопустимый ход: $move")
                    
                    // Пытаемся найти альтернативный ход, если это возможно
                    val legalMoves = board.legalMoves()
                    if (legalMoves.isNotEmpty()) {
                        // Берем первый доступный ход для замены
                        val alternativeMove = legalMoves.first().toString()
                        fixedSolution.add(alternativeMove)
                        
                        // Выполняем альтернативный ход
                        board.doMove(legalMoves.first())
                        
                        Log.d(TAG, "Заменяем ход $move на $alternativeMove")
                    } else {
                        Log.e(TAG, "Нет доступных ходов для замены недопустимого хода: $move")
                    }
                }
            }
            
            return@withContext if (fixedSolution.isNotEmpty()) fixedSolution else null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при исправлении решения задачи", e)
            return@withContext null
        }
    }
}

/**
 * Класс для представления результата валидации задачи.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
) 