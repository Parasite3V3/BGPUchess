package com.chunosov.chessbgpu.data

/**
 * Класс, представляющий шахматную задачу
 */
data class ChessPuzzle(
    val id: String,
    val initialFen: String,
    val solutionMoves: List<String>,
    val puzzleType: String,
    val description: String,
    var currentFenHistory: MutableList<String> = mutableListOf(),
    var currentMoveIndex: Int = 0,
    var isSolved: Boolean = false
) {
    /**
     * Проверяет корректность FEN-строки
     */
    fun isFenValid(fen: String): Boolean {
        // Базовая проверка формата FEN
        val parts = fen.split(" ")
        if (parts.size < 6) return false
        
        // Проверка позиции фигур
        val position = parts[0]
        val ranks = position.split("/")
        if (ranks.size != 8) return false
        
        // Проверка цвета хода
        val turn = parts[1]
        if (turn != "w" && turn != "b") return false
        
        return true
    }
} 