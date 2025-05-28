package com.chunosov.chessbgpu.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

/**
 * Класс для управления темами приложения
 */
class ThemeManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Темы приложения
     */
    enum class AppTheme {
        CLASSIC,        // Классическая тема (светлая с синими элементами)
        DARK,           // Темная тема
        EMERALD,        // Изумрудная тема
        LAVENDER        // Лавандовая тема
    }
    
    /**
     * Темы шахматной доски
     */
    enum class BoardTheme {
        CLASSIC,        // Классическая доска (черно-белая)
        EMERALD,        // Изумрудная доска (белые и изумрудные клетки)
        WOOD,           // Деревянная доска (светло- и темно-коричневые клетки)
        BLUE,           // Синяя доска (белые и синие клетки)
        CLASSIC_WOOD    // Классическая доска с синим и деревянным цветами
    }
    
    /**
     * Получение текущей темы приложения
     * @return текущая тема приложения
     */
    fun getAppTheme(): AppTheme {
        val themeString = sharedPreferences.getString(APP_THEME_KEY, AppTheme.CLASSIC.name)
        return AppTheme.valueOf(themeString ?: AppTheme.CLASSIC.name)
    }
    
    /**
     * Установка темы приложения
     * @param theme тема приложения
     */
    fun setAppTheme(theme: AppTheme) {
        sharedPreferences.edit().putString(APP_THEME_KEY, theme.name).apply()
    }
    
    /**
     * Получение текущей темы шахматной доски
     * @return текущая тема шахматной доски
     */
    fun getBoardTheme(): BoardTheme {
        val themeString = sharedPreferences.getString(BOARD_THEME_KEY, BoardTheme.CLASSIC.name)
        return BoardTheme.valueOf(themeString ?: BoardTheme.CLASSIC.name)
    }
    
    /**
     * Установка темы шахматной доски
     * @param theme тема шахматной доски
     */
    fun setBoardTheme(theme: BoardTheme) {
        sharedPreferences.edit().putString(BOARD_THEME_KEY, theme.name).apply()
    }
    
    /**
     * Получение цветов клеток доски для текущей темы
     * @return пара цветов: светлый и темный
     */
    fun getBoardColors(): Pair<Color, Color> {
        return when (getBoardTheme()) {
            BoardTheme.CLASSIC -> Pair(Color(0xFFF0D9B5), Color(0xFFB58863)) // Стандартные шахматные цвета (светло-коричневый и темно-коричневый)
            BoardTheme.EMERALD -> Pair(Color(0xFFE8F5E9), Color(0xFF2E8B57)) // Светло-зеленый и изумрудный
            BoardTheme.WOOD -> Pair(Color(0xFFD2B48C), Color(0xFF8B4513)) // Светло-коричневый и темно-коричневый
            BoardTheme.BLUE -> Pair(Color(0xFFE3F2FD), Color(0xFF4682B4)) // Светло-голубой и синий
            BoardTheme.CLASSIC_WOOD -> Pair(Color(0xFFD2B48C), Color(0xFF4682B4)) // Деревянный и синий
        }
    }
    
    /**
     * Получение состояния звука
     * @return true - звук включен, false - выключен
     */
    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(SOUND_ENABLED_KEY, true)
    }
    
    /**
     * Установка состояния звука
     * @param enabled true - включить звук, false - выключить
     */
    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SOUND_ENABLED_KEY, enabled).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val APP_THEME_KEY = "app_theme"
        private const val BOARD_THEME_KEY = "board_theme"
        private const val SOUND_ENABLED_KEY = "sound_enabled"
        
        private var instance: ThemeManager? = null
        
        /**
         * Получение экземпляра ThemeManager
         * @param context контекст приложения
         * @return экземпляр ThemeManager
         */
        fun getInstance(context: Context): ThemeManager {
            if (instance == null) {
                instance = ThemeManager(context.applicationContext)
            }
            return instance!!
        }
    }
} 