package com.chunosov.chessbgpu.model

data class GameOptions(
    val timeControl: TimeControl = TimeControl.NoLimit,
    val selectedColor: PieceColor = PieceColor.WHITE,
    val isAgainstBot: Boolean = false
) {
    fun serialize(): Map<String, Any> {
        return mapOf(
            "timeControl" to timeControl.name,
            "selectedColor" to selectedColor.name,
            "isAgainstBot" to isAgainstBot
        )
    }
    
    companion object {
        fun deserialize(data: Map<String, Any>): GameOptions {
            return GameOptions(
                timeControl = try {
                    TimeControl.valueOf(data["timeControl"] as String)
                } catch (e: Exception) {
                    TimeControl.NoLimit
                },
                selectedColor = try {
                    PieceColor.valueOf(data["selectedColor"] as String)
                } catch (e: Exception) {
                    PieceColor.WHITE
                },
                isAgainstBot = data["isAgainstBot"] as? Boolean ?: false
            )
        }
    }
} 