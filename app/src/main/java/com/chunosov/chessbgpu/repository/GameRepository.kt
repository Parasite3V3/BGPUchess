package com.chunosov.chessbgpu.repository

import android.content.Context
import android.content.SharedPreferences
import com.chunosov.chessbgpu.model.GameOptions
import com.chunosov.chessbgpu.model.Move
import com.chunosov.chessbgpu.model.SavedGame
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Репозиторий для управления сохраненными играми
 */
class GameRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "chess_game_prefs"
        private const val SAVED_GAMES_KEY = "saved_games"
    }

    /**
     * Сохраняет игру в хранилище
     * @param savedGame Игра для сохранения
     * @return true, если сохранение прошло успешно
     */
    fun saveGame(savedGame: SavedGame): Boolean {
        try {
            println("GameRepository: Сохранение игры, ID: ${savedGame.id}")
            
            // Загружаем текущий список игр
            val currentGames = getSavedGames().toMutableList()
            println("GameRepository: Текущее количество игр: ${currentGames.size}")
            
            // Проверяем, существует ли уже игра с таким ID
            val existingIndex = currentGames.indexOfFirst { it.id == savedGame.id }
            if (existingIndex >= 0) {
                // Если игра с таким ID уже существует, заменяем её
                currentGames[existingIndex] = savedGame
                println("GameRepository: Обновление существующей игры с ID: ${savedGame.id}")
            } else {
                // Иначе добавляем новую игру
                currentGames.add(savedGame)
                println("GameRepository: Добавление новой игры с ID: ${savedGame.id}")
            }
            
            // Сохраняем все игры в JSON
            val gamesJson = gson.toJson(currentGames)
            
            // Используем commit() вместо apply() для синхронного сохранения
            val result = sharedPreferences.edit().putString(SAVED_GAMES_KEY, gamesJson).commit()
            
            // Проверяем результат сохранения
            println("GameRepository: Результат сохранения: $result")
            
            // Проверяем, что игра действительно сохранена
            val savedGames = getSavedGames()
            val savedGame = savedGames.find { it.id == savedGame.id }
            val saved = savedGame != null
            
            println("GameRepository: Проверка сохранения: ${if (saved) "УСПЕШНО" else "ОШИБКА"}")
            println("GameRepository: Новое количество игр: ${savedGames.size}")
            
            return saved
        } catch (e: Exception) {
            println("GameRepository: Ошибка при сохранении: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Получает список всех сохраненных игр
     * @return Список сохраненных игр
     */
    fun getSavedGames(): List<SavedGame> {
        try {
            val gamesJson = sharedPreferences.getString(SAVED_GAMES_KEY, null)
            println("GameRepository: Получены данные из SharedPreferences: ${gamesJson?.length ?: 0} символов")
            
            if (gamesJson == null) {
                println("GameRepository: Нет сохраненных игр")
                return emptyList()
            }
            
            val type = object : TypeToken<List<SavedGame>>() {}.type
            val games = gson.fromJson<List<SavedGame>>(gamesJson, type)
            println("GameRepository: Десериализовано ${games.size} игр")
            
            return games.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            println("GameRepository: Ошибка при получении игр: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Получает сохраненную игру по ID
     * @param gameId ID игры для получения
     * @return Сохраненная игра или null, если игра не найдена
     */
    fun getSavedGameById(gameId: String): SavedGame? {
        return getSavedGames().find { it.id == gameId }
    }

    /**
     * Удаляет сохраненную игру по ID
     * @param gameId ID игры для удаления
     */
    fun deleteSavedGame(gameId: String) {
        val currentGames = getSavedGames().toMutableList()
        val updatedGames = currentGames.filter { it.id != gameId }
        
        val gamesJson = gson.toJson(updatedGames)
        sharedPreferences.edit().putString(SAVED_GAMES_KEY, gamesJson).commit()
    }

    /**
     * Удаляет все сохраненные игры
     */
    fun deleteAllSavedGames() {
        sharedPreferences.edit().remove(SAVED_GAMES_KEY).commit()
    }
} 