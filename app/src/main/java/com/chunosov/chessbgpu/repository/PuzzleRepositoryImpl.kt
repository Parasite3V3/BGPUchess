package com.chunosov.chessbgpu.repository

import android.content.Context
import com.chunosov.chessbgpu.data.ChessPuzzle
import com.chunosov.chessbgpu.data.ChessPuzzleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.IOException

/**
 * Реализация репозитория для работы с шахматными задачами
 */
class PuzzleRepositoryImpl(private val context: Context) : ChessPuzzleRepository {
    private val _puzzles = MutableStateFlow<List<ChessPuzzle>>(emptyList())
    private val _currentPuzzle = MutableStateFlow<ChessPuzzle?>(null)
    
    override val puzzles: Flow<List<ChessPuzzle>> = _puzzles
    override val currentPuzzle: Flow<ChessPuzzle?> = _currentPuzzle
    
    /**
     * Загружает задачи из JSON-файлов в assets
     */
    suspend fun loadPuzzlesFromAssets() {
        try {
            val allPuzzles = mutableListOf<ChessPuzzle>()
            
            // Список файлов с задачами
            val puzzleFiles = listOf(
                "puzzles_mate_in_1.json",
                "puzzles_mate_in_2.json",
                "puzzles_mate_in_3.json",
                "puzzles_tactical.json"
            )
            
            // Загружаем задачи из каждого файла
            for (fileName in puzzleFiles) {
                val puzzles = loadPuzzlesFromFile(fileName)
                allPuzzles.addAll(puzzles)
            }
            
            // Обновляем список задач
            _puzzles.value = allPuzzles
            println("DEBUG: Загружено ${allPuzzles.size} задач из JSON-файлов")
        } catch (e: Exception) {
            println("DEBUG: Ошибка при загрузке задач из JSON-файлов: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Загружает задачи из конкретного JSON-файла
     */
    private fun loadPuzzlesFromFile(fileName: String): List<ChessPuzzle> {
        try {
            // Читаем содержимое файла
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val puzzlesArray = jsonObject.getJSONArray("puzzles")
            
            val puzzles = mutableListOf<ChessPuzzle>()
            
            // Парсим каждую задачу
            for (i in 0 until puzzlesArray.length()) {
                val puzzleObj = puzzlesArray.getJSONObject(i)
                
                val id = puzzleObj.getString("id")
                val initialFen = puzzleObj.getString("initial_fen")
                val puzzleType = puzzleObj.getString("puzzle_type")
                val description = puzzleObj.getString("description")
                
                // Получаем решение
                val solutionArray = puzzleObj.getJSONArray("solution_moves")
                val solutionMoves = mutableListOf<String>()
                for (j in 0 until solutionArray.length()) {
                    solutionMoves.add(solutionArray.getString(j))
                }
                
                // Создаем объект задачи
                val puzzle = ChessPuzzle(
                    id = id,
                    initialFen = initialFen,
                    solutionMoves = solutionMoves,
                    puzzleType = puzzleType,
                    description = description
                )
                
                puzzles.add(puzzle)
            }
            
            println("DEBUG: Загружено ${puzzles.size} задач из файла $fileName")
            return puzzles
        } catch (e: IOException) {
            println("DEBUG: Ошибка при чтении файла $fileName: ${e.message}")
            e.printStackTrace()
            return emptyList()
        } catch (e: Exception) {
            println("DEBUG: Ошибка при парсинге файла $fileName: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    override suspend fun replacePuzzles(puzzles: List<ChessPuzzle>) {
        _puzzles.value = puzzles
    }
    
    override suspend fun setCurrentPuzzleById(id: String): Boolean {
        val puzzle = _puzzles.value.find { it.id == id }
        if (puzzle != null) {
            _currentPuzzle.value = puzzle
            return true
        }
        return false
    }
    
    override suspend fun setCurrentPuzzle(puzzle: ChessPuzzle) {
        _currentPuzzle.value = puzzle
    }
    
    override suspend fun checkPlayerMove(move: String, resultingFen: String): Boolean {
        val currentPuzzle = _currentPuzzle.value ?: return false
        val currentMoveIndex = currentPuzzle.currentMoveIndex
        
        if (currentMoveIndex < currentPuzzle.solutionMoves.size) {
            val expectedMove = currentPuzzle.solutionMoves[currentMoveIndex]
            
            // Нормализуем ходы, удаляя символы '+', '#' и игнорируя регистр
            val normalizedExpectedMove = expectedMove.replace("+", "").replace("#", "").trim()
            val normalizedUserMove = move.replace("+", "").replace("#", "").trim()
            
            // Добавляем отладочную информацию
            println("DEBUG: Repository: Проверяем ход: '$move' (нормализованный: '$normalizedUserMove')")
            println("DEBUG: Repository: Ожидаемый ход: '$expectedMove' (нормализованный: '$normalizedExpectedMove')")
            println("DEBUG: Repository: Совпадают ли ходы: ${normalizedUserMove.equals(normalizedExpectedMove, ignoreCase = true)}")
            
            if (normalizedUserMove.equals(normalizedExpectedMove, ignoreCase = true)) {
                currentPuzzle.currentMoveIndex++
                currentPuzzle.currentFenHistory.add(resultingFen)
                return true
            }
        }
        return false
    }
    
    override suspend fun getComputerResponse(): String? {
        val currentPuzzle = _currentPuzzle.value ?: return null
        val currentMoveIndex = currentPuzzle.currentMoveIndex
        
        // Если это четный ход (после хода игрока), возвращаем ответный ход компьютера
        if (currentMoveIndex % 2 == 1 && currentMoveIndex < currentPuzzle.solutionMoves.size) {
            return currentPuzzle.solutionMoves[currentMoveIndex]
        }
        return null
    }
    
    override suspend fun makeComputerMove(resultingFen: String): Boolean {
        val currentPuzzle = _currentPuzzle.value ?: return false
        currentPuzzle.currentFenHistory.add(resultingFen)
        return true
    }
    
    override suspend fun isCurrentPuzzleComplete(): Boolean {
        val currentPuzzle = _currentPuzzle.value ?: return false
        return currentPuzzle.currentMoveIndex >= currentPuzzle.solutionMoves.size
    }
    
    override suspend fun undoLastMove(undoComputerMoveAlso: Boolean): String? {
        val currentPuzzle = _currentPuzzle.value ?: return null
        if (currentPuzzle.currentFenHistory.isEmpty()) return null
        
        // Удаляем последний ход
        currentPuzzle.currentFenHistory.removeAt(currentPuzzle.currentFenHistory.size - 1)
        currentPuzzle.currentMoveIndex--
        
        // Если нужно отменить и ход компьютера
        if (undoComputerMoveAlso && currentPuzzle.currentFenHistory.isNotEmpty()) {
            currentPuzzle.currentFenHistory.removeAt(currentPuzzle.currentFenHistory.size - 1)
            currentPuzzle.currentMoveIndex--
        }
        
        return currentPuzzle.currentFenHistory.lastOrNull()
    }
    
    override suspend fun savePuzzleProgress() {
        // Здесь можно добавить сохранение прогресса в SharedPreferences или базу данных
    }
    
    override suspend fun resetPuzzles() {
        _puzzles.value = emptyList()
        _currentPuzzle.value = null
    }
    
    override suspend fun updatePuzzle(puzzle: ChessPuzzle) {
        val index = _puzzles.value.indexOfFirst { it.id == puzzle.id }
        if (index != -1) {
            val updatedPuzzles = _puzzles.value.toMutableList()
            updatedPuzzles[index] = puzzle
            _puzzles.value = updatedPuzzles
        }
    }
    
    override suspend fun markPuzzleCompleted(puzzleId: String, markAsSolved: Boolean) {
        val puzzle = _puzzles.value.find { it.id == puzzleId } ?: return
        puzzle.isSolved = markAsSolved
        updatePuzzle(puzzle)
    }
    
    override suspend fun isPuzzleSolved(puzzleId: String): Boolean {
        return _puzzles.value.find { it.id == puzzleId }?.isSolved ?: false
    }
} 