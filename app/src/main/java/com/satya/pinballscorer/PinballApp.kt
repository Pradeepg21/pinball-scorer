package com.satya.pinballscorer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.satya.pinballscorer.ui.screens.GameScreen
import com.satya.pinballscorer.ui.screens.HighScoresScreen
import com.satya.pinballscorer.ui.screens.LevelSelectionScreen
import com.satya.pinballscorer.ui.screens.MainMenuScreen

enum class Screen {
    MainMenu,
    LevelSelection,
    Game,
    HighScores
}

@Composable
fun PinballApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.name
    ) {
        composable(Screen.MainMenu.name) {
            MainMenuScreen(
                onStartGameClicked = { navController.navigate(Screen.LevelSelection.name) },
                onHighScoresClicked = { navController.navigate(Screen.HighScores.name) }
            )
        }
        composable(Screen.LevelSelection.name) {
            LevelSelectionScreen(
                onLevelSelected = { level ->
                    navController.navigate("${Screen.Game.name}/$level")
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable("${Screen.Game.name}/{level}") { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level") ?: "easy"
            GameScreen(
                level = level,
                onGameOver = {
                    navController.popBackStack(Screen.MainMenu.name, inclusive = false)
                }
            )
        }
        composable(Screen.HighScores.name) {
            HighScoresScreen(
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
