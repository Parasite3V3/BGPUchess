package com.chunosov.chessbgpu.model

/**
 * Класс для хранения информации о ходе в шахматной партии
 */
data class Move(
    val from: Position,                // Начальная позиция
    val to: Position,                  // Конечная позиция
    val piece: ChessPiece,             // Перемещаемая фигура
    val capturedPiece: ChessPiece? = null, // Взятая фигура (если есть)
    val previousBoard: ChessBoard? = null, // Состояние доски до хода (для отмены)
    val previousTurn: PieceColor? = null,  // Ход игрока до хода (для отмены)
    val isCheck: Boolean = false,      // Ход приводит к шаху
    val isCheckmate: Boolean = false,  // Ход приводит к мату
    val isStalemate: Boolean = false,  // Ход приводит к пату
    val isCastling: Boolean = false,   // Ход - рокировка
    val isEnPassant: Boolean = false,  // Ход - взятие на проходе
    val isPromotion: Boolean = false,  // Ход - превращение пешки
    val promotedTo: PieceType? = null  // Тип фигуры после превращения
) {
    /**
     * Преобразует ход в алгебраическую нотацию шахмат
     * @return строка с нотацией хода
     */
    fun toAlgebraicNotation(): String {
        val builder = StringBuilder()
        
        // Добавляем символ фигуры (кроме пешек)
        if (piece.type != PieceType.PAWN) {
            builder.append(
                when (piece.type) {
                    PieceType.KING -> "K"
                    PieceType.QUEEN -> "Q"
                    PieceType.ROOK -> "R"
                    PieceType.BISHOP -> "B"
                    PieceType.KNIGHT -> "N"
                    else -> ""
                }
            )
        }
        
        // Особый случай - рокировка
        if (isCastling) {
            return if (to.col > from.col) "O-O" else "O-O-O"
        }
        
        // Для пешек при взятии указываем вертикаль
        if (piece.type == PieceType.PAWN && capturedPiece != null) {
            builder.append(('a' + from.col).toString())
        }
        
        // Указываем взятие
        if (capturedPiece != null || isEnPassant) {
            builder.append("x")
        }
        
        // Конечная позиция
        builder.append(to.toAlgebraicNotation())
        
        // Превращение пешки
        if (isPromotion && promotedTo != null) {
            builder.append("=")
            builder.append(
                when (promotedTo) {
                    PieceType.QUEEN -> "Q"
                    PieceType.ROOK -> "R"
                    PieceType.BISHOP -> "B"
                    PieceType.KNIGHT -> "N"
                    else -> ""
                }
            )
        }
        
        // Шах и мат
        if (isCheckmate) {
            builder.append("#")
        } else if (isCheck) {
            builder.append("+")
        }
        
        return builder.toString()
    }
    
    /**
     * Сериализует ход в формат для хранения
     * @return карта с данными хода
     */
    fun serialize(): Map<String, Any> {
        val result = mutableMapOf<String, Any>(
            "from" to mapOf("row" to from.row, "col" to from.col),
            "to" to mapOf("row" to to.row, "col" to to.col),
            "piece" to mapOf(
                "type" to piece.type.name,
                "color" to piece.color.name,
                "hasMoved" to piece.hasMoved
            ),
            "isCheck" to isCheck,
            "isCheckmate" to isCheckmate,
            "isStalemate" to isStalemate,
            "isCastling" to isCastling,
            "isEnPassant" to isEnPassant,
            "isPromotion" to isPromotion
        )
        
        capturedPiece?.let {
            result["capturedPiece"] = mapOf(
                "type" to it.type.name,
                "color" to it.color.name,
                "hasMoved" to it.hasMoved
            )
        }
        
        promotedTo?.let {
            result["promotedTo"] = it.name
        }
        
        return result
    }
    
    companion object {
        fun deserialize(data: Map<String, Any>): Move {
            // Получаем данные для позиций
            val fromData = data["from"] as? Map<String, Any> ?: mapOf<String, Any>()
            val toData = data["to"] as? Map<String, Any> ?: mapOf<String, Any>()
            
            // Создаем позиции
            val from = Position(
                row = (fromData["row"] as? Number)?.toInt() ?: 0,
                col = (fromData["col"] as? Number)?.toInt() ?: 0
            )
            
            val to = Position(
                row = (toData["row"] as? Number)?.toInt() ?: 0,
                col = (toData["col"] as? Number)?.toInt() ?: 0
            )
            
            // Получаем данные о фигуре
            val pieceData = data["piece"] as? Map<String, Any> ?: mapOf<String, Any>()
            val piece = createPieceFromData(pieceData)
            
            // Получаем данные о взятой фигуре (если есть)
            val capturedPieceData = data["capturedPiece"] as? Map<String, Any>
            val capturedPiece = if (capturedPieceData != null) {
                createPieceFromData(capturedPieceData)
            } else {
                null
            }
            
            // Получаем данные о превращении пешки
            val isPromotion = data["isPromotion"] as? Boolean ?: false
            val promotedToName = data["promotedTo"] as? String
            val promotedTo = if (promotedToName != null) {
                try {
                    PieceType.valueOf(promotedToName)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            return Move(
                from = from,
                to = to,
                piece = piece,
                capturedPiece = capturedPiece,
                isCheck = data["isCheck"] as? Boolean ?: false,
                isCheckmate = data["isCheckmate"] as? Boolean ?: false,
                isStalemate = data["isStalemate"] as? Boolean ?: false,
                isCastling = data["isCastling"] as? Boolean ?: false,
                isEnPassant = data["isEnPassant"] as? Boolean ?: false,
                isPromotion = isPromotion,
                promotedTo = promotedTo
            )
        }
        
        private fun createPieceFromData(data: Map<String, Any>): ChessPiece {
            val typeName = data["type"] as? String ?: PieceType.PAWN.name
            val colorName = data["color"] as? String ?: PieceColor.WHITE.name
            val hasMoved = data["hasMoved"] as? Boolean ?: false
            
            val pieceType = try {
                PieceType.valueOf(typeName)
            } catch (e: Exception) {
                PieceType.PAWN
            }
            
            val pieceColor = try {
                PieceColor.valueOf(colorName)
            } catch (e: Exception) {
                PieceColor.WHITE
            }
            
            val piece = when (pieceType) {
                PieceType.PAWN -> Pawn(pieceColor)
                PieceType.KNIGHT -> Knight(pieceColor)
                PieceType.BISHOP -> Bishop(pieceColor)
                PieceType.ROOK -> Rook(pieceColor)
                PieceType.QUEEN -> Queen(pieceColor)
                PieceType.KING -> King(pieceColor)
            }
            
            piece.hasMoved = hasMoved
            return piece
        }
    }
} 