package com.chunosov.chessbgpu.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Класс, представляющий коллекцию шахматных задач.
 *
 * @property puzzles Список задач в коллекции
 */
data class ChessPuzzleCollection(
    @SerializedName("puzzles") var puzzles: MutableList<ChessPuzzle> = mutableListOf()
) {
    /**
     * Преобразует коллекцию в JSON-строку.
     * @return JSON-представление коллекции
     */
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    companion object {
        /**
         * Создает объект ChessPuzzleCollection из JSON-строки.
         * @param json JSON-строка
         * @return Объект ChessPuzzleCollection
         */
        fun fromJson(json: String): ChessPuzzleCollection {
            return Gson().fromJson(json, ChessPuzzleCollection::class.java)
        }
        
        /**
         * Создает образец коллекции задач для тестирования.
         * @return Тестовая коллекция задач
         */
        fun createSampleCollection(): ChessPuzzleCollection {
            val collection = ChessPuzzleCollection()
            
            // Добавляем несколько простых задач
            collection.puzzles.add(ChessPuzzle.createSampleMateIn2())
            
            // Добавляем еще несколько простых задач
            collection.puzzles.add(
                ChessPuzzle(
                    id = "test_001",
                    initialFen = "r1b1k1nr/pppp1ppp/2n5/1B2p3/1b2P3/3P1N2/PPP2PPP/RNB1K2R w KQkq - 0 1",
                    solutionMoves = listOf("Bxc6#"),
                    puzzleType = "Мат в 1 ход",
                    description = "Белые начинают и ставят мат в 1 ход"
                )
            )
            
            return collection
        }
    }
} 