package com.chunosov.chessbgpu.model

/**
 * Перечисление для представления цвета шахматных фигур
 */
enum class PieceColor {
    WHITE,  // Белые фигуры
    BLACK;  // Черные фигуры
    
    /**
     * Возвращает противоположный цвет фигуры
     * @return противоположный цвет (BLACK для WHITE, WHITE для BLACK)
     */
    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
} 