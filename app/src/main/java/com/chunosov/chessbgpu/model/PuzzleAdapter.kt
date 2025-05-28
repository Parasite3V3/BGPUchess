package com.chunosov.chessbgpu.model

import com.chunosov.chessbgpu.data.ChessPuzzle as DataChessPuzzle

/**
 * Адаптер для преобразования между типами ChessPuzzle
 */
object PuzzleAdapter {
    /**
     * Преобразует ChessPuzzle из data в model
     */
    fun toModelPuzzle(dataPuzzle: DataChessPuzzle): ChessPuzzle {
        return ChessPuzzle(
            id = dataPuzzle.id,
            initialFen = dataPuzzle.initialFen,
            solutionMoves = dataPuzzle.solutionMoves,
            puzzleType = dataPuzzle.puzzleType,
            description = dataPuzzle.description
        )
    }
} 