package com.chunosov.chessbgpu.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Класс, представляющий шахматную задачу.
 * 
 * @property id Уникальный идентификатор задачи
 * @property initialFen Начальная позиция в формате FEN
 * @property solutionMoves Последовательность правильных ходов для решения задачи
 * @property puzzleType Тип задачи (например, "мат в 1 ход", "выигрыш фигуры")
 * @property description Текстовое описание или условие задачи
 * @property currentMoveIndex Индекс текущего хода в последовательности solutionMoves
 * @property currentFenHistory История состояний доски в формате FEN
 */
data class ChessPuzzle(
    @SerializedName("id") val id: String,
    @SerializedName("initial_fen") val initialFen: String,
    @SerializedName("solution_moves") val solutionMoves: List<String>,
    @SerializedName("puzzle_type") val puzzleType: String,
    @SerializedName("description") val description: String,
    @SerializedName("current_move_index") var currentMoveIndex: Int = 0,
    @SerializedName("current_fen_history") var currentFenHistory: MutableList<String> = mutableListOf()
) {
    init {
        // Инициализируем историю FEN с начальной позицией, если она еще не инициализирована
        if (currentFenHistory.isEmpty()) {
            currentFenHistory.add(initialFen)
        }
    }
    
    /**
     * Проверяет, является ли ход пользователя правильным для текущего состояния задачи
     * 
     * @param move Ход в алгебраической нотации (например, "e4", "Nf3")
     * @return true, если ход правильный, false в противном случае
     */
    fun isCorrectMove(move: String): Boolean {
        if (currentMoveIndex >= solutionMoves.size) {
            println("DEBUG: Ошибка - все ходы уже сделаны, текущий индекс: $currentMoveIndex")
            return false // Все ходы уже были сделаны
        }
        
        // Проверяем, соответствует ли ход ожидаемому
        val expectedMove = solutionMoves[currentMoveIndex]
        val userMove = move.trim()
        
        // Сравниваем ходы, игнорируя символы +, # и т.д. в конце
        val normalizedExpectedMove = expectedMove.replace("+", "").replace("#", "").trim()
        val normalizedUserMove = userMove.replace("+", "").replace("#", "").trim()
        
        // Добавляем отладочную информацию
        println("DEBUG: Проверяем ход: '$userMove' (нормализованный: '$normalizedUserMove')")
        println("DEBUG: Ожидаемый ход: '$expectedMove' (нормализованный: '$normalizedExpectedMove')")
        println("DEBUG: Совпадают ли ходы: ${normalizedUserMove.equals(normalizedExpectedMove, ignoreCase = true)}")
        
        // Проверка на короткую запись хода
        if (!normalizedUserMove.equals(normalizedExpectedMove, ignoreCase = true)) {
            // Если ходы не совпадают, возможно, проблема в формате записи
            // Например, "Rf8" и "Rf1f8" - оба могут быть правильными записями одного хода
            // Проверяем, содержит ли ожидаемый ход тот же тип фигуры и клетку назначения
            
            // Для ходов фигур (не пешек), проверяем, совпадает ли тип фигуры и клетка назначения
            if (normalizedExpectedMove.length >= 3 && normalizedUserMove.length >= 3) {
                val expectedPieceType = normalizedExpectedMove[0]
                val userPieceType = normalizedUserMove[0]
                
                // Извлекаем клетку назначения (последние 2 символа)
                val expectedTarget = normalizedExpectedMove.takeLast(2)
                val userTarget = normalizedUserMove.takeLast(2)
                
                println("DEBUG: Проверка альтернативного формата: ${expectedPieceType == userPieceType && expectedTarget == userTarget}")
                
                if (expectedPieceType == userPieceType && expectedTarget == userTarget) {
                    return true
                }
            }
        }
        
        return normalizedUserMove.equals(normalizedExpectedMove, ignoreCase = true)
    }
    
    /**
     * Выполняет ход и обновляет состояние задачи
     * 
     * @param move Ход в алгебраической нотации
     * @param resultingFen FEN-строка, получившаяся после хода
     * @return true, если ход был успешно выполнен, false в противном случае
     */
    fun makeMove(move: String, resultingFen: String): Boolean {
        if (!isCorrectMove(move)) {
            return false
        }
        
        // Обновляем индекс текущего хода
        currentMoveIndex++
        
        // Добавляем новое состояние доски в историю
        // Эта история используется для отката к последнему правильному состоянию
        // при неправильном ходе пользователя
        currentFenHistory.add(resultingFen)
        
        return true
    }
    
    /**
     * Определяет, чей сейчас должен быть ход (пользователя или компьютера)
     * 
     * @return true, если сейчас должен ходить пользователь, false - если компьютер
     */
    fun isUserTurn(): Boolean {
        // Предполагаем, что ходы чередуются: пользователь (четные индексы) -> компьютер (нечетные индексы)
        // Первый ход (индекс 0) всегда принадлежит пользователю
        return currentMoveIndex % 2 == 0
    }
    
    /**
     * Возвращает ход, который ожидается от компьютера (следующий ход после хода пользователя)
     * Используется для задач, где ходы соперника предопределены
     * 
     * @return Ход компьютера или null, если все ходы выполнены или сейчас не ход компьютера
     */
    fun getComputerMove(): String? {
        // Проверяем, что сейчас ход компьютера и индекс не вышел за пределы массива
        if (currentMoveIndex < solutionMoves.size && !isUserTurn()) {
            return solutionMoves[currentMoveIndex]
        }
        return null
    }
    
    /**
     * Проверяет, завершена ли задача
     * 
     * @return true, если все правильные ходы были сделаны, false в противном случае
     */
    fun isPuzzleComplete(): Boolean {
        return currentMoveIndex >= solutionMoves.size
    }
    
    /**
     * Сбрасывает состояние задачи к начальному
     */
    fun reset() {
        currentMoveIndex = 0
        currentFenHistory.clear()
        currentFenHistory.add(initialFen)
    }
    
    /**
     * Отменяет последний ход (может потребоваться отменить и ход пользователя, и ответный ход компьютера)
     * 
     * @param undoComputerMoveAlso true, если нужно отменить и ход компьютера тоже
     * @return FEN-строка, представляющая состояние доски после отмены хода
     */
    fun undoMove(undoComputerMoveAlso: Boolean = true): String {
        if (currentMoveIndex > 0) {
            if (undoComputerMoveAlso && currentMoveIndex % 2 == 0 && currentMoveIndex > 1) {
                // Если сейчас ход пользователя, и перед ним был ход компьютера,
                // отменяем оба хода (уменьшаем currentMoveIndex на 2)
                currentMoveIndex -= 2
                if (currentFenHistory.size > 2) {
                    currentFenHistory.removeAt(currentFenHistory.size - 1)
                    currentFenHistory.removeAt(currentFenHistory.size - 1)
                }
            } else {
                // Отменяем только один ход (уменьшаем currentMoveIndex на 1)
                currentMoveIndex--
                if (currentFenHistory.size > 1) {
                    currentFenHistory.removeAt(currentFenHistory.size - 1)
                }
            }
        }
        
        // Возвращаем последний доступный FEN из истории
        // Это то состояние, к которому доска должна вернуться
        return currentFenHistory.last()
    }
    
    /**
     * Создает JSON-представление этого объекта
     * 
     * @return JSON-строка
     */
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    /**
     * Проверяет, является ли FEN корректным
     * 
     * @param fen FEN-строка для проверки
     * @return true, если FEN корректный
     */
    fun isFenValid(fen: String): Boolean {
        // Простая проверка структуры FEN
        val parts = fen.split(" ")
        if (parts.size < 4) return false
        
        // Проверка расстановки фигур
        val rows = parts[0].split("/")
        if (rows.size != 8) return false
        
        for (row in rows) {
            var sum = 0
            for (c in row) {
                if (c.isDigit()) {
                    sum += c.toString().toInt()
                } else if ("KQRBNPkqrbnp".contains(c)) {
                    sum += 1
                } else {
                    return false // Недопустимый символ
                }
            }
            if (sum != 8) return false // Неверное количество клеток в ряду
        }
        
        // Проверка очереди хода
        if (parts[1] != "w" && parts[1] != "b") return false
        
        return true
    }
    
    companion object {
        /**
         * Создает объект ChessPuzzle из JSON-строки
         * 
         * @param json JSON-строка
         * @return Объект ChessPuzzle
         */
        fun fromJson(json: String): ChessPuzzle {
            return Gson().fromJson(json, ChessPuzzle::class.java)
        }
        
        /**
         * Пример задачи "мат в 2 хода"
         */
        fun createSampleMateIn2(): ChessPuzzle {
            return ChessPuzzle(
                id = "puzzle_001",
                initialFen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 1",
                solutionMoves = listOf("Bxf7+", "Kxf7", "Ng5+", "Kg8", "Qf3", "h6", "Qf7#"),
                puzzleType = "Мат в 3 хода",
                description = "Белые начинают и ставят мат в 3 хода"
            )
        }
        
        /**
         * Пример задачи "выигрыш материала"
         */
        fun createSampleMaterialWin(): ChessPuzzle {
            return ChessPuzzle(
                id = "puzzle_002",
                initialFen = "r1bqk2r/ppp2ppp/2n5/3np3/1bB5/2N2N2/PPPPQPPP/R1B1K2R w KQkq - 0 1",
                solutionMoves = listOf("Bxf7+", "Kxf7", "Nxe5+", "Ke8", "Nxc6"),
                puzzleType = "Выигрыш материала",
                description = "Белые начинают и выигрывают материал"
            )
        }
    }
}

 