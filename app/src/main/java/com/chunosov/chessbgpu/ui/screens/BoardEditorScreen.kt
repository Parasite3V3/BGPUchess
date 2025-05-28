package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chunosov.chessbgpu.model.*
import com.chunosov.chessbgpu.ui.components.ChessBoard
import com.chunosov.chessbgpu.viewmodel.ChessViewModel

/**
 * Класс для хранения состояния доски редактора
 */
class EditorBoardState {
    val board = ChessBoard()
    var boardVersion by mutableStateOf(0)
        private set
    
    fun setPiece(position: Position, piece: ChessPiece?) {
        board.setPiece(position, piece)
        // Увеличиваем версию доски для перерисовки
        boardVersion++
    }
    
    fun clearBoard() {
        board.clearBoard()
        // Увеличиваем версию доски для перерисовки
        boardVersion++
    }
    
    fun getBoardState(): Map<Position, ChessPiece> {
        return board.getBoardState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorScreen(
    viewModel: ChessViewModel,
    onStartGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Состояние для хранения текущей выбранной фигуры для размещения
    var selectedPiece by remember { mutableStateOf<ChessPiece?>(null) }
    
    // Состояние для хранения цвета, которым будет играть пользователь
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    
    // Состояние для хранения текущей доски
    val editorBoardState = remember { EditorBoardState() }
    
    // Состояние для хранения текущего хода
    var currentTurn by remember { mutableStateOf(PieceColor.WHITE) }
    
    // Состояние для отображения диалога подтверждения сброса доски
    var showResetConfirmation by remember { mutableStateOf(false) }
    
    // Состояние для отображения диалога подтверждения начала игры
    var showStartGameConfirmation by remember { mutableStateOf(false) }
    
    // Список доступных фигур для размещения
    val availablePieces = remember {
        listOf(
            King(PieceColor.WHITE),
            Queen(PieceColor.WHITE),
            Rook(PieceColor.WHITE),
            Bishop(PieceColor.WHITE),
            Knight(PieceColor.WHITE),
            Pawn(PieceColor.WHITE),
            King(PieceColor.BLACK),
            Queen(PieceColor.BLACK),
            Rook(PieceColor.BLACK),
            Bishop(PieceColor.BLACK),
            Knight(PieceColor.BLACK),
            Pawn(PieceColor.BLACK)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор доски") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка для сброса доски
                    IconButton(onClick = { showResetConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить доску")
                    }
                    
                    // Кнопка для начала игры с текущей позиции
                    IconButton(onClick = { showStartGameConfirmation = true }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Начать игру")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Шахматная доска
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                // Используем boardVersion для отслеживания изменений доски
                val boardVersion = editorBoardState.boardVersion
                
                ChessBoard(
                    board = editorBoardState.board,
                    selectedPosition = null,
                    possibleMoves = emptySet(),
                    onSquareClick = { position ->
                        // Если выбрана фигура для размещения, ставим её на доску
                        if (selectedPiece != null) {
                            // Создаем копию выбранной фигуры и размещаем на доске
                            val pieceCopy = selectedPiece!!.copy()
                            editorBoardState.setPiece(position, pieceCopy)
                            println("Размещена фигура ${pieceCopy.type} цвета ${pieceCopy.color} на позиции ${position.row},${position.col}")
                        } else {
                            // Если фигура не выбрана, удаляем фигуру с доски
                            println("Удалена фигура с позиции ${position.row},${position.col}")
                            editorBoardState.setPiece(position, null)
                        }
                    },
                    kingInCheck = null,
                    // Используем более контрастные цвета для шахматной доски
                    lightSquareColor = Color(0xFFD9D9D9), // Светло-серый для белых клеток
                    darkSquareColor = Color(0xFF769656)  // Темно-зеленый для черных клеток
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Выбор цвета хода
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ход:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                
                // Радиокнопка для выбора хода белых
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { currentTurn = PieceColor.WHITE }
                ) {
                    RadioButton(
                        selected = currentTurn == PieceColor.WHITE,
                        onClick = { currentTurn = PieceColor.WHITE }
                    )
                    Text("Белые")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Радиокнопка для выбора хода черных
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { currentTurn = PieceColor.BLACK }
                ) {
                    RadioButton(
                        selected = currentTurn == PieceColor.BLACK,
                        onClick = { currentTurn = PieceColor.BLACK }
                    )
                    Text("Черные")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Выбор цвета игрока
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Играть за:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                
                // Радиокнопка для выбора игры за белых
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedColor = PieceColor.WHITE }
                ) {
                    RadioButton(
                        selected = selectedColor == PieceColor.WHITE,
                        onClick = { selectedColor = PieceColor.WHITE }
                    )
                    Text("Белые")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Радиокнопка для выбора игры за черных
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedColor = PieceColor.BLACK }
                ) {
                    RadioButton(
                        selected = selectedColor == PieceColor.BLACK,
                        onClick = { selectedColor = PieceColor.BLACK }
                    )
                    Text("Черные")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Палитра фигур для размещения
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Выберите фигуру для размещения:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Добавляем "ластик" для удаления фигур
                    item {
                        PieceSelectionItem(
                            piece = null,
                            isSelected = selectedPiece == null,
                            onSelect = { selectedPiece = null }
                        )
                    }
                    
                    // Добавляем все доступные фигуры
                    items(availablePieces) { piece ->
                        PieceSelectionItem(
                            piece = piece,
                            isSelected = selectedPiece?.type == piece.type && selectedPiece?.color == piece.color,
                            onSelect = { selectedPiece = piece.copy() }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопка для начала игры
            Button(
                onClick = { showStartGameConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Начать игру с этой позиции")
            }
        }
    }
    
    // Диалог подтверждения сброса доски
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Очистить доску") },
            text = { Text("Вы уверены, что хотите очистить доску?") },
            confirmButton = {
                Button(
                    onClick = {
                        editorBoardState.clearBoard()
                        showResetConfirmation = false
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог подтверждения начала игры
    if (showStartGameConfirmation) {
        AlertDialog(
            onDismissRequest = { showStartGameConfirmation = false },
            title = { Text("Начать игру") },
            text = { Text("Вы уверены, что хотите начать игру с этой позиции?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Передаем текущую позицию в ViewModel
                        viewModel.startGameFromCustomPosition(
                            boardState = editorBoardState.getBoardState(),
                            playerColor = selectedColor,
                            currentTurn = currentTurn
                        )
                        showStartGameConfirmation = false
                        onStartGame()
                    }
                ) {
                    Text("Начать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartGameConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PieceSelectionItem(
    piece: ChessPiece?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (piece == null) {
            // Ластик
            Icon(
                Icons.Default.Clear,
                contentDescription = "Удалить фигуру",
                tint = MaterialTheme.colorScheme.error
            )
        } else {
            // Фигура
            Text(
                text = when (piece.type) {
                    PieceType.KING -> "♚"
                    PieceType.QUEEN -> "♛"
                    PieceType.ROOK -> "♜"
                    PieceType.BISHOP -> "♝"
                    PieceType.KNIGHT -> "♞"
                    PieceType.PAWN -> "♟"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black
            )
        }
    }
} 