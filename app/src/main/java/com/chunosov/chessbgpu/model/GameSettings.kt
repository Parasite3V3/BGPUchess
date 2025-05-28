package com.chunosov.chessbgpu.model

enum class Opponent {
    Bot, Friend
}

data class GameSettings(
    val opponent: Opponent,
    val timeControl: TimeControl,
    val playerColor: PieceColor
) 