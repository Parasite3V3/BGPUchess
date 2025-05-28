package com.chunosov.chessbgpu.data

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с шахматными задачами
 */
interface ChessPuzzleRepository {
    /**
     * Получает список всех задач
     */
    val puzzles: Flow<List<ChessPuzzle>>
    
    /**
     * Получает текущую активную задачу
     */
    val currentPuzzle: Flow<ChessPuzzle?>
    
    /**
     * Заменяет все задачи новым списком
     */
    suspend fun replacePuzzles(puzzles: List<ChessPuzzle>)
    
    /**
     * Устанавливает текущую задачу по ID
     */
    suspend fun setCurrentPuzzleById(id: String): Boolean
    
    /**
     * Устанавливает текущую задачу
     */
    suspend fun setCurrentPuzzle(puzzle: ChessPuzzle)
    
    /**
     * Проверяет ход игрока
     */
    suspend fun checkPlayerMove(move: String, resultingFen: String): Boolean
    
    /**
     * Получает ответный ход компьютера
     */
    suspend fun getComputerResponse(): String?
    
    /**
     * Делает ход компьютера
     */
    suspend fun makeComputerMove(resultingFen: String): Boolean
    
    /**
     * Проверяет, завершена ли текущая задача
     */
    suspend fun isCurrentPuzzleComplete(): Boolean
    
    /**
     * Отменяет последний ход
     */
    suspend fun undoLastMove(undoComputerMoveAlso: Boolean = true): String?
    
    /**
     * Сохраняет прогресс решения задач
     */
    suspend fun savePuzzleProgress()
    
    /**
     * Сбрасывает прогресс решения задач
     */
    suspend fun resetPuzzles()
    
    /**
     * Обновляет задачу
     */
    suspend fun updatePuzzle(puzzle: ChessPuzzle)
    
    /**
     * Отмечает задачу как решенную
     */
    suspend fun markPuzzleCompleted(puzzleId: String, markAsSolved: Boolean = true)
    
    /**
     * Проверяет, решена ли задача
     */
    suspend fun isPuzzleSolved(puzzleId: String): Boolean
} 