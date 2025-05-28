package com.chunosov.chessbgpu.model

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс для хранения информации о сохраненной шахматной партии
 * 
 * @property id уникальный идентификатор партии
 * @property timestamp время создания записи (окончания игры)
 * @property moves список ходов в партии
 * @property moveCount количество ходов
 * @property result результат партии (например, "Белые выиграли", "Черные выиграли", "Ничья")
 * @property gameOptions настройки игры
 * @property formattedDate форматированная дата для отображения
 */
data class SavedGame(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val moves: List<Move> = emptyList(),
    val moveCount: Int = moves.size,
    val result: String = "",
    val gameOptions: GameOptions = GameOptions()
) {
    val formattedDate: String
        get() {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }

    fun serialize(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "timestamp" to timestamp,
            "moves" to moves.map { it.serialize() },
            "moveCount" to moveCount,
            "result" to result,
            "gameOptions" to gameOptions.serialize()
        )
    }
    
    companion object {
        fun deserialize(data: Map<String, Any>): SavedGame {
            @Suppress("UNCHECKED_CAST")
            val movesList = (data["moves"] as? List<Map<String, Any>> ?: emptyList()).map { Move.deserialize(it) }
            
            val gameOptionsMap = data["gameOptions"] as? Map<String, Any>
            val gameOptions = if (gameOptionsMap != null) {
                GameOptions.deserialize(gameOptionsMap)
            } else {
                GameOptions()
            }
            
            return SavedGame(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
                moves = movesList,
                moveCount = data["moveCount"] as? Int ?: movesList.size,
                result = data["result"] as? String ?: "",
                gameOptions = gameOptions
            )
        }
    }
}

enum class GameResult {
    WHITE_WIN,
    BLACK_WIN,
    DRAW,
    UNFINISHED
} 