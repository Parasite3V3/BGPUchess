package com.chunosov.chessbgpu.model

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

abstract class ChessPiece(val type: PieceType, val color: PieceColor) {
    var hasMoved: Boolean = false
    
    /**
     * Создает копию фигуры
     */
    abstract fun copy(): ChessPiece
    
    fun toUnicode(): String {
        return when (type) {
            PieceType.KING -> if (color == PieceColor.WHITE) "♔" else "♚"
            PieceType.QUEEN -> if (color == PieceColor.WHITE) "♕" else "♛"
            PieceType.ROOK -> if (color == PieceColor.WHITE) "♖" else "♜"
            PieceType.BISHOP -> if (color == PieceColor.WHITE) "♗" else "♝"
            PieceType.KNIGHT -> if (color == PieceColor.WHITE) "♘" else "♞"
            PieceType.PAWN -> if (color == PieceColor.WHITE) "♙" else "♟"
        }
    }

    fun getDisplayName(): String {
        return when (type) {
            PieceType.PAWN -> "Пешка"
            PieceType.KNIGHT -> "Конь"
            PieceType.BISHOP -> "Слон"
            PieceType.ROOK -> "Ладья"
            PieceType.QUEEN -> "Ферзь"
            PieceType.KING -> "Король"
        }
    }
    
    fun getUnicodeSymbol(): String {
        return when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟︎"
            }
        }
    }
}

class King(color: PieceColor) : ChessPiece(PieceType.KING, color) {
    override fun copy(): ChessPiece = King(color).also { it.hasMoved = hasMoved }
}

class Queen(color: PieceColor) : ChessPiece(PieceType.QUEEN, color) {
    override fun copy(): ChessPiece = Queen(color).also { it.hasMoved = hasMoved }
}

class Rook(color: PieceColor) : ChessPiece(PieceType.ROOK, color) {
    override fun copy(): ChessPiece = Rook(color).also { it.hasMoved = hasMoved }
}

class Bishop(color: PieceColor) : ChessPiece(PieceType.BISHOP, color) {
    override fun copy(): ChessPiece = Bishop(color).also { it.hasMoved = hasMoved }
}

class Knight(color: PieceColor) : ChessPiece(PieceType.KNIGHT, color) {
    override fun copy(): ChessPiece = Knight(color).also { it.hasMoved = hasMoved }
}

class Pawn(color: PieceColor) : ChessPiece(PieceType.PAWN, color) {
    override fun copy(): ChessPiece = Pawn(color).also { it.hasMoved = hasMoved }
}

/**
 * Возвращает Unicode символ для шахматной фигуры
 */
fun ChessPiece.toUnicode(): String {
    return when (type) {
        PieceType.PAWN -> if (color == PieceColor.WHITE) "♙" else "♟"
        PieceType.ROOK -> if (color == PieceColor.WHITE) "♖" else "♜"
        PieceType.KNIGHT -> if (color == PieceColor.WHITE) "♘" else "♞"
        PieceType.BISHOP -> if (color == PieceColor.WHITE) "♗" else "♝"
        PieceType.QUEEN -> if (color == PieceColor.WHITE) "♕" else "♛"
        PieceType.KING -> if (color == PieceColor.WHITE) "♔" else "♚"
    }
} 