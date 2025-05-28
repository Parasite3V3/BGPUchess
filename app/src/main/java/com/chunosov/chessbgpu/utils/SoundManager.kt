package com.chunosov.chessbgpu.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.chunosov.chessbgpu.R

/**
 * Класс для управления звуками в приложении
 */
class SoundManager(private val context: Context) {
    
    private var isEnabled = true
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * Типы звуков в приложении
     */
    enum class SoundType {
        MOVE,           // Перемещение фигуры
        CAPTURE,        // Взятие фигуры
        CHECK,          // Шах
        CHECKMATE,      // Мат
        DRAW,           // Ничья
        GAME_START,     // Начало игры
        GAME_END,       // Конец игры
        PROMOTION,      // Превращение пешки
        CASTLING        // Рокировка
    }
    
    /**
     * Включение/выключение звуков
     * @param enabled true - звуки включены, false - выключены
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Возвращает состояние звуков (включены/выключены)
     * @return true - звуки включены, false - выключены
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Воспроизведение звука
     * @param soundType тип звука
     */
    fun playSound(soundType: SoundType) {
        if (!isEnabled) return
        
        // Останавливаем предыдущий звук, если он воспроизводится
        stopSound()
        
        val soundResourceId = getSoundResourceId(soundType)
        mediaPlayer = MediaPlayer.create(context, soundResourceId)
        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
        }
        mediaPlayer?.start()
        
        // Автоматически освобождаем ресурсы через 5 секунд, если звук не завершился
        handler.postDelayed({
            stopSound()
        }, 5000)
    }
    
    /**
     * Остановка текущего звука
     */
    fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Получение идентификатора ресурса звука по типу
     * @param soundType тип звука
     * @return идентификатор ресурса
     */
    private fun getSoundResourceId(soundType: SoundType): Int {
        return when (soundType) {
            SoundType.MOVE -> R.raw.move
            SoundType.CAPTURE -> R.raw.capture
            SoundType.CHECK -> R.raw.check
            SoundType.CHECKMATE -> R.raw.checkmate
            SoundType.DRAW -> R.raw.draw
            SoundType.GAME_START -> R.raw.game_start
            SoundType.GAME_END -> R.raw.game_end
            SoundType.PROMOTION -> R.raw.promotion
            SoundType.CASTLING -> R.raw.castling
        }
    }
    
    companion object {
        private var instance: SoundManager? = null
        
        /**
         * Получение экземпляра SoundManager
         * @param context контекст приложения
         * @return экземпляр SoundManager
         */
        fun getInstance(context: Context): SoundManager {
            if (instance == null) {
                instance = SoundManager(context.applicationContext)
            }
            return instance!!
        }
    }
} 