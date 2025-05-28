package com.chunosov.chessbgpu.repository

import android.content.Context
import android.util.Log
import com.chunosov.chessbgpu.model.ChessPuzzle
import com.chunosov.chessbgpu.model.ChessPuzzleCollection
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Репозиторий для работы с шахматными задачами.
 * Отвечает за загрузку задач из JSON-файлов и предоставление доступа к ним.
 */
class PuzzleRepository(private val context: Context) {
    
    private val _puzzles = MutableStateFlow<List<ChessPuzzle>>(emptyList())
    val puzzles: StateFlow<List<ChessPuzzle>> = _puzzles
    
    // Имя файла для сохранения текущего прогресса
    private val PUZZLE_PROGRESS_FILENAME = "puzzle_progress.json"
    
    // Имена файлов с задачами по категориям
    private val MATE_IN_1_FILENAME = "puzzles_mate_in_1.json"
    private val MATE_IN_2_FILENAME = "puzzles_mate_in_2.json"
    private val MATE_IN_3_FILENAME = "puzzles_mate_in_3.json"
    private val TACTICAL_FILENAME = "puzzles_tactical.json"
    
    // Текущая активная задача
    private val _currentPuzzle = MutableStateFlow<ChessPuzzle?>(null)
    val currentPuzzle: StateFlow<ChessPuzzle?> = _currentPuzzle
    
    /**
     * Инициализирует репозиторий, загружая все доступные задачи.
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                // Вначале проверяем, есть ли сохраненный прогресс
                val progressCollection = loadPuzzleCollection(PUZZLE_PROGRESS_FILENAME)
                if (progressCollection != null && progressCollection.puzzles.isNotEmpty()) {
                    _puzzles.value = progressCollection.puzzles
                    Log.d(TAG, "Loaded ${progressCollection.puzzles.size} puzzles from progress")
                    return@withContext
                }
                
                // Если прогресса нет, загружаем задачи из ресурсов
                val allPuzzles = mutableListOf<ChessPuzzle>()
                
                // Загружаем задачи из разных файлов
                loadPuzzleCollection(MATE_IN_1_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(MATE_IN_2_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(MATE_IN_3_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(TACTICAL_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                
                // Если нет задач в ресурсах, создаем образец коллекции
                if (allPuzzles.isEmpty()) {
                    allPuzzles.addAll(ChessPuzzleCollection.createSampleCollection().puzzles)
                    Log.d(TAG, "Created sample collection with ${allPuzzles.size} puzzles")
                }
                
                _puzzles.value = allPuzzles
                Log.d(TAG, "Initialized repository with ${allPuzzles.size} puzzles")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing puzzle repository", e)
                
                // Если возникла ошибка, используем образец
                val sampleCollection = ChessPuzzleCollection.createSampleCollection()
                _puzzles.value = sampleCollection.puzzles
                Log.d(TAG, "Fallback to sample collection with ${sampleCollection.puzzles.size} puzzles")
            }
        }
    }
    
    /**
     * Загружает коллекцию задач из JSON-файла в assets или из внутреннего хранилища.
     */
    private suspend fun loadPuzzleCollection(filename: String): ChessPuzzleCollection? {
        return withContext(Dispatchers.IO) {
            try {
                // Сначала пробуем загрузить из внутреннего хранилища (для сохраненного прогресса)
                val internalFile = context.getFileStreamPath(filename)
                if (internalFile.exists()) {
                    context.openFileInput(filename).use { inputStream ->
                        val json = inputStream.bufferedReader().use { it.readText() }
                        return@withContext ChessPuzzleCollection.fromJson(json)
                    }
                }
                
                // Затем пробуем из assets (для предустановленных задач)
                context.assets.open(filename).use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    return@withContext ChessPuzzleCollection.fromJson(json)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error loading puzzle collection from $filename", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Сохраняет текущий прогресс по задачам.
     */
    suspend fun savePuzzleProgress() {
        withContext(Dispatchers.IO) {
            try {
                val collection = ChessPuzzleCollection(_puzzles.value.toMutableList())
                val json = collection.toJson()
                
                context.openFileOutput(PUZZLE_PROGRESS_FILENAME, Context.MODE_PRIVATE).use { out ->
                    out.write(json.toByteArray())
                }
                
                Log.d(TAG, "Saved puzzle progress with ${_puzzles.value.size} puzzles")
            } catch (e: IOException) {
                Log.e(TAG, "Error saving puzzle progress", e)
            }
        }
    }
    
    /**
     * Очищает сохраненный прогресс и перезагружает задачи из исходных файлов.
     * Используется для принудительного обновления головоломок.
     */
    suspend fun resetPuzzles() {
        withContext(Dispatchers.IO) {
            try {
                // Удаляем файл сохраненного прогресса
                val internalFile = context.getFileStreamPath(PUZZLE_PROGRESS_FILENAME)
                if (internalFile.exists()) {
                    internalFile.delete()
                    Log.d(TAG, "Deleted saved puzzle progress file")
                }
                
                // Загружаем задачи заново из ресурсов
                val allPuzzles = mutableListOf<ChessPuzzle>()
                
                // Загружаем задачи из разных файлов
                loadPuzzleCollection(MATE_IN_1_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(MATE_IN_2_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(MATE_IN_3_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                loadPuzzleCollection(TACTICAL_FILENAME)?.let { allPuzzles.addAll(it.puzzles) }
                
                // Если нет задач в ресурсах, создаем образец коллекции
                if (allPuzzles.isEmpty()) {
                    allPuzzles.addAll(ChessPuzzleCollection.createSampleCollection().puzzles)
                    Log.d(TAG, "Created sample collection with ${allPuzzles.size} puzzles")
                }
                
                _puzzles.value = allPuzzles
                Log.d(TAG, "Reset and reloaded puzzles: ${allPuzzles.size} puzzles")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting puzzles", e)
            }
        }
    }
    
    /**
     * Получает задачу по ID.
     *
     * @param id ID задачи
     * @return Задача или null, если не найдена
     */
    fun getPuzzleById(id: String): ChessPuzzle? {
        return _puzzles.value.find { it.id == id }
    }
    
    /**
     * Получает список задач по типу.
     *
     * @param type Тип задачи
     * @return Список задач данного типа
     */
    fun getPuzzlesByType(type: String): List<ChessPuzzle> {
        return _puzzles.value.filter { it.puzzleType.contains(type, ignoreCase = true) }
    }
    
    /**
     * Устанавливает текущую активную задачу.
     *
     * @param puzzle Задача для установки как текущей
     */
    fun setCurrentPuzzle(puzzle: ChessPuzzle) {
        _currentPuzzle.value = puzzle
        // Сбрасываем задачу в начальное состояние
        puzzle.reset()
    }
    
    /**
     * Устанавливает текущую активную задачу по ID.
     *
     * @param id ID задачи
     * @return true, если задача найдена и установлена, false в противном случае
     */
    fun setCurrentPuzzleById(id: String): Boolean {
        val puzzle = getPuzzleById(id) ?: return false
        setCurrentPuzzle(puzzle)
        return true
    }
    
    /**
     * Проверяет ход пользователя в текущей задаче.
     *
     * @param move Ход пользователя в алгебраической нотации
     * @param resultingFen FEN-строка после совершения хода
     * @return true, если ход правильный, false в противном случае
     */
    fun checkPlayerMove(move: String, resultingFen: String): Boolean {
        Log.d(TAG, "checkPlayerMove: Проверяем ход '$move' с FEN после хода: '$resultingFen'")
        
        val puzzle = _currentPuzzle.value
        if (puzzle == null) {
            Log.e(TAG, "checkPlayerMove: Текущая задача равна null")
            return false
        }
        
        Log.d(TAG, "checkPlayerMove: Текущая задача: ${puzzle.id}, текущий индекс хода: ${puzzle.currentMoveIndex}")
        Log.d(TAG, "checkPlayerMove: Ожидаемый ход: '${puzzle.solutionMoves.getOrNull(puzzle.currentMoveIndex)}'")
        
        val isCorrect = puzzle.makeMove(move, resultingFen)
        
        Log.d(TAG, "checkPlayerMove: Результат проверки хода: $isCorrect")
        
        // Если ход правильный:
        // 1. Обновляется currentMoveIndex в объекте puzzle
        // 2. В currentFenHistory добавляется новый FEN
        // 3. Сохраняется прогресс
        //
        // Если ход неправильный:
        // 1. currentMoveIndex и currentFenHistory не изменяются
        // 2. Вызывающий код должен восстановить предыдущее состояние доски
        //    из последнего FEN в currentFenHistory
        if (isCorrect) {
            // Сохраняем прогресс после успешного хода
            savePuzzleProgressAsync()
        }
        
        return isCorrect
    }
    
    /**
     * Возвращает ответный ход компьютера в текущей задаче, если это предусмотрено.
     *
     * @return Ход компьютера или null, если компьютер не должен ходить
     */
    fun getComputerResponse(): String? {
        return _currentPuzzle.value?.getComputerMove()
    }
    
    /**
     * Делает ход компьютера в текущей задаче.
     *
     * @param resultingFen FEN-строка после хода компьютера
     * @return true, если компьютер сделал ход, false в противном случае
     */
    fun makeComputerMove(resultingFen: String): Boolean {
        val puzzle = _currentPuzzle.value ?: return false
        val computerMove = puzzle.getComputerMove() ?: return false
        
        // Обновляем состояние задачи после хода компьютера:
        // 1. Увеличиваем currentMoveIndex, чтобы следующий ход был от пользователя
        // 2. Добавляем FEN после хода компьютера в currentFenHistory
        // 3. При неправильном ходе пользователя, доска будет восстановлена 
        //    к этому FEN (последнему правильному состоянию)
        puzzle.currentMoveIndex++
        puzzle.currentFenHistory.add(resultingFen)
        
        // Сохраняем прогресс после хода компьютера
        savePuzzleProgressAsync()
        
        return true
    }
    
    /**
     * Проверяет, завершена ли текущая задача.
     *
     * @return true, если задача завершена, false в противном случае
     */
    fun isCurrentPuzzleComplete(): Boolean {
        return _currentPuzzle.value?.isPuzzleComplete() ?: false
    }
    
    /**
     * Отменяет последний ход в текущей задаче.
     *
     * @param undoComputerMoveAlso true, если нужно отменить и ход компьютера
     * @return FEN-строка состояния доски после отмены или null, если отмена невозможна
     */
    fun undoLastMove(undoComputerMoveAlso: Boolean = true): String? {
        return _currentPuzzle.value?.undoMove(undoComputerMoveAlso)
    }
    
    /**
     * Асинхронно сохраняет прогресс по задачам.
     */
    private fun savePuzzleProgressAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            savePuzzleProgress()
        }
    }
    
    /**
     * Заменяет все текущие задачи на новые.
     * 
     * @param newPuzzles Новый список задач
     */
    fun replacePuzzles(newPuzzles: List<ChessPuzzle>) {
        _puzzles.value = newPuzzles.toList()
        // Сбрасываем текущую задачу
        _currentPuzzle.value = null
        // Сохраняем новый список задач
        savePuzzleProgressAsync()
    }
    
    /**
     * Отмечает задачу как решенную
     * 
     * @param puzzleId ID задачи
     * @param isSolved true, если задача решена, false в противном случае
     * @return true, если задача успешно отмечена, false в противном случае
     */
    fun markPuzzleCompleted(puzzleId: String, isSolved: Boolean = true): Boolean {
        val updatedPuzzles = _puzzles.value.toMutableList()
        val index = updatedPuzzles.indexOfFirst { it.id == puzzleId }
        
        if (index < 0) {
            return false // Задача не найдена
        }
        
        // Здесь можно добавить логику для сохранения статуса решения
        // Например, установка флага или сохранение в SharedPreferences
        
        // Обновляем список задач
        _puzzles.value = updatedPuzzles
        
        // Сохраняем обновленное состояние
        savePuzzleProgressAsync()
        
        return true
    }
    
    /**
     * Проверяет, решена ли задача с указанным ID
     * 
     * @param puzzleId ID задачи
     * @return true, если задача решена, false в противном случае
     */
    fun isPuzzleSolved(puzzleId: String): Boolean {
        // Здесь должен быть код для проверки статуса решения
        // задачи из хранилища (например, SharedPreferences)
        
        // Временная реализация, всегда возвращающая false
        return false
    }
    
    /**
     * Обновляет задачу в репозитории
     * 
     * @param puzzle Обновленная задача
     * @return true, если обновление прошло успешно
     */
    fun updatePuzzle(puzzle: ChessPuzzle): Boolean {
        val puzzles = _puzzles.value.toMutableList()
        val index = puzzles.indexOfFirst { it.id == puzzle.id }
        
        if (index < 0) {
            return false
        }
        
        puzzles[index] = puzzle
        _puzzles.value = puzzles
        
        // Сохраняем изменения
        savePuzzleProgressAsync()
        
        return true
    }
    
    companion object {
        private const val TAG = "PuzzleRepository"
    }
} 