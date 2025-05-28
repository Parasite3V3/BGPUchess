package com.chunosov.chessbgpu.repository

import android.content.Context
import com.chunosov.chessbgpu.database.ChessDatabase
import com.chunosov.chessbgpu.database.SavedGameEntity
import com.chunosov.chessbgpu.model.SavedGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Репозиторий для управления сохраненными играми с использованием Room
 */
class GameRoomRepository(context: Context) {
    private val savedGameDao = ChessDatabase.getDatabase(context).savedGameDao()

    /**
     * Получает поток всех сохраненных игр
     * @return Flow со списком сохраненных игр
     */
    fun getAllSavedGames(): Flow<List<SavedGame>> {
        println("GameRoomRepository: getAllSavedGames вызван")
        return savedGameDao.getAllSavedGames().map { entities ->
            val games = entities.map { it.toModel() }
            println("GameRoomRepository: getAllSavedGames вернул ${games.size} игр")
            games
        }
    }

    /**
     * Получает список всех сохраненных игр (блокирующий вызов)
     * @return Список сохраненных игр
     */
    suspend fun getSavedGamesList(): List<SavedGame> {
        println("GameRoomRepository: getSavedGamesList вызван")
        try {
            val entities = savedGameDao.getAllSavedGamesDirectly()
            println("GameRoomRepository: getSavedGamesList получил ${entities.size} записей из базы данных")
            val games = entities.map { it.toModel() }
            println("GameRoomRepository: getSavedGamesList вернул ${games.size} игр")
            return games
        } catch (e: Exception) {
            println("GameRoomRepository: getSavedGamesList ошибка: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Получает сохраненную игру по ID
     * @param gameId ID игры для получения
     * @return Сохраненная игра или null, если игра не найдена
     */
    suspend fun getSavedGameById(gameId: String): SavedGame? {
        println("GameRoomRepository: getSavedGameById вызван для ID: $gameId")
        val entity = savedGameDao.getSavedGameById(gameId)
        println("GameRoomRepository: getSavedGameById результат: ${entity != null}")
        return entity?.toModel()
    }

    /**
     * Сохраняет игру в базу данных
     * @param savedGame Игра для сохранения
     * @return true, если сохранение прошло успешно
     */
    suspend fun saveGame(savedGame: SavedGame): Boolean {
        println("GameRoomRepository: saveGame вызван для игры с ID: ${savedGame.id}")
        return try {
            val entity = SavedGameEntity.fromModel(savedGame)
            println("GameRoomRepository: saveGame создал entity для сохранения")
            savedGameDao.insertSavedGame(entity)
            println("GameRoomRepository: saveGame успешно сохранил игру в базу данных")
            true
        } catch (e: Exception) {
            println("GameRoomRepository: saveGame ошибка: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Удаляет сохраненную игру по ID
     * @param gameId ID игры для удаления
     */
    suspend fun deleteSavedGame(gameId: String) {
        println("GameRoomRepository: deleteSavedGame вызван для ID: $gameId")
        try {
            savedGameDao.deleteSavedGameById(gameId)
            println("GameRoomRepository: deleteSavedGame успешно удалил игру")
        } catch (e: Exception) {
            println("GameRoomRepository: deleteSavedGame ошибка: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Удаляет все сохраненные игры
     */
    suspend fun deleteAllSavedGames() {
        println("GameRoomRepository: deleteAllSavedGames вызван")
        try {
            savedGameDao.deleteAllSavedGames()
            println("GameRoomRepository: deleteAllSavedGames успешно удалил все игры")
        } catch (e: Exception) {
            println("GameRoomRepository: deleteAllSavedGames ошибка: ${e.message}")
            e.printStackTrace()
        }
    }
} 