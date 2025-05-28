package com.chunosov.chessbgpu.viewmodel

import com.chunosov.chessbgpu.model.PieceColor

/**
 * Определение состояний шахматной игры
 */
sealed class GameState {
    object NOT_STARTED : GameState()
    object Playing : GameState()
    object Stalemate : GameState()
    object WHITE_WINS_BY_TIME : GameState()
    object BLACK_WINS_BY_TIME : GameState()
    object Draw : GameState()
    data class Checkmate(val loser: PieceColor) : GameState()
    data class Check(val inCheck: PieceColor) : GameState()
    data class Resigned(val resignedColor: PieceColor) : GameState()
} 