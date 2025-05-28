package com.chunosov.chessbgpu.data

/**
 * Класс, представляющий коллекцию шахматных задач
 */
data class ChessPuzzleCollection(
    val puzzles: MutableList<ChessPuzzle>
) {
    /**
     * Получает задачу по ID
     */
    fun getPuzzleById(id: String): ChessPuzzle? {
        return puzzles.find { it.id == id }
    }
    
    /**
     * Получает задачи определенного типа
     */
    fun getPuzzlesByType(type: String): List<ChessPuzzle> {
        return puzzles.filter { it.puzzleType == type }
    }
    
    /**
     * Добавляет новую задачу в коллекцию
     */
    fun addPuzzle(puzzle: ChessPuzzle) {
        puzzles.add(puzzle)
    }
    
    /**
     * Удаляет задачу из коллекции
     */
    fun removePuzzle(id: String) {
        puzzles.removeIf { it.id == id }
    }
    
    /**
     * Обновляет существующую задачу
     */
    fun updatePuzzle(puzzle: ChessPuzzle) {
        val index = puzzles.indexOfFirst { it.id == puzzle.id }
        if (index != -1) {
            puzzles[index] = puzzle
        }
    }
} 