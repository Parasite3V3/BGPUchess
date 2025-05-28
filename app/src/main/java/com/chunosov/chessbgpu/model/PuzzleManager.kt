package com.chunosov.chessbgpu.model

import android.content.Context
import android.util.Log
import com.chunosov.chessbgpu.data.ChessPuzzle
import com.chunosov.chessbgpu.data.ChessPuzzleCollection
import com.chunosov.chessbgpu.data.ChessPuzzleRepository
import com.chunosov.chessbgpu.repository.PuzzleRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.chunosov.chessbgpu.engine.PuzzleValidator
import com.chunosov.chessbgpu.engine.ChessLibAdapter
import com.chunosov.chessbgpu.engine.ValidationResult
import kotlinx.coroutines.delay

/**
 * Перечисление типов сообщений
 */
enum class MessageType {
    SUCCESS,
    ERROR
}

/**
 * Менеджер для загрузки и управления шахматными задачами.
 * Использует PuzzleRepository для получения данных и предоставляет
 * удобные методы для работы с задачами.
 */
class PuzzleManager(
    private val context: Context,
    private val repository: ChessPuzzleRepository,
    private val puzzleValidator: PuzzleValidator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    
    // Текущий список доступных задач
    private val _puzzles = MutableStateFlow<List<ChessPuzzle>>(emptyList())
    val puzzles: StateFlow<List<ChessPuzzle>> = _puzzles.asStateFlow()
    
    // Текущая активная задача
    private val _currentPuzzle = MutableStateFlow<ChessPuzzle?>(null)
    val currentPuzzle: StateFlow<ChessPuzzle?> = _currentPuzzle.asStateFlow()
    
    // Текущее состояние доски для активной задачи (FEN)
    private val _currentFen = MutableStateFlow<String?>(null)
    val currentFen: StateFlow<String?> = _currentFen.asStateFlow()
    
    // Статус загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Шахматная библиотека для анализа позиций
    private val chessLibAdapter by lazy { ChessLibAdapter.getInstance() }
    
    // Флаг, указывающий, инициализирована ли шахматная библиотека
    private val _isChessLibReady = MutableStateFlow(false)
    val isChessLibReady: StateFlow<Boolean> = _isChessLibReady.asStateFlow()
    
    // Текущий индекс хода в текущей задаче
    private val _currentMoveIndex = MutableStateFlow(0)
    val currentMoveIndex: StateFlow<Int> = _currentMoveIndex.asStateFlow()
    
    // Текущее сообщение
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // Тип текущего сообщения
    private val _messageType = MutableStateFlow<MessageType?>(null)
    val messageType: StateFlow<MessageType?> = _messageType.asStateFlow()
    
    // Флаг, указывающий, показывать ли диалог успеха
    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog: StateFlow<Boolean> = _showSuccessDialog.asStateFlow()
    
    init {
        // Подписываемся на изменения в репозитории
        scope.launch {
            repository.puzzles.collectLatest { puzzleList ->
                _puzzles.value = puzzleList
            }
        }
        
        scope.launch {
            repository.currentPuzzle.collectLatest { puzzle ->
                _currentPuzzle.value = puzzle
                _currentFen.value = puzzle?.currentFenHistory?.lastOrNull()
            }
        }
    }
    
    /**
     * Инициализирует менеджер, загружая задачи из репозитория
     */
    suspend fun initialize() {
        _isLoading.value = true
        try {
            // Загружаем задачи из JSON-файлов
            if (repository is PuzzleRepositoryImpl) {
                (repository as PuzzleRepositoryImpl).loadPuzzlesFromAssets()
                println("DEBUG: Задачи загружены из JSON-файлов")
            } else {
                // Устанавливаем пустой список задач, если репозиторий не поддерживает загрузку из JSON
            _puzzles.value = emptyList()
            println("DEBUG: Инициализация с пустым списком задач")
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка при инициализации: ${e.message}")
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Загружает тестовый набор задач (для быстрого тестирования)
     */
    fun loadTestPuzzles() {
        // Загружаем простые задачи
        loadSimplePuzzles()
    }
    
    /**
     * Загружает две простые задачи с конкретными решениями
     */
    fun loadSimplePuzzles() {
        // Создаем коллекцию из 2 задач
        val simplePuzzles = listOf(
            ChessPuzzle(
                id = "mate-in-one-001",
                initialFen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
                solutionMoves = listOf("Qxf7#"),
                puzzleType = "Мат в 1 ход",
                description = "Белые начинают и ставят мат в 1 ход. Решение: ферзь на f7."
            ),
            ChessPuzzle(
                id = "mate-in-two-001",
                initialFen = "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 0 1",
                solutionMoves = listOf("Qb8", "Nxb8", "Rd8#"),
                puzzleType = "Мат в 2 хода",
                description = "Белые начинают и ставят мат в 2 хода. Сначала ферзь идет на b8, затем после взятия конем, ладья ставит мат на d8."
            )
        )
        
        scope.launch {
            _isLoading.value = true
            // Очищаем текущий репозиторий и добавляем новые задачи
            val collection = ChessPuzzleCollection(simplePuzzles.toMutableList())
            repository.replacePuzzles(collection.puzzles)
            _isLoading.value = false
            
            println("DEBUG: Загружено 2 задачи: мат в 1 ход и мат в 2 хода")
            
            // Выбираем первую задачу как текущую
            if (simplePuzzles.isNotEmpty()) {
                repository.setCurrentPuzzle(simplePuzzles.first())
                _currentFen.value = simplePuzzles.first().initialFen
                println("DEBUG: Установлена текущая задача: ${simplePuzzles.first().id}")
            }
        }
    }
    
    /**
     * Выбирает задачу по ID и устанавливает ее как текущую
     * 
     * @param id ID задачи
     * @return true, если задача найдена и установлена, false в противном случае
     */
    suspend fun selectPuzzleById(id: String): Boolean {
        val result = repository.setCurrentPuzzleById(id)
        if (result) {
            // После установки задачи обновляем значение currentFen
            _currentFen.value = _currentPuzzle.value?.initialFen
        }
        return result
    }
    
    /**
     * Выбирает задачу по индексу в списке и устанавливает ее как текущую
     * 
     * @param index Индекс задачи в списке
     * @return true, если задача найдена и установлена, false в противном случае
     */
    suspend fun selectPuzzleByIndex(index: Int): Boolean {
        if (index < 0 || index >= _puzzles.value.size) {
            return false
        }
        repository.setCurrentPuzzle(_puzzles.value[index])
        // После установки задачи обновляем значение currentFen
        _currentFen.value = _currentPuzzle.value?.initialFen
        return true
    }
    
    /**
     * Возвращает текущее состояние доски (FEN) для активной задачи
     * 
     * @return FEN-строка или null, если нет активной задачи
     */
    fun getCurrentFen(): String? {
        return _currentPuzzle.value?.currentFenHistory?.lastOrNull()
    }
    
    /**
     * Проверяет ход пользователя
     * 
     * @param move Ход пользователя в алгебраической нотации
     * @param resultingFen FEN-строка после совершения хода
     * @return true, если ход правильный, false в противном случае
     */
    suspend fun checkUserMove(move: String, resultingFen: String): Boolean {
        println("DEBUG: checkUserMove получил ход: '$move' и FEN: '$resultingFen'")
        
        val currentPuzzle = _currentPuzzle.value
        if (currentPuzzle == null) {
            println("DEBUG: Ошибка: текущая задача равна null")
            return false
        }
        
        println("DEBUG: Текущая задача: ${currentPuzzle.id}")
        println("DEBUG: Ожидаемый ход: '${currentPuzzle.solutionMoves.getOrNull(currentPuzzle.currentMoveIndex)}'")
        
        val result = repository.checkPlayerMove(move, resultingFen)
        
        println("DEBUG: Результат проверки хода: $result")
        
        if (result) {
            _currentFen.value = resultingFen
        }
        return result
    }
    
    /**
     * Возвращает ответный ход компьютера, если это необходимо
     * 
     * @return Ход компьютера или null, если компьютер не должен ходить
     */
    suspend fun getComputerResponse(): String? {
        val currentPuzzle = _currentPuzzle.value
        if (currentPuzzle == null) {
            println("DEBUG: getComputerResponse: Текущая задача равна null")
            return null
        }
        
        // Для второй задачи (мат в 2 хода)
        if (currentPuzzle.id == "mate-in-two-001" && currentPuzzle.currentMoveIndex == 1) {
            // После первого хода белых (ферзь на b8), конь черных съедает ферзя
            println("DEBUG: getComputerResponse: Возвращаем ход коня на b8 для задачи мат в 2 хода")
            return "d7b8" // Конь с d7 на b8
        }
        
        // Стандартная логика для других задач
        return repository.getComputerResponse()
    }
    
    /**
     * Делает ход компьютера
     * 
     * @param resultingFen FEN-строка после хода компьютера
     * @return true, если компьютер сделал ход, false в противном случае
     */
    suspend fun makeComputerMove(resultingFen: String): Boolean {
        val currentPuzzle = _currentPuzzle.value
        if (currentPuzzle == null) {
            println("DEBUG: makeComputerMove: Текущая задача равна null")
            return false
        }
        
        // Для второй задачи (мат в 2 хода)
        if (currentPuzzle.id == "mate-in-two-001" && currentPuzzle.currentMoveIndex == 1) {
            // Конь черных съедает ферзя на b8
            _currentFen.value = resultingFen
            // Увеличиваем индекс хода для следующего хода пользователя (ладья на d8)
            _currentMoveIndex.value = 2
            return true
        }
        
        // Стандартная логика для других задач
        val result = repository.makeComputerMove(resultingFen)
        if (result) {
            _currentFen.value = resultingFen
        }
        return result
    }
    
    /**
     * Проверяет, завершена ли текущая задача
     * 
     * @return true, если задача завершена, false в противном случае
     */
    suspend fun isPuzzleComplete(): Boolean {
        return repository.isCurrentPuzzleComplete()
    }
    
    /**
     * Отменяет последний ход
     * 
     * @param undoComputerMoveAlso true, если нужно отменить и ход компьютера
     * @return FEN-строка состояния доски после отмены или null, если отмена невозможна
     */
    suspend fun undoLastMove(undoComputerMoveAlso: Boolean = true): String? {
        val fen = repository.undoLastMove(undoComputerMoveAlso)
        if (fen != null) {
            _currentFen.value = fen
        }
        return fen
    }
    
    /**
     * Возвращает все доступные задачи определенного типа
     * 
     * @param type Тип задачи (например, "мат в 2 хода")
     * @return Список задач указанного типа
     */
    fun getPuzzlesByType(type: String): List<ChessPuzzle> {
        return _puzzles.value.filter { it.puzzleType == type }
    }
    
    /**
     * Отмечает текущую задачу как решенную и сохраняет прогресс
     * 
     * @param markAsSolved true, если нужно пометить задачу как решенную
     */
    suspend fun markCurrentPuzzleCompleted(markAsSolved: Boolean = true) {
        val puzzle = _currentPuzzle.value ?: return
        
        // Сохраняем состояние решения в репозитории
        repository.markPuzzleCompleted(puzzle.id, markAsSolved)
        
        // Обновляем UI состояние
        scope.launch {
            // Обновляем список задач чтобы отразить новый статус
            val updatedPuzzles = _puzzles.value.toMutableList()
            val index = updatedPuzzles.indexOfFirst { it.id == puzzle.id }
            if (index >= 0) {
                _puzzles.value = updatedPuzzles
            }
        }
    }
    
    /**
     * Проверяет, решена ли задача с указанным ID
     * 
     * @param puzzleId ID задачи
     * @return true, если задача решена, false в противном случае
     */
    suspend fun isPuzzleSolved(puzzleId: String): Boolean {
        return repository.isPuzzleSolved(puzzleId)
    }
    
    /**
     * Получает информацию о том, чей сейчас ход в задаче
     * 
     * @return Строка "WHITE" или "BLACK", указывающая на цвет текущего хода
     */
    fun getCurrentTurn(): String {
        val currentPuzzle = _currentPuzzle.value ?: return "WHITE"
        val fen = _currentFen.value ?: currentPuzzle.initialFen
        
        // В FEN, информация о том, чей ход, находится в позиции после последнего пробела
        // Например: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        // Здесь "w" означает, что ход белых
        val parts = fen.split(" ")
        if (parts.size > 1) {
            return if (parts[1] == "w") "WHITE" else "BLACK"
        }
        
        return "WHITE" // По умолчанию возвращаем, что ход белых
    }
    
    /**
     * Сбрасывает кэш и перезагружает задачи из исходных файлов ресурсов.
     * Используется при обновлении задач.
     */
    suspend fun resetPuzzles() {
        _isLoading.value = true
        try {
            // Вызываем метод репозитория для сброса и перезагрузки задач
            repository.resetPuzzles()
            
            // Обновляем currentPuzzle и currentFen, если они установлены
            if (_currentPuzzle.value != null) {
                _currentPuzzle.value = null
                _currentFen.value = null
            }
            
            // Выводим отладочную информацию
            println("DEBUG: Задачи успешно сброшены и перезагружены. Всего задач: ${_puzzles.value.size}")
        } catch (e: Exception) {
            println("DEBUG: Ошибка при сбросе задач: ${e.message}")
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Инициализирует шахматную библиотеку ChessLibAdapter
     * 
     * @return true, если инициализация прошла успешно
     */
    suspend fun initializeChessLib(): Boolean {
        _isLoading.value = true
        val result = chessLibAdapter.initialize()
        _isChessLibReady.value = result
        _isLoading.value = false
        return result
    }
    
    /**
     * Проверяет задачу на корректность
     * 
     * @param puzzleId ID задачи для проверки или null для текущей задачи
     * @return Результат валидации
     */
    suspend fun validatePuzzle(puzzleId: String? = null): ValidationResult {
        val puzzle = if (puzzleId != null) {
            getPuzzleById(puzzleId)
        } else {
            _currentPuzzle.value
        }
        
        if (puzzle == null) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Задача не найдена"
            )
        }
        
        return puzzleValidator.validatePuzzle(com.chunosov.chessbgpu.model.PuzzleAdapter.toModelPuzzle(puzzle))
    }
    
    /**
     * Исправляет решение задачи с помощью ChessLibAdapter
     * 
     * @param puzzleId ID задачи или null для текущей задачи
     * @return Исправленное решение или null, если исправление не требуется или невозможно
     */
    suspend fun fixPuzzleSolution(puzzleId: String? = null): List<String>? {
        val puzzle = if (puzzleId != null) {
            getPuzzleById(puzzleId)
        } else {
            _currentPuzzle.value
        }
        
        if (puzzle == null) {
            return null
        }
        
        // Получаем исправленное решение
        val fixedSolution = puzzleValidator.fixPuzzleSolution(com.chunosov.chessbgpu.model.PuzzleAdapter.toModelPuzzle(puzzle))
        
        // Если решение найдено, обновляем задачу
        if (fixedSolution != null && fixedSolution.isNotEmpty()) {
            // Создаем копию задачи с новым решением
            val updatedPuzzle = puzzle.copy(
                solutionMoves = fixedSolution
            )
            
            // Обновляем задачу в репозитории
            repository.updatePuzzle(updatedPuzzle)
            
            // Если это текущая задача, обновляем её состояние
            if (_currentPuzzle.value?.id == updatedPuzzle.id) {
                repository.setCurrentPuzzle(updatedPuzzle)
            }
        }
        
        return fixedSolution
    }
    
    /**
     * Получает подсказку для текущего хода в задаче
     * 
     * @return Подсказка или null, если подсказка недоступна
     */
    suspend fun getHint(): String? {
        val puzzle = _currentPuzzle.value ?: return null
        
        // Получаем текущую позицию на доске
        val currentFen = _currentFen.value ?: puzzle.initialFen
        
        // Получаем следующий ход из решения
        if (_currentMoveIndex.value >= puzzle.solutionMoves.size) {
            Log.d(TAG, "getHint: Нет доступных ходов для подсказки")
            return null
        }
        
        val nextMove = puzzle.solutionMoves[_currentMoveIndex.value]
        Log.d(TAG, "getHint: Следующий ход из решения: $nextMove")
        
        // Проверяем формат хода
        if (nextMove.length < 4) {
            Log.e(TAG, "getHint: Некорректный формат хода: $nextMove")
            return null
        }
        
        // Используем ChessLibAdapter для получения подсказки
        if (_isChessLibReady.value) {
            try {
                val hint = chessLibAdapter.getHint(currentFen, nextMove)
                if (hint != null) {
                    Log.d(TAG, "getHint: Получена подсказка от ChessLibAdapter: $hint")
                    return hint
                }
            } catch (e: Exception) {
                Log.e(TAG, "getHint: Ошибка при получении подсказки от ChessLibAdapter", e)
            }
        }
        
        // Если ChessLibAdapter не доступен или не смог дать подсказку,
        // возвращаем следующий ход из решения, если он в правильном формате
        return if (nextMove.length == 4) nextMove else null
    }
    
    /**
     * Получает лучший ход от ChessLibAdapter в текущей позиции
     * 
     * @param timeMs Время на расчет в миллисекундах
     * @return Лучший ход или null, если библиотека не инициализирована
     */
    suspend fun getBestMove(timeMs: Int = 1000): String? {
        if (!_isChessLibReady.value) {
            return null
        }
        
        val currentFen = _currentFen.value ?: return null
        return chessLibAdapter.findBestMove(currentFen, timeMs)
    }
    
    /**
     * Оценивает текущую позицию с помощью ChessLibAdapter
     * 
     * @param timeMs Время на расчет в миллисекундах
     * @return Оценка позиции или null, если библиотека не инициализирована
     */
    suspend fun evaluatePosition(timeMs: Int = 1000): Double? {
        if (!_isChessLibReady.value) {
            return null
        }
        
        val currentFen = _currentFen.value ?: return null
        return chessLibAdapter.evaluatePosition(currentFen, timeMs)
    }
    
    /**
     * Удаляет все шахматные задачи и оставляет раздел пустым
     */
    suspend fun removeAllPuzzles() {
        _isLoading.value = true
        try {
            // Создаем пустую коллекцию
            val emptyCollection = ChessPuzzleCollection(mutableListOf())
            
            // Обновляем репозиторий пустой коллекцией
            repository.replacePuzzles(emptyCollection.puzzles)
            
            // Сохраняем пустую коллекцию как прогресс
            repository.savePuzzleProgress()
            
            // Сбрасываем текущую задачу
            _currentPuzzle.value = null
            _currentFen.value = null
            
            println("DEBUG: Все шахматные задачи удалены")
        } catch (e: Exception) {
            println("DEBUG: Ошибка при удалении задач: ${e.message}")
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Освобождает ресурсы ChessLibAdapter при уничтожении менеджера
     */
    fun cleanup() {
        chessLibAdapter.cleanup()
        puzzleValidator.cleanup()
    }
    
    /**
     * Получает задачу по ID
     *
     * @param id ID задачи для получения
     * @return ChessPuzzle или null, если задача не найдена
     */
    fun getPuzzleById(id: String): ChessPuzzle? {
        return _puzzles.value.find { it.id == id }
    }
    
    /**
     * Обновляет список задач после изменений
     */
    fun refreshPuzzles() {
        // Создаем новый список на основе текущего для обновления StateFlow
        _puzzles.value = _puzzles.value.toList()
    }
    
    /**
     * Проверяет корректность всех загруженных задач
     */
    fun validateAllPuzzles(): List<String> {
        val errors = mutableListOf<String>()
        
        for (puzzle in _puzzles.value) {
            // Проверка FEN
            if (!puzzle.isFenValid(puzzle.initialFen)) {
                errors.add("Ошибка в задаче ${puzzle.id}: некорректный FEN")
                continue
            }
            
            // Проверка наличия решения
            if (puzzle.solutionMoves.isEmpty()) {
                errors.add("Ошибка в задаче ${puzzle.id}: отсутствует решение")
                continue
            }
            
            // Дополнительные проверки могут быть добавлены здесь
        }
        
        return errors
    }
    
    /**
     * Загружает и проверяет корректность тестовых задач
     */
    suspend fun loadAndValidateTestPuzzles() {
        // Сначала загружаем задачи
            _isLoading.value = true
        loadTestPuzzles()
        
        // Ждем немного, чтобы задачи успели загрузиться
        delay(500)
        
        if (_isChessLibReady.value) {
            // Проверяем каждую задачу
            var validCount = 0
            var invalidCount = 0
            var fixedCount = 0
            
            for (puzzle in _puzzles.value) {
                val result = validatePuzzle(puzzle.id)
                if (result.isValid) {
                    validCount++
                } else {
                    invalidCount++
                    // Пытаемся исправить задачу
                    val fixedSolution = fixPuzzleSolution(puzzle.id)
                    if (fixedSolution != null) {
                        fixedCount++
                    }
                }
            }
            
            println("DEBUG: Проверка завершена. Корректных задач: $validCount, некорректных: $invalidCount, исправлено: $fixedCount")
            
            // Обновляем список задач после проверки
            if (fixedCount > 0) {
                refreshPuzzles()
            }
        } else {
            println("DEBUG: Шахматная библиотека не инициализирована. Проверка невозможна.")
        }
        
        _isLoading.value = false
    }
    
    /**
     * Проверяет, является ли ход правильным решением
     */
    fun checkMove(move: String): Boolean {
        val currentPuzzle = currentPuzzle.value ?: return false
        val currentMoveIndex = currentMoveIndex.value
        
        println("DEBUG: checkMove: Проверка хода '$move', текущий индекс: $currentMoveIndex")
        println("DEBUG: checkMove: ID текущей задачи: ${currentPuzzle.id}")
        
        // Для первой задачи (мат в 1 ход)
        if (currentPuzzle.id == "mate-in-one-001") {
            // Проверяем, что ход - это ферзь на f7
            // Нормализуем ход, удаляя символы '+', '#', 'x'
            val normalizedMove = move.replace("+", "")
                .replace("#", "")
                .replace("x", "")
                .trim()
                .lowercase()
            
            println("DEBUG: checkMove: Нормализованный ход: '$normalizedMove'")
            
            // Проверяем, заканчивается ли ход на f7
            val result = normalizedMove.endsWith("f7") || 
                   (normalizedMove.length == 4 && normalizedMove.substring(2) == "f7")
            
            println("DEBUG: checkMove: Результат проверки для мат в 1 ход: $result")
            return result
        }
        
        // Для второй задачи (мат в 2 хода)
        if (currentPuzzle.id == "mate-in-two-001") {
            // Нормализуем ход
            val normalizedMove = move.replace("+", "")
                .replace("#", "")
                .replace("x", "")
                .trim()
                .lowercase()
            
            println("DEBUG: checkMove: Нормализованный ход: '$normalizedMove'")
            
            // Первый ход - ферзь на b8
            if (currentMoveIndex == 0) {
                // Проверяем, заканчивается ли ход на b8
                val result = normalizedMove.endsWith("b8") || 
                       (normalizedMove.length == 4 && normalizedMove.substring(2) == "b8")
                
                // Добавляем отладочную информацию
                println("DEBUG: checkMove: Проверка хода на b8: $result")
                println("DEBUG: checkMove: Конец хода: ${if (normalizedMove.length >= 2) normalizedMove.takeLast(2) else "слишком короткий ход"}")
                println("DEBUG: checkMove: Подстрока хода: ${if (normalizedMove.length >= 4) normalizedMove.substring(2) else "слишком короткий ход"}")
                
                return result
            }
            
            // Второй ход (после хода компьютера) - ладья на d8
            if (currentMoveIndex == 2) {
                // Проверяем, что это ход ладьей на d8
                // Возможные варианты: "d1d8", "rd8", "rxd8" и т.д.
                val result = normalizedMove.endsWith("d8") || 
                       (normalizedMove.length == 4 && normalizedMove.substring(2) == "d8") ||
                       (normalizedMove.startsWith("r") && normalizedMove.endsWith("d8"))
                
                println("DEBUG: checkMove: Проверка хода на d8: $result")
                println("DEBUG: checkMove: Ход: '$normalizedMove'")
                
                if (result) {
                    // Если ход правильный, устанавливаем флаг для показа диалога успеха
                    _showSuccessDialog.value = true
                }
                
                return result
            }
        }
        
        // Для других задач используем стандартную проверку
        if (currentMoveIndex < currentPuzzle.solutionMoves.size) {
            val expectedMove = currentPuzzle.solutionMoves[currentMoveIndex]
            
            // Нормализуем ходы, удаляя символы '+', '#', 'x' и игнорируя регистр
            val normalizedExpectedMove = expectedMove.replace("+", "")
                .replace("#", "")
                .replace("x", "")
                .trim()
                .lowercase()
            val normalizedUserMove = move.replace("+", "")
                .replace("#", "")
                .replace("x", "")
                .trim()
                .lowercase()
            
            println("DEBUG: checkMove: Нормализованный ход пользователя: '$normalizedUserMove'")
            println("DEBUG: checkMove: Нормализованный ожидаемый ход: '$normalizedExpectedMove'")
            
            // Если ход в формате e2e4, проверяем только конечную позицию
            if (normalizedUserMove.length == 4) {
                val userDestination = normalizedUserMove.substring(2, 4)
                
                // Извлекаем конечную позицию из ожидаемого хода
                val expectedDestination = if (normalizedExpectedMove.length >= 2) {
                    // Если ожидаемый ход в формате e2e4
                    if (normalizedExpectedMove.length == 4) {
                        normalizedExpectedMove.substring(2, 4)
                    } else {
                        // Если ожидаемый ход в формате Nf3, берем последние 2 символа
                        normalizedExpectedMove.takeLast(2)
                    }
                } else {
                    ""
                }
                
                println("DEBUG: checkMove: Конечная позиция пользователя: '$userDestination'")
                println("DEBUG: checkMove: Ожидаемая конечная позиция: '$expectedDestination'")
                
                if (userDestination == expectedDestination) {
                    println("DEBUG: checkMove: Конечные позиции совпадают")
                    return true
                }
            }
            
            // Если ход в формате Qf7 или Qxf7, проверяем только конечную позицию
            if (normalizedExpectedMove.length >= 2 && normalizedUserMove.length >= 2) {
                // Получаем конечные позиции из ходов
                val userDestination = if (normalizedUserMove.length == 4) {
                    normalizedUserMove.substring(2, 4)
                } else {
                    normalizedUserMove.takeLast(2)
                }
                
                val expectedDestination = if (normalizedExpectedMove.length == 4) {
                    normalizedExpectedMove.substring(2, 4)
                } else {
                    normalizedExpectedMove.takeLast(2)
                }
                
                println("DEBUG: checkMove: Конечная позиция пользователя: '$userDestination'")
                println("DEBUG: checkMove: Ожидаемая конечная позиция: '$expectedDestination'")
                
                if (userDestination == expectedDestination) {
                    println("DEBUG: checkMove: Конечные позиции совпадают")
                    return true
                }
            }
            
            // Если ничего не совпало, проверяем полное совпадение
            val isMatch = normalizedUserMove == normalizedExpectedMove
            println("DEBUG: checkMove: Полное совпадение ходов: $isMatch")
            return isMatch
        }
        
        println("DEBUG: checkMove: Индекс хода ($currentMoveIndex) выходит за пределы решения (${currentPuzzle.solutionMoves.size})")
        return false
    }

    /**
     * Делает ход и проверяет решение
     */
    fun makeMove(move: String) {
        val currentPuzzle = currentPuzzle.value ?: return
        val currentMoveIndex = currentMoveIndex.value
        
        println("DEBUG: makeMove: Делаем ход '$move', текущий индекс: $currentMoveIndex")
        
        // Проверяем правильность хода
        if (checkMove(move)) {
            // Ход правильный
            
            // Для первой задачи (мат в 1 ход)
            if (currentPuzzle.id == "mate-in-one-001") {
                // Задача решена после первого хода
                _currentMoveIndex.value = currentPuzzle.solutionMoves.size
                
                // Показываем сообщение об успехе
                _message.value = "Поздравляем! Задача решена!"
                _messageType.value = MessageType.SUCCESS
                
                // Показываем диалог успеха
                _showSuccessDialog.value = true
                
                // Отмечаем задачу как решенную
                scope.launch {
                    markCurrentPuzzleCompleted(true)
                }
                
                // Скрываем сообщение через 1 секунду
                scope.launch {
                    delay(1000)
                    _message.value = null
                }
                
                return
            }
            
            // Для второй задачи (мат в 2 хода)
            if (currentPuzzle.id == "mate-in-two-001") {
                if (currentMoveIndex == 0) {
                    // Первый ход - ферзь на b8
                    _currentMoveIndex.value = 1
                    
                    // Показываем сообщение о правильном ходе
                    _message.value = "Правильный ход! Ферзь на b8."
                    _messageType.value = MessageType.SUCCESS
                    
                    // Скрываем сообщение через 1 секунду
                    scope.launch {
                        delay(1000)
                        _message.value = null
                    }
                    
                    return
                } else if (currentMoveIndex == 2) {
                    // Второй ход - ладья на d8, задача решена
                    _currentMoveIndex.value = currentPuzzle.solutionMoves.size
                    
                    // Показываем сообщение об успехе
                    _message.value = "Поздравляем! Мат в 2 хода выполнен!"
                    _messageType.value = MessageType.SUCCESS
                    
                    // Показываем диалог успеха
                    _showSuccessDialog.value = true
                    
                    // Отмечаем задачу как решенную
                    scope.launch {
                        markCurrentPuzzleCompleted(true)
                    }
                    
                    // Скрываем сообщение через 1 секунду
                    scope.launch {
                        delay(1000)
                        _message.value = null
                    }
                    
                    println("DEBUG: makeMove: Задача мат в 2 хода решена!")
                    
                    return
                }
            }
            
            // Стандартная логика для других задач
            _currentMoveIndex.value = currentMoveIndex + 1
            
            // Если это последний ход в решении
            if (currentMoveIndex + 1 >= currentPuzzle.solutionMoves.size) {
                // Показываем сообщение об успехе
                _message.value = "Поздравляем! Задача решена!"
                _messageType.value = MessageType.SUCCESS
                
                // Показываем диалог успеха
                _showSuccessDialog.value = true
                
                // Отмечаем задачу как решенную
                scope.launch {
                    markCurrentPuzzleCompleted(true)
                }
            } else {
                // Показываем сообщение о правильном ходе
                _message.value = "Правильный ход! Продолжайте..."
                _messageType.value = MessageType.SUCCESS
            }
            
            // Скрываем сообщение через 1 секунду
            scope.launch {
                delay(1000)
                _message.value = null
            }
        } else {
            // Ход неправильный
            _message.value = "Неправильный ход. Попробуйте еще раз."
            _messageType.value = MessageType.ERROR
            
            // Скрываем сообщение через 0.5 секунды
            scope.launch {
                delay(500)
                _message.value = null
            }
        }
    }
    
    /**
     * Обрабатывает ход компьютера во второй задаче
     * Вызывается после того, как пользователь сделал первый ход в задаче мат в 2 хода
     */
    fun handleComputerMoveInMateInTwo() {
        val currentPuzzle = _currentPuzzle.value ?: return
        
        // Проверяем, что это вторая задача и мы на правильном шаге
        if (currentPuzzle.id == "mate-in-two-001" && _currentMoveIndex.value == 1) {
            // Устанавливаем индекс для следующего хода пользователя (ладья на d8)
            _currentMoveIndex.value = 2
            
            // Показываем сообщение о ходе компьютера
            _message.value = "Конь черных съедает ферзя! Теперь ваш ход - ладьей на d8 для мата."
            _messageType.value = MessageType.SUCCESS
            
            // Скрываем сообщение через 3 секунды
            scope.launch {
                delay(3000)
                _message.value = null
            }
            
            println("DEBUG: handleComputerMoveInMateInTwo: Индекс хода установлен на 2, ожидаем ход ладьей на d8")
        }
    }
    
    companion object {
        private const val TAG = "PuzzleManager"
        private var instance: PuzzleManager? = null
        
        /**
         * Получение экземпляра PuzzleManager (singleton)
         * 
         * @param context Контекст приложения
         * @return Экземпляр PuzzleManager
         */
        fun getInstance(context: Context): PuzzleManager {
            if (instance == null) {
                val repository = PuzzleRepositoryImpl(context.applicationContext)
                val validator = PuzzleValidator.getInstance(context)
                instance = PuzzleManager(
                    context.applicationContext,
                    repository,
                    validator
                )
            }
            return instance!!
        }
    }
} 