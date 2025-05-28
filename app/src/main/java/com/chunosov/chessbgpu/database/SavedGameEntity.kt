package com.chunosov.chessbgpu.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.chunosov.chessbgpu.model.GameOptions
import com.chunosov.chessbgpu.model.Move
import com.chunosov.chessbgpu.model.SavedGame
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val movesList: List<Move> = emptyList(),
    val moveCount: Int = movesList.size,
    val result: String = "",
    val gameOptions: GameOptions = GameOptions()
) {
    // Преобразование из Entity в модель
    fun toModel(): SavedGame {
        println("SavedGameEntity: преобразование Entity в модель, ID: $id")
        val savedGame = SavedGame(
            id = id,
            timestamp = timestamp,
            moves = movesList,
            moveCount = moveCount,
            result = result,
            gameOptions = gameOptions
        )
        println("SavedGameEntity: преобразование завершено, создана SavedGame с ID: ${savedGame.id}")
        return savedGame
    }

    companion object {
        // Преобразование из модели в Entity
        fun fromModel(savedGame: SavedGame): SavedGameEntity {
            println("SavedGameEntity: преобразование модели в Entity, ID: ${savedGame.id}")
            val entity = SavedGameEntity(
                id = savedGame.id,
                timestamp = savedGame.timestamp,
                movesList = savedGame.moves,
                moveCount = savedGame.moveCount,
                result = savedGame.result,
                gameOptions = savedGame.gameOptions
            )
            println("SavedGameEntity: преобразование завершено, создан Entity с ID: ${entity.id}")
            return entity
        }
    }
} 