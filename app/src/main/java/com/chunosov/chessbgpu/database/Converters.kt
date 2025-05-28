package com.chunosov.chessbgpu.database

import androidx.room.TypeConverter
import com.chunosov.chessbgpu.model.GameOptions
import com.chunosov.chessbgpu.model.Move
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMovesList(movesList: List<Move>): String {
        return gson.toJson(movesList.map { it.serialize() })
    }

    @TypeConverter
    fun toMovesList(movesJson: String): List<Move> {
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val movesData = gson.fromJson<List<Map<String, Any>>>(movesJson, type)
        return movesData.map { Move.deserialize(it) }
    }

    @TypeConverter
    fun fromGameOptions(gameOptions: GameOptions): String {
        return gson.toJson(gameOptions.serialize())
    }

    @TypeConverter
    fun toGameOptions(gameOptionsJson: String): GameOptions {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val gameOptionsData = gson.fromJson<Map<String, Any>>(gameOptionsJson, type)
        return GameOptions.deserialize(gameOptionsData)
    }
} 