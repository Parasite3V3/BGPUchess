package com.chunosov.chessbgpu.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.chunosov.chessbgpu.utils.ThemeManager

/**
 * Перечисление доступных тем оформления
 */
enum class AppTheme {
    STANDARD, DARK, LIGHT, WOODEN
}

/**
 * Класс для хранения и управления настройками приложения
 */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val themeManager = ThemeManager.getInstance(context)
    
    // Состояния для настроек
    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION_ENABLED, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _hintsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HINTS_ENABLED, true))
    val hintsEnabled: StateFlow<Boolean> = _hintsEnabled.asStateFlow()
    
    private val _appTheme = MutableStateFlow(themeManager.getAppTheme())
    val appTheme: StateFlow<ThemeManager.AppTheme> = _appTheme.asStateFlow()
    
    private val _boardTheme = MutableStateFlow(themeManager.getBoardTheme())
    val boardTheme: StateFlow<ThemeManager.BoardTheme> = _boardTheme.asStateFlow()
    
    /**
     * Установить включение/выключение звука
     */
    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        themeManager.setSoundEnabled(enabled)
    }
    
    /**
     * Установить включение/выключение вибрации
     */
    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
    
    /**
     * Установить включение/выключение подсказок
     */
    fun setHintsEnabled(enabled: Boolean) {
        _hintsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HINTS_ENABLED, enabled).apply()
    }
    
    /**
     * Установить тему приложения
     */
    fun setAppTheme(theme: ThemeManager.AppTheme) {
        _appTheme.value = theme
        themeManager.setAppTheme(theme)
    }
    
    /**
     * Установить тему шахматной доски
     */
    fun setBoardTheme(theme: ThemeManager.BoardTheme) {
        _boardTheme.value = theme
        themeManager.setBoardTheme(theme)
    }
    
    /**
     * Получить название темы приложения для отображения
     */
    fun getAppThemeName(theme: ThemeManager.AppTheme): String {
        return when (theme) {
            ThemeManager.AppTheme.CLASSIC -> "Классическая"
            ThemeManager.AppTheme.DARK -> "Темная"
            ThemeManager.AppTheme.EMERALD -> "Изумрудная"
            ThemeManager.AppTheme.LAVENDER -> "Лавандовая"
        }
    }
    
    /**
     * Получить название темы доски для отображения
     */
    fun getBoardThemeName(theme: ThemeManager.BoardTheme): String {
        return when (theme) {
            ThemeManager.BoardTheme.CLASSIC -> "Классическая"
            ThemeManager.BoardTheme.EMERALD -> "Изумрудная"
            ThemeManager.BoardTheme.WOOD -> "Деревянная"
            ThemeManager.BoardTheme.BLUE -> "Синяя"
            ThemeManager.BoardTheme.CLASSIC_WOOD -> "Классическая деревянная"
        }
    }
    
    companion object {
        private const val PREFS_NAME = "chess_app_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_HINTS_ENABLED = "hints_enabled"
    }
} 