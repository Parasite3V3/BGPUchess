package com.chunosov.chessbgpu.ui

import android.content.Context
import com.chunosov.chessbgpu.data.ChessPuzzle
import com.chunosov.chessbgpu.engine.ValidationResult
import com.chunosov.chessbgpu.model.MessageType
import com.chunosov.chessbgpu.model.PuzzleAdapter
import com.chunosov.chessbgpu.model.PuzzleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Адаптер для использования PuzzleManager в UI
 * Запускает suspend-функции в корутинах
 */
class PuzzleManagerUIAdapter(private val context: Context) {
    private val puzzleManager = PuzzleManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // Экспортируем StateFlow для UI
    val puzzles: StateFlow<List<ChessPuzzle>> = puzzleManager.puzzles
    val currentPuzzle: StateFlow<ChessPuzzle?> = puzzleManager.currentPuzzle
    val currentFen: StateFlow<String?> = puzzleManager.currentFen
    val isLoading: StateFlow<Boolean> = puzzleManager.isLoading
    val isChessLibReady: StateFlow<Boolean> = puzzleManager.isChessLibReady
    val currentMoveIndex: StateFlow<Int> = puzzleManager.currentMoveIndex
    val message: StateFlow<String?> = puzzleManager.message
    val messageType: StateFlow<MessageType?> = puzzleManager.messageType
    val showSuccessDialog: StateFlow<Boolean> = puzzleManager.showSuccessDialog
    
    /**
     * Инициализирует менеджер
     */
    fun initialize() {
        scope.launch {
            puzzleManager.initialize()
        }
    }
    
    /**
     * Загружает тестовые задачи
     */
    fun loadTestPuzzles() {
        puzzleManager.loadTestPuzzles()
    }
    
    /**
     * Загружает простые задачи (мат в 1 ход и мат в 2 хода)
     */
    fun loadSimplePuzzles() {
        puzzleManager.loadSimplePuzzles()
    }
    
    /**
     * Выбирает задачу по ID
     */
    fun selectPuzzleById(id: String, callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.selectPuzzleById(id)
            callback(result)
        }
    }
    
    /**
     * Выбирает задачу по индексу
     */
    fun selectPuzzleByIndex(index: Int, callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.selectPuzzleByIndex(index)
            callback(result)
        }
    }
    
    /**
     * Возвращает текущий FEN
     */
    fun getCurrentFen(): String? {
        return puzzleManager.getCurrentFen()
    }
    
    /**
     * Проверяет ход пользователя
     */
    fun checkUserMove(move: String, resultingFen: String, callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.checkUserMove(move, resultingFen)
            callback(result)
        }
    }
    
    /**
     * Возвращает ответный ход компьютера
     */
    fun getComputerResponse(callback: (String?) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.getComputerResponse()
            callback(result)
        }
    }
    
    /**
     * Делает ход компьютера
     */
    fun makeComputerMove(resultingFen: String, callback: (Boolean) -> Unit = {}) {
        scope.launch {
            println("DEBUG: PuzzleManagerUIAdapter.makeComputerMove: FEN = $resultingFen")
            val result = puzzleManager.makeComputerMove(resultingFen)
            println("DEBUG: PuzzleManagerUIAdapter.makeComputerMove: Результат = $result")
            callback(result)
        }
    }
    
    /**
     * Проверяет, завершена ли текущая задача
     */
    fun isPuzzleComplete(callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.isPuzzleComplete()
            callback(result)
        }
    }
    
    /**
     * Отменяет последний ход
     */
    fun undoLastMove(undoComputerMoveAlso: Boolean = true, callback: (String?) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.undoLastMove(undoComputerMoveAlso)
            callback(result)
        }
    }
    
    /**
     * Возвращает все доступные задачи определенного типа
     */
    fun getPuzzlesByType(type: String): List<ChessPuzzle> {
        return puzzleManager.getPuzzlesByType(type)
    }
    
    /**
     * Отмечает текущую задачу как решенную
     */
    fun markCurrentPuzzleCompleted(markAsSolved: Boolean = true) {
        scope.launch {
            puzzleManager.markCurrentPuzzleCompleted(markAsSolved)
        }
    }
    
    /**
     * Проверяет, решена ли задача с указанным ID
     */
    fun isPuzzleSolved(puzzleId: String, callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.isPuzzleSolved(puzzleId)
            callback(result)
        }
    }
    
    /**
     * Инициализирует шахматную библиотеку
     */
    fun initializeChessLib(callback: (Boolean) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.initializeChessLib()
            callback(result)
        }
    }
    
    /**
     * Проверяет задачу на корректность
     */
    fun validatePuzzle(puzzleId: String? = null, callback: (ValidationResult) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.validatePuzzle(puzzleId)
            callback(result)
        }
    }
    
    /**
     * Исправляет решение задачи
     */
    fun fixPuzzleSolution(puzzleId: String? = null, callback: (List<String>?) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.fixPuzzleSolution(puzzleId)
            callback(result)
        }
    }
    
    /**
     * Получает подсказку для текущего хода
     */
    fun getHint(callback: (String?) -> Unit = {}) {
        scope.launch {
            val result = puzzleManager.getHint()
            callback(result)
        }
    }
    
    /**
     * Проверяет, является ли ход правильным решением
     */
    fun checkMove(move: String): Boolean {
        return puzzleManager.checkMove(move)
    }
    
    /**
     * Делает ход и проверяет решение
     */
    fun makeMove(move: String) {
        puzzleManager.makeMove(move)
    }
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        puzzleManager.cleanup()
    }
    
    /**
     * Сбрасывает кэш и перезагружает задачи из исходных файлов ресурсов
     */
    fun resetPuzzles() {
        scope.launch {
            puzzleManager.resetPuzzles()
        }
    }
    
    /**
     * Удаляет все шахматные задачи и оставляет раздел пустым
     */
    fun removeAllPuzzles() {
        scope.launch {
            puzzleManager.removeAllPuzzles()
        }
    }
    
    /**
     * Получает информацию о том, чей сейчас ход в задаче
     */
    fun getCurrentTurn(): String {
        return puzzleManager.getCurrentTurn()
    }
    
    /**
     * Загружает и проверяет корректность тестовых задач
     */
    fun loadAndValidateTestPuzzles() {
        scope.launch {
            puzzleManager.loadAndValidateTestPuzzles()
        }
    }
    
    /**
     * Обновляет список задач после изменений
     */
    fun refreshPuzzles() {
        puzzleManager.refreshPuzzles()
    }
    
    /**
     * Обрабатывает ход компьютера во второй задаче
     */
    fun handleComputerMoveInMateInTwo() {
        puzzleManager.handleComputerMoveInMateInTwo()
    }
} 