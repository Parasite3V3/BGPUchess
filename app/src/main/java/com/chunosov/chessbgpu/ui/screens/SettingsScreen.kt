package com.chunosov.chessbgpu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chunosov.chessbgpu.utils.ThemeManager
import com.chunosov.chessbgpu.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    // Получаем состояние настроек из ViewModel
    val soundEnabled by settingsViewModel.soundEnabled.collectAsState()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val hintsEnabled by settingsViewModel.hintsEnabled.collectAsState()
    val appTheme by settingsViewModel.appTheme.collectAsState()
    val boardTheme by settingsViewModel.boardTheme.collectAsState()
    
    // Список доступных тем приложения
    val appThemes = listOf(
        ThemeManager.AppTheme.CLASSIC to "Классическая",
        ThemeManager.AppTheme.DARK to "Темная",
        ThemeManager.AppTheme.EMERALD to "Изумрудная",
        ThemeManager.AppTheme.LAVENDER to "Лавандовая"
    )
    
    // Список доступных тем доски
    val boardThemes = listOf(
        ThemeManager.BoardTheme.CLASSIC to "Классическая",
        ThemeManager.BoardTheme.EMERALD to "Изумрудная",
        ThemeManager.BoardTheme.WOOD to "Деревянная",
        ThemeManager.BoardTheme.BLUE to "Синяя",
        ThemeManager.BoardTheme.CLASSIC_WOOD to "Классическая деревянная"
    )
    
    // Флаг для отслеживания изменений настроек
    var settingsChanged by remember { mutableStateOf(false) }
    
    // Состояние прокрутки
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Общие настройки",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Настройка звука
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Звук",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { 
                        settingsViewModel.setSoundEnabled(it)
                        settingsChanged = true
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Настройка вибрации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Вибрация",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { 
                        settingsViewModel.setVibrationEnabled(it)
                        settingsChanged = true
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Настройка подсказок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Подсказки ходов",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = hintsEnabled,
                    onCheckedChange = { 
                        settingsViewModel.setHintsEnabled(it)
                        settingsChanged = true
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Выбор темы приложения
            Text(
                text = "Тема приложения",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(vertical = 8.dp)
            ) {
                appThemes.forEach { (themeEnum, themeName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (themeEnum == appTheme),
                                onClick = { 
                                    settingsViewModel.setAppTheme(themeEnum)
                                    settingsChanged = true
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeEnum == appTheme),
                            onClick = null // null because we're handling the click on the row
                        )
                        Text(
                            text = themeName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Выбор темы шахматной доски
            Text(
                text = "Тема шахматной доски",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(vertical = 8.dp)
            ) {
                boardThemes.forEach { (themeEnum, themeName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (themeEnum == boardTheme),
                                onClick = { 
                                    settingsViewModel.setBoardTheme(themeEnum)
                                    settingsChanged = true
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeEnum == boardTheme),
                            onClick = null // null because we're handling the click on the row
                        )
                        Text(
                            text = themeName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопка сохранения настроек
            Button(
                onClick = { 
                    // Настройки сохраняются автоматически при изменении
                    settingsChanged = false
                    // Показать сообщение об успешном сохранении
                    // (в реальном приложении можно добавить Snackbar)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp),
                enabled = settingsChanged
            ) {
                Text("Сохранить настройки")
            }
        }
    }
}
