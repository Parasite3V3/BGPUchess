package com.chunosov.chessbgpu.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllSavedGames(): Flow<List<SavedGameEntity>>
    
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    suspend fun getAllSavedGamesDirectly(): List<SavedGameEntity>

    @Query("SELECT * FROM saved_games WHERE id = :gameId")
    suspend fun getSavedGameById(gameId: String): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedGame(savedGame: SavedGameEntity)

    @Delete
    suspend fun deleteSavedGame(savedGame: SavedGameEntity)

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteSavedGameById(gameId: String)

    @Query("DELETE FROM saved_games")
    suspend fun deleteAllSavedGames()
} 