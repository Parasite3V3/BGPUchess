package com.chunosov.chessbgpu.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.chunosov.chessbgpu.navigation.Screen

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.MainMenu.route,
            onClick = { 
                if (currentRoute != Screen.MainMenu.route) {
                    navController.navigate(Screen.MainMenu.route)
                }
            },
            icon = { Text("🏠") },
            label = { Text("Меню") }
        )

        NavigationBarItem(
            selected = currentRoute == Screen.GameOptions.route,
            onClick = { 
                if (currentRoute != Screen.GameOptions.route) {
                    navController.navigate(Screen.GameOptions.route)
                }
            },
            icon = { Text("⚔") },
            label = { Text("Играть") }
        )
        
        NavigationBarItem(
            selected = currentRoute == Screen.Analysis.route,
            onClick = { 
                if (currentRoute != Screen.Analysis.route) {
                    navController.navigate(Screen.Analysis.route)
                }
            },
            icon = { Text("🔍") },
            label = { Text("Анализ") }
        )
        
        NavigationBarItem(
            selected = currentRoute == Screen.Tasks.route,
            onClick = { 
                if (currentRoute != Screen.Tasks.route) {
                    navController.navigate(Screen.Tasks.route)
                }
            },
            icon = { Text("♟") },
            label = { Text("Задачи") }
        )
        
        NavigationBarItem(
            selected = currentRoute == Screen.Learning.route,
            onClick = { 
                if (currentRoute != Screen.Learning.route) {
                    navController.navigate(Screen.Learning.route)
                }
            },
            icon = { Text("📚") },
            label = { Text("Обучение") }
        )

        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { 
                if (currentRoute != Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route)
                }
            },
            icon = { Text("⚙") },
            label = { Text("Настройки") }
        )
    }
} 