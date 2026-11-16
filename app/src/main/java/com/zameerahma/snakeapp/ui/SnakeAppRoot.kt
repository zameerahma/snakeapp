package com.zameerahma.snakeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zameerahma.snakeapp.data.ThemeMode
import com.zameerahma.snakeapp.ui.game.GameScreen
import com.zameerahma.snakeapp.ui.menu.MenuScreen
import com.zameerahma.snakeapp.ui.scores.HighScoresScreen
import com.zameerahma.snakeapp.ui.settings.SettingsScreen
import com.zameerahma.snakeapp.ui.theme.SnakeTheme

private enum class Screen { MENU, GAME, SETTINGS, HIGH_SCORES }

@Composable
fun SnakeAppRoot(
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    gameViewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val highScores by settingsViewModel.highScores.collectAsStateWithLifecycle()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SnakeTheme(darkTheme = darkTheme) {
        var screen by rememberSaveable { mutableStateOf(Screen.MENU) }
        val toMenu = { screen = Screen.MENU }

        if (screen == Screen.SETTINGS || screen == Screen.HIGH_SCORES) BackHandler(onBack = toMenu)

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                Screen.MENU -> MenuScreen(
                    settings = settings,
                    highScores = highScores,
                    onSelectDifficulty = settingsViewModel::setDifficulty,
                    onPlay = {
                        gameViewModel.newGame()
                        screen = Screen.GAME
                    },
                    onHighScores = { screen = Screen.HIGH_SCORES },
                    onSettings = { screen = Screen.SETTINGS },
                )
                Screen.GAME -> GameScreen(
                    viewModel = gameViewModel,
                    settings = settings,
                    highScores = highScores,
                    onExit = {
                        gameViewModel.exitGame()
                        screen = Screen.MENU
                    },
                )
                Screen.SETTINGS -> SettingsScreen(settings, settingsViewModel, onBack = toMenu)
                Screen.HIGH_SCORES -> HighScoresScreen(
                    highScores = highScores,
                    onReset = settingsViewModel::resetHighScores,
                    onBack = toMenu,
                )
            }
        }
    }
}
