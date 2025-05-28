package com.chunosov.chessbgpu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chunosov.chessbgpu.model.*
import com.chunosov.chessbgpu.model.GameSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.UUID
import com.chunosov.chessbgpu.repository.GameRepository
import android.app.Application
import com.chunosov.chessbgpu.repository.GameRoomRepository

/**
 * Класс для хранения информации о ходе
 */
data class Move(
    val from: Position,
    val to: Position,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece?,
    val previousBoard: ChessBoard,
    val previousTurn: PieceColor,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isPromotion: Boolean = false,
    val promotedTo: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false
)

class ChessViewModel : AndroidViewModel {
    
    constructor(application: Application) : super(application) {
        gameRepository = GameRepository(application.applicationContext)
        resetGame()
        // Начальное состояние игры - готова к началу
        _gameState.value = GameState.NOT_STARTED
        // Загружаем сохраненные игры при создании ViewModel
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadSavedGames()
                println("Загружено сохраненных партий при инициализации: ${_savedGames.value.size}")
            } catch (e: Exception) {
                println("Ошибка при загрузке сохраненных партий: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Второй конструктор для превью и тестирования
    constructor() : super(Application()) {
        gameRepository = GameRepository(getApplication<Application>().applicationContext)
        resetGame()
        // Начальное состояние игры - готова к началу
        _gameState.value = GameState.NOT_STARTED
        // Загружаем сохраненные игры при создании ViewModel
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadSavedGames()
                println("Загружено сохраненных партий при инициализации: ${_savedGames.value.size}")
            } catch (e: Exception) {
                println("Ошибка при загрузке сохраненных партий: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private val _board = MutableStateFlow<ChessBoard>(WhiteChangeBoard())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()
    
    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition.asStateFlow()
    
    private val _selectedColor = MutableStateFlow<PieceColor>(PieceColor.WHITE)
    val selectedColor: StateFlow<PieceColor> = _selectedColor.asStateFlow()
    
    private val _currentTurn = MutableStateFlow(PieceColor.WHITE)
    val currentTurn: StateFlow<PieceColor> = _currentTurn.asStateFlow()
    
    private val _gameState = MutableStateFlow<GameState>(GameState.Playing)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val _boardState = MutableStateFlow<Map<Position, ChessPiece>>(mapOf())
    val boardState: StateFlow<Map<Position, ChessPiece>> = _boardState.asStateFlow()
    
    // Позиция короля под шахом (для подсветки)
    private val _kingInCheck = MutableStateFlow<Position?>(null)
    val kingInCheck: StateFlow<Position?> = _kingInCheck
    
    private var lastMovedPawn: Pair<Position, Boolean>? = null
    
    // Счетчик ходов
    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()
    
    // Настройки времени
    private val _timeControlEnabled = MutableStateFlow(false)
    val timeControlEnabled: StateFlow<Boolean> = _timeControlEnabled.asStateFlow()
    
    private var _initialTimeMillis = MutableStateFlow(5 * 60 * 1000L) // По умолчанию 5 минут
    val initialTimeMillis: StateFlow<Long> = _initialTimeMillis.asStateFlow()
    
    // Состояния времени
    private val _whiteTimeRemaining = MutableStateFlow(0L)
    val whiteTimeRemaining: StateFlow<Long> = _whiteTimeRemaining.asStateFlow()
    
    private val _blackTimeRemaining = MutableStateFlow(0L)
    val blackTimeRemaining: StateFlow<Long> = _blackTimeRemaining.asStateFlow()
    
    private var timerJob: Job? = null
    
    // История ходов
    private val _possibleMoves = MutableStateFlow<Set<Position>>(emptySet())
    val possibleMoves: StateFlow<Set<Position>> = _possibleMoves.asStateFlow()
    
    private val _moveHistory = MutableStateFlow<List<Move>>(emptyList())
    val moveHistory: StateFlow<List<Move>> = _moveHistory.asStateFlow()
    
    data class MoveInfo(
        val from: Position,
        val to: Position,
        val piece: ChessPiece,
        val capturedPiece: ChessPiece?,
        var isCheck: Boolean,
        var isCheckmate: Boolean,
        var isStalemate: Boolean,
        val previousBoard: ChessBoard
    )
    
    private var isPlayingAgainstBot = false
    private var timeControlMinutes = 0
    private var playerColor = PieceColor.WHITE
    private var isBoardFlipped = false
    
    private val _hasTimeControl = MutableStateFlow(false)
    val hasTimeControl: StateFlow<Boolean> = _hasTimeControl.asStateFlow()
    
    // Сохранённые игры
    private val _savedGames = MutableStateFlow<List<SavedGame>>(emptyList())
    val savedGames: StateFlow<List<SavedGame>> = _savedGames.asStateFlow()
    
    // Флаг для отображения диалога подтверждения выхода
    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()
    
    // Ключи для SharedPreferences
    companion object {
        private const val PREFS_NAME = "chess_game_prefs"
        private const val SAVED_GAMES_KEY = "saved_games"
    }

    private var context: Context? = null
    private lateinit var gameRepository: GameRepository
    
    // Флаг, указывающий, что игра находится в процессе
    private val _isGameInProgress = MutableStateFlow(false)
    val isGameInProgress: StateFlow<Boolean> = _isGameInProgress
    
    // Состояние диалога подтверждения выхода
    private val _showExitConfirmationDialog = MutableStateFlow(false)
    val showExitConfirmationDialog: StateFlow<Boolean> = _showExitConfirmationDialog

    // State для диалога подтверждения новой игры
    val _showNewGameConfirmationDialog = MutableStateFlow(false)
    val showNewGameConfirmationDialog: StateFlow<Boolean> = _showNewGameConfirmationDialog

    // State для диалога выбора фигуры при превращении пешки
    private val _showPawnPromotionDialog = MutableStateFlow(false)
    val showPawnPromotionDialog: StateFlow<Boolean> = _showPawnPromotionDialog.asStateFlow()
    
    // State для диалога окончания игры
    private val _showGameEndDialog = MutableStateFlow(false)
    val showGameEndDialog: StateFlow<Boolean> = _showGameEndDialog.asStateFlow()
    
    // Флаг, указывающий, сохранена ли текущая партия
    private val _isGameSaved = MutableStateFlow(false)
    val isGameSaved: StateFlow<Boolean> = _isGameSaved.asStateFlow()

    // Данные для превращения пешки
    private var pendingPromotion: Triple<Position, Position, PieceColor>? = null
    private var pendingPromotionPreviousBoard: ChessBoard? = null
    
    // Индекс текущего просматриваемого хода в истории
    private val _currentViewedMoveIndex = MutableStateFlow<Int>(-1)
    val currentViewedMoveIndex: StateFlow<Int> = _currentViewedMoveIndex.asStateFlow()

    // Счетчик для правила 50 ходов (увеличивается на 1 при каждом полуходе)
    private var _fiftyMoveRuleCounter = 0

    // Инициализация контекста для доступа к SharedPreferences
    fun initializeContext(appContext: Context) {
        context = appContext.applicationContext
        // Загружаем сохраненные игры при инициализации
        loadSavedGames()
    }

    fun setTimeControl(enabled: Boolean, timeMillis: Long = 5 * 60 * 1000L) {
        viewModelScope.launch {
            _timeControlEnabled.value = enabled
            _initialTimeMillis.value = timeMillis
            _hasTimeControl.value = enabled
            resetTimers()
        }
    }
    
    private fun resetTimers() {
        timerJob?.cancel()
        if (_timeControlEnabled.value) {
            _whiteTimeRemaining.value = _initialTimeMillis.value
            _blackTimeRemaining.value = _initialTimeMillis.value
            startTimer()
        } else {
            _whiteTimeRemaining.value = 0L
            _blackTimeRemaining.value = 0L
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _hasTimeControl.value) {
                delay(1000) // Обновляем каждую секунду
                when (_currentTurn.value) {
                    PieceColor.WHITE -> {
                        _whiteTimeRemaining.value = (_whiteTimeRemaining.value - 1000).coerceAtLeast(0)
                        if (_whiteTimeRemaining.value <= 0) {
                            _gameState.value = GameState.BLACK_WINS_BY_TIME
                            saveCurrentGame() // Сохраняем игру
                            _showGameEndDialog.value = true // Показываем диалог завершения игры
                            break
                        }
                    }
                    PieceColor.BLACK -> {
                        _blackTimeRemaining.value = (_blackTimeRemaining.value - 1000).coerceAtLeast(0)
                        if (_blackTimeRemaining.value <= 0) {
                            _gameState.value = GameState.WHITE_WINS_BY_TIME
                            saveCurrentGame() // Сохраняем игру
                            _showGameEndDialog.value = true // Показываем диалог завершения игры
                            break
                        }
                    }
                }
            }
        }
    }
    
    private fun updateBoardState() {
        val newState = mutableMapOf<Position, ChessPiece>()
        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)
                val piece = _board.value.getPiece(position)
                if (piece != null) {
                    newState[position] = piece
                }
            }
        }
        _boardState.value = newState
    }
    
    /**
     * Проверяет, находится ли король указанного цвета под шахом
     * @param color цвет короля
     * @return true, если король под шахом
     */
    private fun isKingInCheck(color: PieceColor): Boolean {
        // 1. Найти позицию короля указанного цвета
        val kingPosition = findKingPosition(color) ?: return false
        
        // 2. Проверить, атакует ли какая-либо фигура противника короля
        return isSquareUnderAttack(kingPosition, color.opposite())
    }
    
    /**
     * Проверяет, находится ли клетка под атакой фигур указанного цвета
     * @param position проверяемая позиция
     * @param attackerColor цвет атакующих фигур
     * @return true, если клетка под атакой
     */
    private fun isSquareUnderAttack(position: Position, attackerColor: PieceColor): Boolean {
        // Для каждой фигуры противника проверяем, может ли она атаковать указанную позицию
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = _board.value.getPiece(Position(row, col)) ?: continue
                
                // Если фигура не того цвета, пропускаем её
                if (piece.color != attackerColor) continue
                
                // Проверяем, может ли фигура атаковать позицию
                if (canAttack(Position(row, col), position)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Проверяет, может ли фигура с указанной позиции атаковать целевую позицию
     * @param from позиция фигуры
     * @param to целевая позиция
     * @return true, если фигура может атаковать целевую позицию
     */
    private fun canAttack(from: Position, to: Position): Boolean {
        val piece = _board.value.getPiece(from) ?: return false
        
        // Особая логика для пешек - они атакуют по диагонали
        if (piece.type == PieceType.PAWN) {
            val direction = if (piece.color == PieceColor.WHITE) -1 else 1
            return to.row == from.row + direction && Math.abs(to.col - from.col) == 1
        }
        
        // Для других фигур используем стандартную логику хода
        // Если фигура может сходить на клетку согласно правилам движения
        val isBlackBottom = when (_board.value) {
            is BlackChangeBoard -> true
            is WhiteChangeBoard -> false
            else -> piece.color == PieceColor.BLACK
        }
        
        // Проверяем, валидный ли это ход по правилам шахмат
        if (!_board.value.isValidMove(from, to, isBlackBottom)) {
            return false
        }
        
        // Для коня не нужно проверять путь
        if (piece.type == PieceType.KNIGHT) {
            return true
        }
        
        // Для других фигур проверяем, не заблокирован ли путь
        return isChessBoardPathClear(_board.value, from, to)
    }
    
    /**
     * Обновляет состояние игры на основе текущей позиции
     * Проверяет шах, мат и пат
     */
    private fun updateGameState() {
        // Цвет игрока, чей сейчас ход
        val currentPlayer = _currentTurn.value
        
        // 1. Проверяем, находится ли король текущего игрока под шахом
        val kingInCheck = isKingInCheck(currentPlayer)
        
        // 2. Если король под шахом, определяем позицию короля для подсветки
        if (kingInCheck) {
            _kingInCheck.value = findKingPosition(currentPlayer)
            _gameState.value = GameState.Check(currentPlayer)
            println("Король цвета $currentPlayer под шахом!")
            
            // Отладочная информация о легальных ходах при шахе
            val hasLegal = hasLegalMoves(currentPlayer)
            println("Есть ли легальные ходы при шахе: $hasLegal")
            
            // 3. Проверяем, есть ли у игрока легальные ходы для выхода из шаха
            if (!hasLegal) {
                // Если нет легальных ходов, это мат
                println("МАТ ОБНАРУЖЕН! Королю $currentPlayer объявлен мат!")
                
                // Устанавливаем состояние Checkmate
                _gameState.value = GameState.Checkmate(currentPlayer)
                
                // Завершаем игру и делаем все необходимые действия
                _isGameInProgress.value = false
                
                // Сбрасываем флаг сохранения игры (теперь игру нужно сохранять вручную)
                resetGameSavedFlag()
                
                // Явно устанавливаем флаг показа диалога и выводим отладочную информацию
                println("Устанавливаем флаг showGameEndDialog = true при мате")
                _showGameEndDialog.value = true // Показываем диалог завершения игры
                
                // Дополнительно выведем все текущие значения для отладки
                println("Текущие значения после мата:")
                println("gameState = ${_gameState.value}")
                println("isGameInProgress = ${_isGameInProgress.value}")
                println("showGameEndDialog = ${_showGameEndDialog.value}")
                return
            } else {
                // ВАЖНО: Если есть легальные ходы при шахе, игра продолжается
                // Не меняем _isGameInProgress и не показываем диалог
                println("Шах королю $currentPlayer, но есть легальные ходы для выхода")
                // Игра продолжается в состоянии Check
                _currentViewedMoveIndex.value = -1 // Сбрасываем индекс просмотра, чтобы не переходить в режим просмотра
            }
        } else {
            // Король не под шахом, сбрасываем подсветку
            _kingInCheck.value = null
            
            // 4. Проверяем на пат - нет шаха, но и нет легальных ходов
            if (!hasLegalMoves(currentPlayer)) {
                _gameState.value = GameState.Stalemate
                _isGameInProgress.value = false
                
                // Сбрасываем флаг сохранения игры
                resetGameSavedFlag()
                
                _showGameEndDialog.value = true // Показываем диалог завершения игры
                println("ПАТ! Нет легальных ходов, но и шаха нет")
                return
            } else {
                // Обычная игровая ситуация - нет шаха, есть легальные ходы
                _gameState.value = GameState.Playing
                _currentViewedMoveIndex.value = -1 // Сбрасываем индекс просмотра
            }
        }
        
        // 5. Проверяем правило 50 ходов
        if (_fiftyMoveRuleCounter >= 100) { // 50 ходов = 100 полуходов
            _gameState.value = GameState.Draw
            _isGameInProgress.value = false
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            _showGameEndDialog.value = true // Показываем диалог завершения игры
            println("Ничья по правилу 50 ходов")
            return
        }
    }
    
    /**
     * Проверяет, есть ли у игрока легальные ходы
     * @param color цвет игрока
     * @return true, если у игрока есть хотя бы один легальный ход
     */
    private fun hasLegalMoves(color: PieceColor): Boolean {
        // Перебираем все фигуры игрока
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = _board.value.getPiece(Position(row, col))
                // Если фигура принадлежит игроку
                if (piece != null && piece.color == color) {
                    // Проверяем все возможные ходы этой фигуры
                    if (getLegalMoves(Position(row, col)).isNotEmpty()) {
                        return true // Нашли хотя бы один легальный ход
                    }
                }
            }
        }
        return false // Нет легальных ходов
    }
    
    /**
     * Получает все легальные ходы для фигуры на указанной позиции
     * @param position позиция фигуры
     * @return множество легальных ходов
     */
    private fun getLegalMoves(position: Position): Set<Position> {
        val piece = _board.value.getPiece(position) ?: return emptySet()
        val color = piece.color
        val legalMoves = mutableSetOf<Position>()
        
        // Получаем все теоретически возможные ходы для этой фигуры
        val possibleMoves = getPossibleMoves(position)
        
        // Проверяем каждый ход на легальность (не оставляет ли короля под шахом)
        for (move in possibleMoves) {
            // Создаем временную копию доски
            val tempBoard = when (_board.value) {
                is BlackChangeBoard -> BlackChangeBoard()
                is WhiteChangeBoard -> WhiteChangeBoard()
                else -> ChessBoard()
            }
            
            // Копируем текущее состояние доски
            for (r in 0..7) {
                for (c in 0..7) {
                    val pos = Position(r, c)
                    val p = _board.value.getPiece(pos)
                    if (p != null) {
                        tempBoard.setPiece(pos, p.copy())
                    }
                }
            }
            
            // Выполняем ход на временной доске
            val movingPiece = tempBoard.getPiece(position)?.copy() ?: continue
            tempBoard.setPiece(position, null)
            tempBoard.setPiece(move, movingPiece)
            
            // Проверяем, не оставляет ли этот ход собственного короля под шахом
            val kingPos = findKingPositionOnBoard(tempBoard, color)
            if (kingPos != null && !isSquareUnderAttackOnBoard(tempBoard, kingPos, color.opposite())) {
                legalMoves.add(move)
            }
        }
        
        return legalMoves
    }
    
    /**
     * Находит позицию короля указанного цвета на указанной доске
     */
    private fun findKingPositionOnBoard(board: ChessBoard, color: PieceColor): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.getPiece(Position(row, col))
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }
    
    /**
     * Проверяет, находится ли клетка под атакой на указанной доске
     */
    private fun isSquareUnderAttackOnBoard(board: ChessBoard, position: Position, attackerColor: PieceColor): Boolean {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.getPiece(Position(row, col)) ?: continue
                
                if (piece.color != attackerColor) continue
                
                // Проверяем, может ли фигура атаковать позицию
                if (canAttackOnBoard(board, Position(row, col), position)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Проверяет, может ли фигура атаковать позицию на указанной доске
     */
    private fun canAttackOnBoard(board: ChessBoard, from: Position, to: Position): Boolean {
        val piece = board.getPiece(from) ?: return false
        
        // Особая логика для пешек
        if (piece.type == PieceType.PAWN) {
            val direction = if (piece.color == PieceColor.WHITE) -1 else 1
            return to.row == from.row + direction && Math.abs(to.col - from.col) == 1
        }
        
        // Для других фигур
        val isBlackBottom = when (board) {
            is BlackChangeBoard -> true
            is WhiteChangeBoard -> false
            else -> piece.color == PieceColor.BLACK
        }
        
        if (!board.isValidMove(from, to, isBlackBottom)) {
            return false
        }
        
        // Конь может перепрыгивать
        if (piece.type == PieceType.KNIGHT) {
            return true
        }
        
        // Проверяем путь
        return isChessBoardPathClear(board, from, to)
    }
    
    /**
     * Получает все возможные ходы фигуры без учета шаха
     */
    private fun getPossibleMoves(position: Position): Set<Position> {
        val piece = _board.value.getPiece(position) ?: return emptySet()
        val possibleMoves = mutableSetOf<Position>()
        
        // В зависимости от типа фигуры определяем возможные ходы
        when (piece.type) {
            PieceType.PAWN -> {
                // Ходы пешкой
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                
                // Ход вперед на одну клетку
                val forwardOne = Position(position.row + direction, position.col)
                if (isOnBoard(forwardOne) && _board.value.getPiece(forwardOne) == null) {
                    possibleMoves.add(forwardOne)
                    
                    // Ход вперед на две клетки с начальной позиции
                    if (!piece.hasMoved) {
                        val forwardTwo = Position(position.row + 2 * direction, position.col)
                        if (isOnBoard(forwardTwo) && _board.value.getPiece(forwardTwo) == null) {
                            possibleMoves.add(forwardTwo)
                        }
                    }
                }
                
                // Взятие по диагонали
                val captureLeft = Position(position.row + direction, position.col - 1)
                if (isOnBoard(captureLeft)) {
                    val pieceLeft = _board.value.getPiece(captureLeft)
                    if (pieceLeft != null && pieceLeft.color != piece.color) {
                        possibleMoves.add(captureLeft)
                    }
                }
                
                val captureRight = Position(position.row + direction, position.col + 1)
                if (isOnBoard(captureRight)) {
                    val pieceRight = _board.value.getPiece(captureRight)
                    if (pieceRight != null && pieceRight.color != piece.color) {
                        possibleMoves.add(captureRight)
                    }
                }
                
                // Взятие на проходе
                if (_moveHistory.value.isNotEmpty()) {
                    val lastMove = _moveHistory.value.last()
                    if (lastMove.piece.type == PieceType.PAWN && 
                        Math.abs(lastMove.from.row - lastMove.to.row) == 2 &&
                        Math.abs(lastMove.to.col - position.col) == 1 &&
                        lastMove.to.row == position.row) {
                        
                        val enPassantTarget = Position(position.row + direction, lastMove.to.col)
                        possibleMoves.add(enPassantTarget)
                    }
                }
            }
            
            PieceType.KNIGHT -> {
                // Ходы конем
                val knightMoves = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                
                for ((rowOffset, colOffset) in knightMoves) {
                    val newPos = Position(position.row + rowOffset, position.col + colOffset)
                    if (isOnBoard(newPos)) {
                        val targetPiece = _board.value.getPiece(newPos)
                        if (targetPiece == null || targetPiece.color != piece.color) {
                            possibleMoves.add(newPos)
                        }
                    }
                }
            }
            
            PieceType.BISHOP -> {
                // Ходы слоном
                addLinearMoves(position, listOf(
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                ), possibleMoves)
            }
            
            PieceType.ROOK -> {
                // Ходы ладьей
                addLinearMoves(position, listOf(
                    Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0)
                ), possibleMoves)
            }
            
            PieceType.QUEEN -> {
                // Ходы ферзем (комбинация ходов слона и ладьи)
                addLinearMoves(position, listOf(
                    Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0),
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                ), possibleMoves)
            }
            
            PieceType.KING -> {
                // Обычные ходы короля
                val kingMoves = listOf(
                    Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
                    Pair(0, -1),               Pair(0, 1),
                    Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
                )
                
                for ((rowOffset, colOffset) in kingMoves) {
                    val newPos = Position(position.row + rowOffset, position.col + colOffset)
                    if (isOnBoard(newPos)) {
                        val targetPiece = _board.value.getPiece(newPos)
                        if (targetPiece == null || targetPiece.color != piece.color) {
                            possibleMoves.add(newPos)
                        }
                    }
                }
                
                // Рокировка
                if (!piece.hasMoved && !isKingInCheck(piece.color)) {
                    // Короткая рокировка
                    val kingRook = Position(position.row, 7)
                    if (_board.value.getPiece(kingRook)?.type == PieceType.ROOK && 
                        !_board.value.getPiece(kingRook)!!.hasMoved) {
                        
                        val path = listOf(
                            Position(position.row, position.col + 1),
                            Position(position.row, position.col + 2)
                        )
                        
                        if (path.all { _board.value.getPiece(it) == null } && 
                            path.none { isSquareUnderAttack(it, piece.color.opposite()) }) {
                            possibleMoves.add(path[1])
                        }
                    }
                    
                    // Длинная рокировка
                    val queenRook = Position(position.row, 0)
                    if (_board.value.getPiece(queenRook)?.type == PieceType.ROOK && 
                        !_board.value.getPiece(queenRook)!!.hasMoved) {
                        
                        val path = listOf(
                            Position(position.row, position.col - 1),
                            Position(position.row, position.col - 2),
                            Position(position.row, position.col - 3)
                        )
                        
                        if (path.all { _board.value.getPiece(it) == null } && 
                            path.take(2).none { isSquareUnderAttack(it, piece.color.opposite()) }) {
                            possibleMoves.add(path[1])
                        }
                    }
                }
            }
        }
        
        return possibleMoves
    }
    
    /**
     * Добавляет линейные ходы для слона, ладьи и ферзя
     */
    private fun addLinearMoves(position: Position, directions: List<Pair<Int, Int>>, moves: MutableSet<Position>) {
        val piece = _board.value.getPiece(position) ?: return
        
        for ((rowDir, colDir) in directions) {
            var row = position.row + rowDir
            var col = position.col + colDir
            
            while (isOnBoard(Position(row, col))) {
                val targetPos = Position(row, col)
                val targetPiece = _board.value.getPiece(targetPos)
                
                if (targetPiece == null) {
                    // Пустая клетка - добавляем ход
                    moves.add(targetPos)
                } else if (targetPiece.color != piece.color) {
                    // Фигура противника - добавляем ход и прерываем направление
                    moves.add(targetPos)
                    break
                } else {
                    // Своя фигура - прерываем направление
                    break
                }
                
                row += rowDir
                col += colDir
            }
        }
    }
    
    /**
     * Проверяет, находится ли позиция на доске
     */
    private fun isOnBoard(position: Position): Boolean {
        return position.row in 0..7 && position.col in 0..7
    }

    /**
     * Проверяет текущее состояние игры (шах/мат/пат)
     * Обертка для функции updateGameState
     */
    private fun checkForCheck() {
        updateGameState()
    }
    
    /**
     * Отладочная функция для вывода информации о шахе
     */
    private fun debugKingInCheck(kingColor: PieceColor) {
        val kingPos = findKingPosition(kingColor) ?: return
        if (kingPos != null) {
            println("ОТЛАДКА: Король $kingColor на позиции $kingPos под шахом")
            val attackers = findAttackingPieces(kingPos, kingColor.opposite())
            println("Атакующие фигуры:")
            attackers.forEach { (pos, piece) ->
                println("- ${piece.type} на $pos")
            }
        }
    }
    
    private fun isPositionUnderAttack(position: Position, byColor: PieceColor): Boolean {
        return _boardState.value.any { (pos, piece) ->
            piece.color == byColor && 
            (if (piece.type == PieceType.PAWN) {
                // Для пешки проверяем только диагональные атаки
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                abs(pos.col - position.col) == 1 && 
                position.row == pos.row + direction
            } else {
                val isBlackBottom = when (_board.value) {
                    is BlackChangeBoard -> true
                    is WhiteChangeBoard -> false
                    else -> piece.color == PieceColor.BLACK
                }
                _board.value.isValidMove(pos, position, isBlackBottom)
            })
        }
    }
    
    /**
     * Отменяет последний ход
     * @return true, если ход был успешно отменен
     */
    fun undoLastMove(): Boolean {
        // Если история ходов пуста, нечего отменять
        if (_moveHistory.value.isEmpty()) {
            return false
        }
        
        // Получаем последний ход из истории
        val lastMove = _moveHistory.value.last()
        
        // Восстанавливаем предыдущее состояние доски
        val previousBoard = when (lastMove.previousBoard) {
            is BlackChangeBoard -> (lastMove.previousBoard as BlackChangeBoard).createCopy()
            is WhiteChangeBoard -> (lastMove.previousBoard as WhiteChangeBoard).createCopy()
            else -> lastMove.previousBoard.copy()
        }
        
        _board.value = previousBoard
        _currentTurn.value = lastMove.previousTurn
        _moveHistory.value = _moveHistory.value.dropLast(1)
        _selectedPosition.value = null
        _possibleMoves.value = emptySet()
        
        // Уменьшаем счетчик правила 50 ходов
        // При отмене хода уменьшаем счетчик на 1
        // Если отмененный ход был взятием или ходом пешки, сбрасываем счетчик
        val lastPiece = lastMove.piece
        val wasCapture = lastMove.capturedPiece != null || lastMove.isEnPassant
        val wasPawnMove = lastPiece.type == PieceType.PAWN
        
        if (wasCapture || wasPawnMove) {
            // Если мы отменяем взятие или ход пешки, нужно определить значение счетчика
            // из предыдущего состояния. Для простоты сбрасываем его до 0.
            _fiftyMoveRuleCounter = 0
        } else if (_fiftyMoveRuleCounter > 0) {
            _fiftyMoveRuleCounter--
        }
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Обновляем состояние игры
        updateGameState()
        
        return true
    }
    
    private fun findKingPosition(color: PieceColor): Position? {
        return _boardState.value.entries.find { (_, piece) -> 
            piece.type == PieceType.KING && piece.color == color 
        }?.key
    }
    
    /**
     * Возвращает путь между двумя позициями (для блокирования линии атаки)
     * @param from начальная позиция
     * @param to конечная позиция
     * @return множество позиций на пути
     */
    private fun getPath(from: Position, to: Position): Set<Position> {
        val path = mutableSetOf<Position>()
        
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        
        val rowStep = when {
            rowDiff > 0 -> 1
            rowDiff < 0 -> -1
            else -> 0
        }
        
        val colStep = when {
            colDiff > 0 -> 1
            colDiff < 0 -> -1
            else -> 0
        }
        
        var currentRow = from.row + rowStep
        var currentCol = from.col + colStep
        
        while (currentRow != to.row || currentCol != to.col) {
            path.add(Position(currentRow, currentCol))
            currentRow += rowStep
            currentCol += colStep
        }
        
        return path
    }
    
    /**
     * Проверяет, есть ли у игрока указанного цвета хотя бы один легальный ход
     */
    private fun checkPlayerHasLegalMoves(color: PieceColor): Boolean {
        // Перебираем все позиции на доске
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos)
                
                // Проверяем только фигуры нужного цвета
                if (piece?.color == color) {
                    // Если есть хотя бы один легальный ход, возвращаем true
                    val moves = calculatePossibleMoves(pos)
                    if (moves.isNotEmpty()) {
                        return true
                    }
                }
            }
        }
        
        // Если дошли до этого места, то нет легальных ходов
        return false
    }

    private fun calculatePossibleMoves(from: Position): Set<Position> {
        val possibleMoves = mutableSetOf<Position>()
        val piece = getPieceAt(from) ?: return possibleMoves
        
        // Если это не ход текущего игрока, возвращаем пустой список
        if (piece.color != _currentTurn.value) {
            return possibleMoves
        }
        
        // Определяем, является ли доска перевернутой
        val isBlackBottom = when (_board.value) {
            is BlackChangeBoard -> true
            is WhiteChangeBoard -> false
            else -> piece.color == PieceColor.BLACK
        }
        
        // Перебираем все клетки на доске для поиска возможных ходов
        for (row in 0..7) {
            for (col in 0..7) {
                val to = Position(row, col)
                if (from == to) continue  // Пропускаем текущую позицию
                
                // Проверяем, может ли фигура сделать базовый ход по правилам шахмат
                // или взятие на проходе или рокировку
                val isBasicMoveValid = 
                    _board.value.isValidMove(from, to, isBlackBottom) || 
                    isEnPassantPossible(from, to) || 
                    isCastlingPossible(from, to)
                
                if (isBasicMoveValid) {
                    // Создаем копию доски для проверки
        val tempBoard = when (_board.value) {
                        is BlackChangeBoard -> BlackChangeBoard()
                        is WhiteChangeBoard -> WhiteChangeBoard()
                        else -> ChessBoard()
                    }
                    
                    // Копируем текущее состояние доски
                    for (r in 0..7) {
                        for (c in 0..7) {
                            val pos = Position(r, c)
                            val p = _board.value.getPiece(pos)
                            if (p != null) {
                                tempBoard.setPiece(pos, p.copy())
                            }
                        }
                    }
                    
                    // Выполняем ход на временной доске
                    if (isEnPassantPossible(from, to)) {
                        // Особый случай - взятие на проходе
                        val captureRow = from.row
                        val captureCol = to.col
                        tempBoard.setPiece(Position(captureRow, captureCol), null)
                        tempBoard.setPiece(to, piece.copy())
        tempBoard.setPiece(from, null)
                    } else {
                        // Обычный ход
                        val movingPiece = piece.copy()
                        tempBoard.setPiece(from, null)
                        tempBoard.setPiece(to, movingPiece)
                    }
                    
                    // Проверяем, не оставит ли этот ход собственного короля под шахом
                    if (!tempBoard.isKingInCheck(piece.color)) {
                        possibleMoves.add(to)
                    }
                }
            }
        }
        
        return possibleMoves
    }

    // Вспомогательный метод для определения угроз королю
    private fun getThreatInfo(kingColor: PieceColor): List<Pair<Position, ChessPiece>> {
        val threats = mutableListOf<Pair<Position, ChessPiece>>()
        val kingPos = findKingPosition(kingColor) ?: return threats
        val oppositeColor = kingColor.opposite()
        
        // Перебираем все фигуры противника
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos) ?: continue
                
                if (piece.color == oppositeColor) {
                    val isBlackBottom = when (_board.value) {
                        is BlackChangeBoard -> true
                        is WhiteChangeBoard -> false
                        else -> piece.color == PieceColor.BLACK
                    }
                    
                    // Проверяем, может ли фигура атаковать короля
                    if (_board.value.isValidMove(pos, kingPos, isBlackBottom)) {
                        // Для всех фигур, кроме коня, нужно проверять путь
                        if (piece.type == PieceType.KNIGHT || isChessBoardPathClear(_board.value, pos, kingPos)) {
                            threats.add(pos to piece)
                        }
                    }
                }
            }
        }
        
        return threats
    }
    
    // Отладочный метод для диагностики проблем с шахом
    private fun debugCheckIssues() {
        // Получаем текущее состояние шаха
        val gameState = _gameState.value
        if (gameState is GameState.Check) {
            val kingColor = gameState.inCheck
            val kingPos = findKingPosition(kingColor) ?: return
            
            println("ОТЛАДКА ШАХА: Король $kingColor находится на позиции $kingPos")
            
            // Проверяем, какие фигуры атакуют короля
            val attackers = findAttackingPieces(kingPos, kingColor.opposite())
            
            println("Атакующие фигуры (${attackers.size}):")
            attackers.forEach { (pos, piece) ->
                println("- ${piece.type} на позиции $pos")
            }
            
            // Проверяем возможные ходы для короля
            val kingMoves = calculatePossibleMoves(kingPos)
            println("Возможные ходы короля (${kingMoves.size}):")
            kingMoves.forEach { move ->
                println("- $move")
            }
            
            // Проверяем, можно ли взять атакующую фигуру
            if (attackers.size == 1) {
                val (attackerPos, _) = attackers.entries.first()
                println("Проверяем возможность взятия атакующей фигуры на позиции $attackerPos")
                
                // Ищем фигуры, которые могут взять атакующую
                for (row in 0..7) {
                    for (col in 0..7) {
                        val pos = Position(row, col)
                        val piece = _board.value.getPiece(pos) ?: continue
                        
                        if (piece.color == kingColor) {
                            val legalMoves = calculatePossibleMoves(pos)
                            if (attackerPos in legalMoves) {
                                println("- ${piece.type} на позиции $pos может взять атакующую фигуру")
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Находит фигуры, атакующие указанную позицию короля
     * @param kingPos позиция короля
     * @param attackerColor цвет атакующих фигур
     * @return карта с позициями и фигурами атакующих
     */
    private fun findAttackingPieces(kingPos: Position, attackerColor: PieceColor): Map<Position, ChessPiece> {
        val attackers = mutableMapOf<Position, ChessPiece>()
        
        // Проверяем атаку от пешек
        val pawnDirection = if (attackerColor == PieceColor.WHITE) -1 else 1
        val pawnAttackCols = listOf(kingPos.col - 1, kingPos.col + 1)
        val pawnAttackRow = kingPos.row + pawnDirection
        
        if (pawnAttackRow in 0..7) {
            for (col in pawnAttackCols) {
                if (col in 0..7) {
                    val pos = Position(pawnAttackRow, col)
                    val piece = _board.value.getPiece(pos)
                    if (piece?.type == PieceType.PAWN && piece.color == attackerColor) {
                        attackers[pos] = piece
                    }
                }
            }
        }
        
        // Проверяем атаку от коней
        val knightMoves = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for ((rowOffset, colOffset) in knightMoves) {
            val row = kingPos.row + rowOffset
            val col = kingPos.col + colOffset
            if (row in 0..7 && col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos)
                if (piece?.type == PieceType.KNIGHT && piece.color == attackerColor) {
                    attackers[pos] = piece
                }
            }
        }
        
        // Проверяем атаку по горизонтали и вертикали (ладья, ферзь)
        val straightDirections = listOf(
            Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0)
        )
        
        for ((rowDir, colDir) in straightDirections) {
            var row = kingPos.row + rowDir
            var col = kingPos.col + colDir
            
            while (row in 0..7 && col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos)
                
                if (piece != null) {
                    if (piece.color == attackerColor && 
                        (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN)) {
                        attackers[pos] = piece
                    }
                    break // Если на пути стоит фигура, дальше не проверяем
                }
                
                row += rowDir
                col += colDir
            }
        }
        
        // Проверяем атаку по диагонали (слон, ферзь)
        val diagonalDirections = listOf(
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )
        
        for ((rowDir, colDir) in diagonalDirections) {
            var row = kingPos.row + rowDir
            var col = kingPos.col + colDir
            
            while (row in 0..7 && col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos)
                
                if (piece != null) {
                    if (piece.color == attackerColor && 
                        (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN)) {
                        attackers[pos] = piece
                    }
                    break // Если на пути стоит фигура, дальше не проверяем
                }
                
                row += rowDir
                col += colDir
            }
        }
        
        // Проверяем атаку от короля
        val kingMoves = listOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1),               Pair(0, 1),
            Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
        )
        
        for ((rowOffset, colOffset) in kingMoves) {
            val row = kingPos.row + rowOffset
            val col = kingPos.col + colOffset
            if (row in 0..7 && col in 0..7) {
                val pos = Position(row, col)
                val piece = _board.value.getPiece(pos)
                if (piece?.type == PieceType.KING && piece.color == attackerColor) {
                    attackers[pos] = piece
                }
            }
        }
        
        return attackers
    }

    /**
     * Выполняет ход фигурой
     * @param from начальная позиция
     * @param to конечная позиция
     */
    private fun makeMove(from: Position, to: Position) {
        val piece = _board.value.getPiece(from) ?: return
        
        // Проверяем, есть ли на целевой позиции фигура противника (взятие)
        val isCapture = _board.value.getPiece(to) != null
        
        // Проверяем специальные ситуации
        
        // 1. Рокировка
        if (piece.type == PieceType.KING && Math.abs(from.col - to.col) == 2) {
            performCastling(from, to)
            return
        }
        
        // 2. Взятие на проходе
        if (piece.type == PieceType.PAWN && from.col != to.col && !isCapture) {
            // Проверка на взятие на проходе - если пешка двигается по диагонали на пустую клетку
            // Это можно определить и без прямого доступа к enPassantTarget
            val targetPiece = _board.value.getPiece(to)
            if (targetPiece == null) {
                // Если целевая клетка пуста, и пешка движется по диагонали - это взятие на проходе
                performEnPassant(from, to)
                return
            }
        }
        
        // 3. Превращение пешки
        if (piece.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
            // Сохраняем информацию для превращения пешки
            pendingPromotion = Triple(from, to, piece.color)
            
            // Показываем диалог выбора фигуры
            _showPawnPromotionDialog.value = true
            return
        }
        
        // Стандартный ход
        _board.value.movePiece(from, to, _board.value is BlackChangeBoard)
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Сохраняем ход в историю
        val moveInfo = createMoveInfo(from, to, piece)
        addMoveToHistory(moveInfo)
        
        // Обновляем счетчик ходов и проверяем состояние игры
        _moveCount.value++
        
        // Важно! Проверяем статус игры после хода
        updateGameState()
        
        // Дополнительная проверка на мат после хода
        checkForCheckmate()
        
        // Если ход привел к мату, сразу показываем диалог завершения игры
        val gameState = _gameState.value
        if (gameState is GameState.Checkmate || gameState is GameState.Stalemate || 
            gameState == GameState.Draw || gameState == GameState.WHITE_WINS_BY_TIME || 
            gameState == GameState.BLACK_WINS_BY_TIME) {
            _isGameInProgress.value = false
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            // Явно устанавливаем флаг для отображения диалога
            println("makeMove: Игра завершена состоянием $gameState! Устанавливаем showGameEndDialog = true")
            _showGameEndDialog.value = true
            
            // Для отладки покажем все состояния
            println("makeMove после мата - текущие состояния:")
            println("gameState = $gameState")
            println("isGameInProgress = ${_isGameInProgress.value}")
            println("showGameEndDialog = ${_showGameEndDialog.value}")
        }
    }
    
    /**
     * Дополнительная проверка на мат
     * Используется для гарантированного обнаружения мата после хода
     */
    private fun checkForCheckmate() {
        // Проверяем мат для противника (того, кто должен ходить следующим)
        val opponentColor = _currentTurn.value.opposite()
        
        // Проверяем, под шахом ли король противника
        if (isKingInCheck(opponentColor)) {
            // Проверяем, может ли противник выйти из шаха
            if (!hasLegalMoves(opponentColor)) {
                // Это мат
                println("checkForCheckmate: МАТ ОБНАРУЖЕН! Королю $opponentColor объявлен мат!")
                _gameState.value = GameState.Checkmate(opponentColor)
                _isGameInProgress.value = false
                
                // Сбрасываем флаг сохранения игры
                resetGameSavedFlag()
                
                _showGameEndDialog.value = true
            }
        }
    }
    
    /**
     * Выполняет рокировку
     */
    private fun performCastling(kingFrom: Position, kingTo: Position) {
        val isKingside = kingTo.col > kingFrom.col
        val rookFrom = Position(kingFrom.row, if (isKingside) 7 else 0)
        val rookTo = Position(kingFrom.row, if (isKingside) kingTo.col - 1 else kingTo.col + 1)
        
        // Перемещаем короля
        val king = _board.value.getPiece(kingFrom)!!
        _board.value.setPiece(kingFrom, null)
        _board.value.setPiece(kingTo, king)
        king.hasMoved = true
        
        // Перемещаем ладью
        val rook = _board.value.getPiece(rookFrom)!!
        _board.value.setPiece(rookFrom, null)
        _board.value.setPiece(rookTo, rook)
        rook.hasMoved = true
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Создаем запись о ходе
        val moveInfo = createMoveInfo(
            from = kingFrom,
            to = kingTo,
            piece = king
        )
        addMoveToHistory(moveInfo)
        
        // Обновляем счетчик ходов и проверяем состояние игры
        _moveCount.value++
        
        // Проверяем статус игры после хода
        updateGameState()
        
        // Дополнительная проверка на мат
        checkForCheckmate()
        
        // Если ход привел к мату или пату, показываем диалог завершения игры
        val gameState = _gameState.value
        if (gameState is GameState.Checkmate || gameState is GameState.Stalemate || 
            gameState == GameState.Draw || gameState == GameState.WHITE_WINS_BY_TIME || 
            gameState == GameState.BLACK_WINS_BY_TIME) {
            _isGameInProgress.value = false
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            _showGameEndDialog.value = true
            println("Игра завершена! Вызвано отображение диалога окончания игры из performCastling")
        }
    }
    
    /**
     * Выполняет взятие на проходе
     */
    private fun performEnPassant(from: Position, to: Position) {
        val pawn = _board.value.getPiece(from)!!
        val capturedPawnPosition = Position(from.row, to.col)
        val capturedPawn = _board.value.getPiece(capturedPawnPosition)!!
        
        // Перемещаем пешку
        _board.value.setPiece(from, null)
        _board.value.setPiece(to, pawn)
        
        // Удаляем взятую пешку
        _board.value.setPiece(capturedPawnPosition, null)
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Создаем запись о ходе
        val moveInfo = Move(
            from = from,
            to = to,
            piece = pawn.copy(),
            capturedPiece = capturedPawn.copy(),
            previousBoard = _board.value.copy(),
            previousTurn = _currentTurn.value
        )
        addMoveToHistory(moveInfo)
        
        // Обновляем счетчик ходов и проверяем состояние игры
        _moveCount.value++
        
        // Проверяем статус игры после хода
        updateGameState()
        
        // Дополнительная проверка на мат
        checkForCheckmate()
        
        // Если ход привел к мату или пату, показываем диалог завершения игры
        val gameState = _gameState.value
        if (gameState is GameState.Checkmate || gameState is GameState.Stalemate || 
            gameState == GameState.Draw || gameState == GameState.WHITE_WINS_BY_TIME || 
            gameState == GameState.BLACK_WINS_BY_TIME) {
            _isGameInProgress.value = false
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            _showGameEndDialog.value = true
            println("Игра завершена! Вызвано отображение диалога окончания игры из performEnPassant")
        }
    }

    /**
     * Сбрасывает игру в начальное состояние
     */
    fun resetGame() {
        println("resetGame: начало метода, текущий isGameInProgress = ${_isGameInProgress.value}")
        viewModelScope.launch {
            // Важно! Сначала сохраняем текущий список партий
            val currentSavedGames = _savedGames.value
            
            // Инициализируем новую доску в зависимости от выбранного цвета игрока
            _board.value = if (playerColor == PieceColor.BLACK) {
                BlackChangeBoard()
            } else {
                WhiteChangeBoard()
            }
            _board.value.resetBoard()
            
            // Сбрасываем состояние хода и игры
            _currentTurn.value = PieceColor.WHITE
            _gameState.value = GameState.NOT_STARTED
            
            // Очищаем историю и выбранные позиции
            _moveHistory.value = emptyList()
            _selectedPosition.value = null
            _possibleMoves.value = emptySet()
            _kingInCheck.value = null
            
            // Сбрасываем счетчик ходов
            _moveCount.value = 0
            _fiftyMoveRuleCounter = 0
            
            // Обновляем состояние доски
            updateBoardState()
            
            // Сбрасываем таймеры
            resetTimers()
            
            // Игра готова к началу
            _isGameInProgress.value = false
            println("resetGame: установлен isGameInProgress = false")
            
            // Сбрасываем индекс просмотра сохраненной партии
            _currentViewedMoveIndex.value = -1
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            // Важно! Восстанавливаем сохраненные партии, чтобы они не потерялись
            _savedGames.value = currentSavedGames
            
            // На всякий случай перезагружаем сохраненные партии из хранилища через 100 мс
            delay(100)
            loadSavedGames()
            println("resetGame: завершение метода")
        }
    }
    
    /**
     * Переключает ход на следующего игрока
     */
    private fun switchTurn() {
        _currentTurn.value = _currentTurn.value.opposite()
    }
    
    /**
     * Добавляет ход в историю
     */
    private fun addMoveToHistory(move: Move) {
        _moveHistory.value = _moveHistory.value + move
    }

    /**
     * Загружает сохраненные игры из хранилища
     */
    fun loadSavedGames() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("loadSavedGames: начало метода")
                
                // Создаем Room репозиторий
                val gameRoomRepository = GameRoomRepository(getApplication())
                
                // Пытаемся загрузить игры из Room
                val roomGames = gameRoomRepository.getSavedGamesList()
                println("loadSavedGames: загружено игр из Room репозитория: ${roomGames.size}")
                
                if (roomGames.isNotEmpty()) {
                    // Если есть игры в Room, используем их
                    _savedGames.value = roomGames
                    println("loadSavedGames: установлены игры из Room репозитория")
                } else {
                    // Если нет игр в Room, загружаем из старого репозитория для обратной совместимости
                    val oldGames = gameRepository.getSavedGames()
                    println("loadSavedGames: загружено игр из старого репозитория: ${oldGames.size}")
                    
                    // Обновляем список сохраненных игр
                    _savedGames.value = oldGames
                    println("loadSavedGames: установлены игры из старого репозитория")
                    
                    // Мигрируем игры в Room для будущего использования
                    if (oldGames.isNotEmpty()) {
                        println("loadSavedGames: мигрируем ${oldGames.size} игр в Room репозиторий")
                        oldGames.forEach { game ->
                            val success = gameRoomRepository.saveGame(game)
                            println("loadSavedGames: миграция игры ${game.id}, результат: $success")
                        }
                    }
                }
                
                // Проверяем на потерю данных
                val currentSize = _savedGames.value.size
                if (roomGames.isEmpty() && currentSize > 0) {
                    // Если в Room нет игр, но в памяти есть, сохраняем их в Room
                    println("loadSavedGames: сохраняем ${currentSize} игр из памяти в Room репозиторий")
                    _savedGames.value.forEach { game ->
                        val success = gameRoomRepository.saveGame(game)
                        println("loadSavedGames: сохранение игры ${game.id} из памяти, результат: $success")
                    }
                }
                
                println("loadSavedGames: всего загружено игр: ${_savedGames.value.size}")
                _savedGames.value.forEach { game ->
                    println("loadSavedGames: загружена игра ${game.id}, результат: ${game.result}, ходов: ${game.moveCount}")
                }
            } catch (e: Exception) {
                println("loadSavedGames: ошибка при загрузке сохраненных игр: ${e.message}")
                e.printStackTrace()
                
                try {
                    val updatedGames = gameRepository.getSavedGames()
                    _savedGames.value = updatedGames
                    println("loadSavedGames: резервная загрузка из старого репозитория, игр: ${updatedGames.size}")
                } catch (e: Exception) {
                    println("loadSavedGames: ошибка при удалении игры: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Устанавливает настройки игры
     */
    fun setGameOptions(opponent: Opponent, timeControl: TimeControl, playerColor: PieceColor) {
        viewModelScope.launch {
            // Сохраняем настройки
            this@ChessViewModel.playerColor = playerColor
            this@ChessViewModel.isPlayingAgainstBot = opponent == Opponent.Bot
            
            // Устанавливаем контроль времени
            when (timeControl) {
                TimeControl.NoLimit -> setTimeControl(false)
                TimeControl.OneMinute -> setTimeControl(true, 1 * 60 * 1000L)
                TimeControl.FiveMinutes -> setTimeControl(true, 5 * 60 * 1000L)
                TimeControl.TenMinutes -> setTimeControl(true, 10 * 60 * 1000L)
            }
            
            // Инициализируем доску в соответствии с выбранным цветом
            _board.value = if (playerColor == PieceColor.BLACK) {
                BlackChangeBoard()
            } else {
                WhiteChangeBoard()
            }
            _board.value.resetBoard()
            
            // Обновляем состояние доски
            updateBoardState()
        }
    }

    /**
     * Применяет настройки игры и начинает новую партию
     */
    fun applyGameSettings() {
        println("applyGameSettings: начало метода")
        viewModelScope.launch {
            // Сбрасываем игру с текущими настройками
            resetGame()
            
            // Запускаем игру
            println("applyGameSettings: устанавливаем состояние игры в Playing и isGameInProgress в true")
            _gameState.value = GameState.Playing
            _isGameInProgress.value = true
            startTimers()
            println("applyGameSettings: завершение метода, isGameInProgress = ${_isGameInProgress.value}")
        }
    }

    /**
     * Обработка запроса на выход из игры
     */
    fun onGameExit() {
        if (_isGameInProgress.value && 
            _gameState.value != GameState.Checkmate(PieceColor.WHITE) && 
            _gameState.value != GameState.Checkmate(PieceColor.BLACK) &&
            _gameState.value != GameState.Stalemate &&
            _gameState.value != GameState.Draw &&
            _gameState.value != GameState.WHITE_WINS_BY_TIME &&
            _gameState.value != GameState.BLACK_WINS_BY_TIME) {
            // Игра в процессе и не завершена - показываем подтверждение
            _showExitConfirmationDialog.value = true
        } else {
            // Игра не начата или уже завершена - выходим без подтверждения
            confirmExit()
        }
    }

    /**
     * Скрывает диалог подтверждения новой игры
     */
    fun hideNewGameConfirmationDialog() {
        _showNewGameConfirmationDialog.value = false
    }

    /**
     * Отменяет выход из игры
     */
    fun cancelExit() {
        _showExitConfirmationDialog.value = false
    }

    /**
     * Подтверждает выход из игры
     */
    fun confirmExit() {
        _showExitConfirmationDialog.value = false
        
        // Если игра в процессе, сохраняем её
        if (_isGameInProgress.value && _moveHistory.value.isNotEmpty()) {
            saveCurrentGame()
        }
        
        // Сбрасываем игру
        resetGame()
    }

    /**
     * Превращает пешку в выбранную фигуру
     */
    fun promotePawn(pieceType: PieceType) {
        val (from, to, color) = pendingPromotion ?: return
        
        // Создаем новую фигуру выбранного типа
        val newPiece = when (pieceType) {
            PieceType.QUEEN -> Queen(color)
            PieceType.ROOK -> Rook(color)
            PieceType.BISHOP -> Bishop(color)
            PieceType.KNIGHT -> Knight(color)
            else -> Queen(color) // По умолчанию ферзь (хотя такого быть не должно)
        }
        
        // Устанавливаем новую фигуру на доску
        _board.value.setPiece(from, null)
        _board.value.setPiece(to, newPiece)
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Сохраняем ход в историю
        val moveInfo = Move(
            from = from,
            to = to,
            piece = Pawn(color),
            capturedPiece = _board.value.getPiece(to),
            previousBoard = pendingPromotionPreviousBoard ?: _board.value.copy(),
            previousTurn = _currentTurn.value,
            isPromotion = true,
            promotedTo = pieceType
        )
        addMoveToHistory(moveInfo)
        
        // Обновляем счетчик ходов и проверяем состояние игры
        _moveCount.value++
        
        // Меняем ход
        switchTurn()
        
        // Проверяем статус игры после хода
        updateGameState()
        
        // Сбрасываем данные о превращении
        pendingPromotion = null
        pendingPromotionPreviousBoard = null
        
        // Закрываем диалог
        _showPawnPromotionDialog.value = false
    }

    /**
     * Закрывает диалог превращения пешки
     */
    fun dismissPawnPromotionDialog() {
        _showPawnPromotionDialog.value = false
        pendingPromotion = null
        pendingPromotionPreviousBoard = null
    }

    /**
     * Сохраняет текущую игру в хранилище
     */
    fun saveCurrentGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Определяем результат игры
                val result = when (_gameState.value) {
                    is GameState.Checkmate -> {
                        val color = (_gameState.value as GameState.Checkmate).inCheck
                        if (color == PieceColor.WHITE) "Черные выиграли (мат)" else "Белые выиграли (мат)"
                    }
                    is GameState.Stalemate -> "Ничья (пат)"
                    is GameState.Draw -> "Ничья"
                    is GameState.WHITE_WINS_BY_TIME -> "Белые выиграли (время)"
                    is GameState.BLACK_WINS_BY_TIME -> "Черные выиграли (время)"
                    else -> ""
                }
                
                // Создаем объект сохраненной игры
                val savedGame = SavedGame(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    moves = _moveHistory.value.map { convertViewModelMoveToModelMove(it) },
                    moveCount = _moveHistory.value.size,
                    result = result,
                    gameOptions = GameOptions(
                        timeControl = if (_hasTimeControl.value) {
                            when (_initialTimeMillis.value) {
                                60 * 1000L -> TimeControl.OneMinute
                                5 * 60 * 1000L -> TimeControl.FiveMinutes
                                10 * 60 * 1000L -> TimeControl.TenMinutes
                                else -> TimeControl.NoLimit
                            }
                        } else TimeControl.NoLimit,
                        selectedColor = playerColor,
                        isAgainstBot = isPlayingAgainstBot
                    )
                )
                
                // Сохраняем игру в Room репозиторий
                val gameRoomRepository = GameRoomRepository(getApplication())
                val success = gameRoomRepository.saveGame(savedGame)
                
                // Также сохраняем в старый репозиторий для обратной совместимости
                gameRepository.saveGame(savedGame)
                
                // Обновляем список сохраненных игр
                loadSavedGames()
                
                println("Игра сохранена: ${savedGame.id}, результат: $result, успешно: $success")
            } catch (e: Exception) {
                println("Ошибка при сохранении игры: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Конвертирует Move из ViewModel в модельный Move
     */
    private fun convertViewModelMoveToModelMove(move: Move): com.chunosov.chessbgpu.model.Move {
        return com.chunosov.chessbgpu.model.Move(
            from = move.from,
            to = move.to,
            piece = move.piece,
            capturedPiece = move.capturedPiece,
            previousBoard = move.previousBoard,
            previousTurn = move.previousTurn,
            isCheck = move.isCheck,
            isCheckmate = move.isCheckmate,
            isStalemate = move.isStalemate,
            isPromotion = move.isPromotion,
            promotedTo = move.promotedTo,
            isEnPassant = move.isEnPassant,
            isCastling = move.isCastling
        )
    }

    /**
     * Конвертирует модельный Move в Move из ViewModel
     */
    private fun convertModelMoveToViewModelMove(move: com.chunosov.chessbgpu.model.Move): Move {
        return Move(
            from = move.from,
            to = move.to,
            piece = move.piece,
            capturedPiece = move.capturedPiece,
            previousBoard = move.previousBoard ?: _board.value.copy(),
            previousTurn = move.previousTurn ?: PieceColor.WHITE,
            isCheck = move.isCheck,
            isCheckmate = move.isCheckmate,
            isStalemate = move.isStalemate,
            isPromotion = move.isPromotion,
            promotedTo = move.promotedTo,
            isEnPassant = move.isEnPassant,
            isCastling = move.isCastling
        )
    }

    /**
     * Загружает сохраненную игру для просмотра
     * @param gameId ID сохраненной игры
     */
    fun viewSavedGame(gameId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Сначала пытаемся загрузить из Room
                val gameRoomRepository = GameRoomRepository(getApplication())
                val savedGame = gameRoomRepository.getSavedGameById(gameId)
                
                if (savedGame != null) {
                    // Загружаем игру из Room
                    loadSavedGameForViewing(savedGame)
                } else {
                    // Если не нашли в Room, пытаемся загрузить из старого репозитория
                    val oldGame = gameRepository.getSavedGameById(gameId)
                    if (oldGame != null) {
                        loadSavedGameForViewing(oldGame)
                    }
                }
            } catch (e: Exception) {
                println("Ошибка при загрузке сохраненной игры: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Загружает сохраненную игру для просмотра
     * @param savedGame объект сохраненной игры
     */
    private fun loadSavedGameForViewing(savedGame: SavedGame) {
        viewModelScope.launch {
            // Сбрасываем текущую игру
            resetGame()
            
            // Устанавливаем настройки из сохраненной игры
            when (savedGame.gameOptions.timeControl) {
                TimeControl.NoLimit -> setTimeControl(false)
                TimeControl.OneMinute -> setTimeControl(true, 1 * 60 * 1000L)
                TimeControl.FiveMinutes -> setTimeControl(true, 5 * 60 * 1000L)
                TimeControl.TenMinutes -> setTimeControl(true, 10 * 60 * 1000L)
            }
            
            // Сохраняем историю ходов
            _moveHistory.value = savedGame.moves.map { convertModelMoveToViewModelMove(it) }
            
            // Устанавливаем индекс просмотра на последний ход
            if (savedGame.moves.isNotEmpty()) {
                _currentViewedMoveIndex.value = savedGame.moves.size - 1
                // Применяем последний ход для отображения финальной позиции
                applyMoveAtIndex(savedGame.moves.size - 1)
            }
            
            // Устанавливаем состояние игры
            when {
                savedGame.result.contains("мат") -> {
                    if (savedGame.result.contains("Белые")) {
                        _gameState.value = GameState.Checkmate(PieceColor.BLACK)
                    } else {
                        _gameState.value = GameState.Checkmate(PieceColor.WHITE)
                    }
                }
                savedGame.result.contains("пат") -> _gameState.value = GameState.Stalemate
                savedGame.result.contains("Ничья") -> _gameState.value = GameState.Draw
                savedGame.result.contains("время") -> {
                    if (savedGame.result.contains("Белые")) {
                        _gameState.value = GameState.WHITE_WINS_BY_TIME
                    } else {
                        _gameState.value = GameState.BLACK_WINS_BY_TIME
                    }
                }
                else -> _gameState.value = GameState.Playing
            }
            
            // Игра не в процессе, а в режиме просмотра
            _isGameInProgress.value = false
        }
    }

    /**
     * Просматривает ход в сохраненной игре
     * @param moveIndex индекс хода для просмотра
     */
    fun viewSavedGameMove(moveIndex: Int) {
        if (moveIndex < -1 || moveIndex >= _moveHistory.value.size) return
        
        _currentViewedMoveIndex.value = moveIndex
        
        if (moveIndex == -1) {
            // Показываем начальную позицию
            resetBoardToInitial()
            return
        }
        
        // Применяем ход с указанным индексом
        applyMoveAtIndex(moveIndex)
    }

    /**
     * Применяет ход с указанным индексом
     * @param moveIndex индекс хода для применения
     */
    private fun applyMoveAtIndex(moveIndex: Int) {
        if (moveIndex < 0 || moveIndex >= _moveHistory.value.size) return
        
        // Сначала сбрасываем доску к начальной позиции
        resetBoardToInitial()
        
        // Применяем все ходы до указанного индекса включительно
        for (i in 0..moveIndex) {
            val move = _moveHistory.value[i]
            
            // Если это превращение пешки
            if (move.isPromotion && move.promotedTo != null) {
                val newPiece = when (move.promotedTo) {
                    PieceType.QUEEN -> Queen(move.piece.color)
                    PieceType.ROOK -> Rook(move.piece.color)
                    PieceType.BISHOP -> Bishop(move.piece.color)
                    PieceType.KNIGHT -> Knight(move.piece.color)
                    else -> Queen(move.piece.color)
                }
                _board.value.setPiece(move.from, null)
                _board.value.setPiece(move.to, newPiece)
            }
            // Если это взятие на проходе
            else if (move.isEnPassant) {
                val capturedPawnPosition = Position(move.from.row, move.to.col)
                _board.value.setPiece(move.from, null)
                _board.value.setPiece(capturedPawnPosition, null)
                _board.value.setPiece(move.to, move.piece)
            }
            // Если это рокировка
            else if (move.isCastling) {
                val isKingside = move.to.col > move.from.col
                val rookFrom = Position(move.from.row, if (isKingside) 7 else 0)
                val rookTo = Position(move.from.row, if (isKingside) move.to.col - 1 else move.to.col + 1)
                
                // Перемещаем короля
                _board.value.setPiece(move.from, null)
                _board.value.setPiece(move.to, move.piece)
                
                // Перемещаем ладью
                val rook = _board.value.getPiece(rookFrom)
                if (rook != null) {
                    _board.value.setPiece(rookFrom, null)
                    _board.value.setPiece(rookTo, rook)
                }
            }
            // Обычный ход
            else {
                _board.value.setPiece(move.from, null)
                _board.value.setPiece(move.to, move.piece)
            }
            
            // Обновляем текущий ход
            _currentTurn.value = if (i % 2 == 0) PieceColor.BLACK else PieceColor.WHITE
        }
        
        // Обновляем внутреннее состояние доски
        updateBoardState()
        
        // Проверяем шах
        val lastMove = _moveHistory.value[moveIndex]
        if (lastMove.isCheck) {
            _kingInCheck.value = findKingPosition(_currentTurn.value)
        } else {
            _kingInCheck.value = null
        }
    }
    
    /**
     * Сбрасывает доску к начальной позиции
     */
    private fun resetBoardToInitial() {
        _board.value = if (playerColor == PieceColor.BLACK) {
            BlackChangeBoard()
        } else {
            WhiteChangeBoard()
        }
        _board.value.resetBoard()
        _currentTurn.value = PieceColor.WHITE
        _kingInCheck.value = null
        updateBoardState()
    }

    /**
     * Удаляет сохраненную игру
     * @param gameId ID игры для удаления
     */
    fun deleteSavedGame(gameId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Удаляем из Room репозитория
                val gameRoomRepository = GameRoomRepository(getApplication())
                gameRoomRepository.deleteSavedGame(gameId)
                
                // Удаляем из старого репозитория для обратной совместимости
                gameRepository.deleteSavedGame(gameId)
                
                // Обновляем список сохраненных игр
                loadSavedGames()
                
                println("Игра удалена: $gameId")
            } catch (e: Exception) {
                println("Ошибка при удалении игры: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Проверяет, свободен ли путь между двумя позициями на доске
     * @param board доска для проверки
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если путь свободен
     */
    private fun isChessBoardPathClear(board: ChessBoard, from: Position, to: Position): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col

        // Направление движения
        val rowDir = when {
            rowDiff > 0 -> 1
            rowDiff < 0 -> -1
            else -> 0
        }
        
        val colDir = when {
            colDiff > 0 -> 1
            colDiff < 0 -> -1
            else -> 0
        }
        
        var currentRow = from.row + rowDir
        var currentCol = from.col + colDir
        
        // Проверяем все клетки на пути
        while (currentRow != to.row || currentCol != to.col) {
            if (board.getPiece(Position(currentRow, currentCol)) != null) {
                return false // Путь перекрыт
            }
            currentRow += rowDir
            currentCol += colDir
        }
        
        return true
    }

    /**
     * Получает фигуру на указанной позиции
     * @param position позиция на доске
     * @return фигура или null, если клетка пуста
     */
    private fun getPieceAt(position: Position): ChessPiece? {
        return _board.value.getPiece(position)
    }

    /**
     * Проверяет, возможно ли взятие на проходе
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если возможно взятие на проходе
     */
    private fun isEnPassantPossible(from: Position, to: Position): Boolean {
        val piece = getPieceAt(from) ?: return false
        
        // Взятие на проходе возможно только для пешек
        if (piece.type != PieceType.PAWN) return false
        
        // Проверяем, что пешка движется по диагонали
        if (Math.abs(from.col - to.col) != 1) return false
        
        // Направление движения пешки
        val direction = if (piece.color == PieceColor.WHITE) -1 else 1
        
        // Проверяем, что пешка движется вперед на одну клетку
        if (to.row - from.row != direction) return false
        
        // Проверяем, что целевая клетка пуста
        if (getPieceAt(to) != null) return false
        
        // Проверяем наличие пешки противника рядом
        val adjacentPosition = Position(from.row, to.col)
        val adjacentPiece = getPieceAt(adjacentPosition)
        
        // Должна быть пешка противника
        if (adjacentPiece?.type != PieceType.PAWN || adjacentPiece.color == piece.color) return false
        
        // Проверяем, был ли предыдущий ход двойным ходом пешки
        if (_moveHistory.value.isEmpty()) return false
        
        val lastMove = _moveHistory.value.last()
        return lastMove.piece.type == PieceType.PAWN && 
               Math.abs(lastMove.from.row - lastMove.to.row) == 2 &&
               lastMove.to.col == to.col &&
               lastMove.to.row == from.row
    }

    /**
     * Проверяет, возможна ли рокировка
     * @param from начальная позиция
     * @param to конечная позиция
     * @return true, если возможна рокировка
     */
    private fun isCastlingPossible(from: Position, to: Position): Boolean {
        val piece = getPieceAt(from) ?: return false
        
        // Рокировка возможна только для короля
        if (piece.type != PieceType.KING) return false
        
        // Король должен двигаться на две клетки по горизонтали
        if (from.row != to.row || Math.abs(from.col - to.col) != 2) return false
        
        // Король не должен был ходить ранее
        if (piece.hasMoved) return false
        
        // Определяем тип рокировки (короткая или длинная)
        val isKingside = to.col > from.col
        val rookPosition = Position(from.row, if (isKingside) 7 else 0)
        
        // Проверяем наличие ладьи
        val rook = getPieceAt(rookPosition)
        if (rook?.type != PieceType.ROOK || rook.color != piece.color || rook.hasMoved) return false
        
        // Проверяем, что путь между королем и ладьей свободен
        val path = if (isKingside) {
            (from.col + 1 until rookPosition.col).map { Position(from.row, it) }
        } else {
            (rookPosition.col + 1 until from.col).map { Position(from.row, it) }
        }
        
        if (path.any { getPieceAt(it) != null }) return false
        
        // Проверяем, что король не под шахом и не проходит через атакованные поля
        if (isKingInCheck(piece.color)) return false
        
        val kingPath = if (isKingside) {
            listOf(Position(from.row, from.col + 1))
        } else {
            listOf(Position(from.row, from.col - 1))
        }
        
        return kingPath.none { isSquareUnderAttack(it, piece.color.opposite()) }
    }

    /**
     * Создает информацию о ходе
     * @param from начальная позиция
     * @param to конечная позиция
     * @param piece фигура, которая делает ход
     * @return информация о ходе
     */
    private fun createMoveInfo(from: Position, to: Position, piece: ChessPiece): Move {
        // Сохраняем текущее состояние доски перед ходом
        val previousBoard = _board.value.copy()
        val previousTurn = _currentTurn.value
        
        // Определяем, есть ли взятие
        val capturedPiece = _board.value.getPiece(to)
        
                    // Выполняем ход на доске
                        _board.value.setPiece(from, null)
                        _board.value.setPiece(to, piece)
                        
        // Обновляем состояние доски
        updateBoardState()
        
        // Меняем ход
        switchTurn()
        
        // Проверяем, есть ли шах после хода
        val isCheck = isKingInCheck(_currentTurn.value)
        val isCheckmate = isCheck && !hasLegalMoves(_currentTurn.value)
        val isStalemate = !isCheck && !hasLegalMoves(_currentTurn.value)
        
        // Определяем, является ли ход рокировкой
        val isCastling = piece.type == PieceType.KING && Math.abs(from.col - to.col) == 2
        
        // Определяем, является ли ход взятием на проходе
        val isEnPassant = piece.type == PieceType.PAWN && 
                         from.col != to.col && 
                         capturedPiece == null
        
        // Создаем объект с информацией о ходе
        return Move(
            from = from,
            to = to,
            piece = piece,
            capturedPiece = capturedPiece,
            previousBoard = previousBoard,
            previousTurn = previousTurn,
            isCheck = isCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isEnPassant = isEnPassant,
            isCastling = isCastling
        )
    }

    /**
     * Запускает таймеры для контроля времени
     */
    private fun startTimers() {
        if (_timeControlEnabled.value) {
            resetTimers()
        }
    }

    /**
     * Показывает диалог подтверждения сдачи
     */
    private val _showResignConfirmationDialog = MutableStateFlow(false)
    val showResignConfirmationDialog: StateFlow<Boolean> = _showResignConfirmationDialog.asStateFlow()

    /**
     * Показывает диалог подтверждения сдачи
     */
    fun showResignConfirmation() {
        _showResignConfirmationDialog.value = true
    }

    /**
     * Подтверждает сдачу
     */
    fun confirmResign() {
        _showResignConfirmationDialog.value = false
        
        // Устанавливаем результат игры в зависимости от текущего хода
        _gameState.value = if (_currentTurn.value == PieceColor.WHITE) {
            GameState.BLACK_WINS_BY_RESIGNATION
        } else {
            GameState.WHITE_WINS_BY_RESIGNATION
        }
        
        // Завершаем игру и показываем диалог
        _isGameInProgress.value = false
        
        // Сбрасываем флаг сохранения игры
        resetGameSavedFlag()
        
        _showGameEndDialog.value = true
    }

    /**
     * Отменяет сдачу
     */
    fun cancelResign() {
        _showResignConfirmationDialog.value = false
    }

    /**
     * Обрабатывает нажатие на клетку доски
     * @param position позиция на доске
     */
    fun onSquareClick(position: Position) {
        // Отладочное логирование
        println("onSquareClick: позиция = $position, isGameInProgress = ${_isGameInProgress.value}, currentViewedMoveIndex = ${_currentViewedMoveIndex.value}")
        
        // Если игра не в процессе или просматривается сохраненная партия, ничего не делаем
        if (!_isGameInProgress.value || _currentViewedMoveIndex.value >= 0) {
            println("onSquareClick: выход из метода, т.к. игра не в процессе или просматривается сохраненная партия")
            return
        }
        
        val piece = _board.value.getPiece(position)
        println("onSquareClick: фигура на позиции = $piece, текущий ход = ${_currentTurn.value}")
        
        // Если уже выбрана фигура
        if (_selectedPosition.value != null) {
            val selectedPos = _selectedPosition.value!!
            val selectedPiece = _board.value.getPiece(selectedPos)
            println("onSquareClick: уже выбрана позиция = $selectedPos, фигура = $selectedPiece")
            
            // Если выбрана та же клетка, снимаем выделение
            if (selectedPos == position) {
                println("onSquareClick: снимаем выделение с той же клетки")
                _selectedPosition.value = null
                _possibleMoves.value = emptySet()
                return
            }
            
            // Если выбрана другая фигура того же цвета, меняем выделение
            if (piece != null && piece.color == selectedPiece?.color) {
                println("onSquareClick: меняем выделение на другую фигуру того же цвета")
                _selectedPosition.value = position
                _possibleMoves.value = calculatePossibleMoves(position)
                return
            }
            
            // Проверяем, возможен ли ход на выбранную клетку
            if (position in _possibleMoves.value) {
                println("onSquareClick: выполняем ход с $selectedPos на $position")
                // Выполняем ход
                makeMove(selectedPos, position)
                
                // Сбрасываем выделение
                _selectedPosition.value = null
                _possibleMoves.value = emptySet()
            } else {
                println("onSquareClick: ход невозможен, снимаем выделение")
                // Если ход невозможен, снимаем выделение
                _selectedPosition.value = null
                _possibleMoves.value = emptySet()
            }
        } else {
            // Если фигура не выбрана, выбираем фигуру, если она есть и принадлежит текущему игроку
            if (piece != null && piece.color == _currentTurn.value) {
                println("onSquareClick: выбираем фигуру $piece на позиции $position")
                _selectedPosition.value = position
                _possibleMoves.value = calculatePossibleMoves(position)
                println("onSquareClick: возможные ходы = ${_possibleMoves.value}")
            } else {
                println("onSquareClick: на позиции $position нет фигуры текущего игрока")
            }
        }
    }

    /**
     * Обрабатывает нажатие на кнопку "Новая игра" в диалоге окончания игры
     */
    fun onNewGameFromEndDialog() {
        _showGameEndDialog.value = false
        resetGame()
        _showNewGameConfirmationDialog.value = true
    }

    /**
     * Обрабатывает нажатие на кнопку "В меню" в диалоге окончания игры
     */
    fun onBackToMenuFromEndDialog() {
        _showGameEndDialog.value = false
    }

    /**
     * Просматривает предыдущий ход
     */
    fun viewPreviousMove() {
        if (_currentViewedMoveIndex.value > -1) {
            viewSavedGameMove(_currentViewedMoveIndex.value - 1)
        }
    }

    /**
     * Просматривает следующий ход
     */
    fun viewNextMove() {
        if (_currentViewedMoveIndex.value < _moveHistory.value.size - 1) {
            viewSavedGameMove(_currentViewedMoveIndex.value + 1)
        }
    }

    /**
     * Принудительно устанавливает мат (для отладки)
     */
    fun forceCheckmate() {
        _gameState.value = GameState.Checkmate(_currentTurn.value)
        _isGameInProgress.value = false
        saveCurrentGame()
        _showGameEndDialog.value = true
    }

    // Добавляем состояние для победы по сдаче
    sealed class GameState {
        object NOT_STARTED : GameState()
        object Playing : GameState()
        data class Check(val inCheck: PieceColor) : GameState()
        data class Checkmate(val inCheck: PieceColor) : GameState()
        object Stalemate : GameState()
        object Draw : GameState()
        object WHITE_WINS_BY_TIME : GameState()
        object BLACK_WINS_BY_TIME : GameState()
        object WHITE_WINS_BY_RESIGNATION : GameState()
        object BLACK_WINS_BY_RESIGNATION : GameState()
    }

    /**
     * Начинает игру - устанавливает флаг _isGameInProgress в true сразу
     * и применяет настройки игры без сброса
     */
    fun startGame() {
        println("startGame: начало метода")
        
        // Устанавливаем состояние игры
        _gameState.value = GameState.Playing
        _isGameInProgress.value = true
        
        // Запускаем таймеры, если они включены
        startTimers()
        
        println("startGame: завершение метода, isGameInProgress = ${_isGameInProgress.value}, gameState = ${_gameState.value}")
    }

    /**
     * Сохраняет текущую игру в хранилище и устанавливает флаг сохранения
     */
    fun saveCurrentGameManually() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("saveCurrentGameManually: начало метода")
                
                // Определяем результат игры
                val result = when (_gameState.value) {
                    is GameState.Checkmate -> {
                        val color = (_gameState.value as GameState.Checkmate).inCheck
                        if (color == PieceColor.WHITE) "Черные выиграли (мат)" else "Белые выиграли (мат)"
                    }
                    is GameState.Stalemate -> "Ничья (пат)"
                    is GameState.Draw -> "Ничья"
                    is GameState.WHITE_WINS_BY_TIME -> "Белые выиграли (время)"
                    is GameState.BLACK_WINS_BY_TIME -> "Черные выиграли (время)"
                    is GameState.WHITE_WINS_BY_RESIGNATION -> "Белые выиграли (сдача)"
                    is GameState.BLACK_WINS_BY_RESIGNATION -> "Черные выиграли (сдача)"
                    else -> ""
                }
                
                println("saveCurrentGameManually: результат игры = $result")
                println("saveCurrentGameManually: количество ходов = ${_moveHistory.value.size}")
                
                // Создаем объект сохраненной игры
                val savedGame = SavedGame(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    moves = _moveHistory.value.map { convertViewModelMoveToModelMove(it) },
                    moveCount = _moveHistory.value.size,
                    result = result,
                    gameOptions = GameOptions(
                        timeControl = if (_hasTimeControl.value) {
                            when (_initialTimeMillis.value) {
                                60 * 1000L -> TimeControl.OneMinute
                                5 * 60 * 1000L -> TimeControl.FiveMinutes
                                10 * 60 * 1000L -> TimeControl.TenMinutes
                                else -> TimeControl.NoLimit
                            }
                        } else TimeControl.NoLimit,
                        selectedColor = playerColor,
                        isAgainstBot = isPlayingAgainstBot
                    )
                )
                
                println("saveCurrentGameManually: создан объект SavedGame с ID = ${savedGame.id}")
                
                // Сохраняем игру в Room репозиторий
                val gameRoomRepository = GameRoomRepository(getApplication())
                val success = gameRoomRepository.saveGame(savedGame)
                println("saveCurrentGameManually: сохранение в Room репозиторий, результат = $success")
                
                // Также сохраняем в старый репозиторий для обратной совместимости
                val oldSuccess = gameRepository.saveGame(savedGame)
                println("saveCurrentGameManually: сохранение в старый репозиторий, результат = $oldSuccess")
                
                // Обновляем список сохраненных игр
                println("saveCurrentGameManually: обновляем список сохраненных игр")
                loadSavedGames()
                
                // Устанавливаем флаг, что игра сохранена
                _isGameSaved.value = true
                println("saveCurrentGameManually: установлен флаг isGameSaved = true")
                
                println("saveCurrentGameManually: игра сохранена вручную: ${savedGame.id}, результат: $result, успешно: $success")
            } catch (e: Exception) {
                println("Ошибка при сохранении игры: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Сбрасывает флаг сохранения игры
     */
    private fun resetGameSavedFlag() {
        _isGameSaved.value = false
    }

    /**
     * Начинает игру с пользовательской позиции
     * @param boardState карта позиций и фигур
     * @param playerColor цвет, за который играет пользователь
     * @param currentTurn текущий ход (белые или черные)
     */
    fun startGameFromCustomPosition(
        boardState: Map<Position, ChessPiece>,
        playerColor: PieceColor,
        currentTurn: PieceColor
    ) {
        viewModelScope.launch {
            println("startGameFromCustomPosition: начало метода")
            
            // Сохраняем цвет игрока
            this@ChessViewModel.playerColor = playerColor
            
            // Инициализируем доску в соответствии с выбранным цветом
            _board.value = if (playerColor == PieceColor.BLACK) {
                BlackChangeBoard()
            } else {
                WhiteChangeBoard()
            }
            
            // Очищаем доску перед установкой фигур
            _board.value.clearBoard()
            
            // Устанавливаем фигуры на доску
            boardState.forEach { (position, piece) ->
                _board.value.setPiece(position, piece.copy())
            }
            
            // Устанавливаем текущий ход
            _currentTurn.value = currentTurn
            
            // Обновляем внутреннее состояние доски
            updateBoardState()
            
            // Сбрасываем историю ходов
            _moveHistory.value = emptyList()
            _selectedPosition.value = null
            _possibleMoves.value = emptySet()
            _kingInCheck.value = null
            
            // Сбрасываем счетчик ходов
            _moveCount.value = 0
            _fiftyMoveRuleCounter = 0
            
            // Сбрасываем флаг сохранения игры
            resetGameSavedFlag()
            
            // Запускаем игру
            _gameState.value = GameState.Playing
            _isGameInProgress.value = true
            
            // Проверяем, не находится ли король под шахом в начальной позиции
            updateGameState()
            
            println("startGameFromCustomPosition: игра начата с пользовательской позиции")
        }
    }
} 