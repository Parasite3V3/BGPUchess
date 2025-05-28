package com.chunosov.chessbgpu.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import com.chunosov.chessbgpu.ui.theme.ChessBGPUTheme
import com.chunosov.chessbgpu.viewmodel.ChessViewModel

class ChessActivity : ComponentActivity() {
    private lateinit var viewModel: ChessViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ChessViewModel::class.java]
        viewModel.initializeContext(applicationContext)
        
        // Обработка нажатия кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Вызовем метод во ViewModel, который покажет диалог подтверждения
                viewModel.onGameExit()
            }
        })
        
        setContent {
            ChessBGPUTheme {
                val showExitDialog by viewModel.showExitDialog.collectAsState()
                
                // Отображаем основной экран игры
                ChessScreen(viewModel = viewModel)
                
                // Отображаем диалог подтверждения выхода, если нужно
                if (showExitDialog) {
                    ExitGameDialog(
                        onDismiss = { viewModel.cancelExit() },
                        onConfirm = {
                            viewModel.confirmExit()
                            finish() // Завершаем активити после сохранения
                        }
                    )
                }
            }
        }
    }
} 