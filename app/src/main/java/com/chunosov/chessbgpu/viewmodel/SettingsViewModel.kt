package com.chunosov.chessbgpu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.chunosov.chessbgpu.model.AppSettings
import com.chunosov.chessbgpu.utils.ThemeManager
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel для управления настройками приложения
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appSettings = AppSettings(application.applicationContext)
    
    // Состояния настроек
    val soundEnabled: StateFlow<Boolean> = appSettings.soundEnabled
    val vibrationEnabled: StateFlow<Boolean> = appSettings.vibrationEnabled
    val hintsEnabled: StateFlow<Boolean> = appSettings.hintsEnabled
    val appTheme: StateFlow<ThemeManager.AppTheme> = appSettings.appTheme
    val boardTheme: StateFlow<ThemeManager.BoardTheme> = appSettings.boardTheme
    
    /**
     * Установить включение/выключение звука
     */
    fun setSoundEnabled(enabled: Boolean) {
        appSettings.setSoundEnabled(enabled)
    }
    
    /**
     * Установить включение/выключение вибрации
     */
    fun setVibrationEnabled(enabled: Boolean) {
        appSettings.setVibrationEnabled(enabled)
    }
    
    /**
     * Установить включение/выключение подсказок
     */
    fun setHintsEnabled(enabled: Boolean) {
        appSettings.setHintsEnabled(enabled)
    }
    
    /**
     * Установить тему приложения
     */
    fun setAppTheme(theme: ThemeManager.AppTheme) {
        appSettings.setAppTheme(theme)
    }
    
    /**
     * Установить тему шахматной доски
     */
    fun setBoardTheme(theme: ThemeManager.BoardTheme) {
        appSettings.setBoardTheme(theme)
    }
    
    /**
     * Получить название темы приложения для отображения
     */
    fun getAppThemeName(theme: ThemeManager.AppTheme): String {
        return appSettings.getAppThemeName(theme)
    }
    
    /**
     * Получить название темы доски для отображения
     */
    fun getBoardThemeName(theme: ThemeManager.BoardTheme): String {
        return appSettings.getBoardThemeName(theme)
    }
} 