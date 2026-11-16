package com.zameerahma.snakeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zameerahma.snakeapp.SnakeApplication
import com.zameerahma.snakeapp.data.Settings
import com.zameerahma.snakeapp.data.SettingsRepository
import com.zameerahma.snakeapp.data.ThemeMode
import com.zameerahma.snakeapp.game.Difficulty
import com.zameerahma.snakeapp.game.WallMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the menu, settings and high score screens. */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val highScores: StateFlow<Map<Difficulty, Int>> =
        repository.highScores.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setDifficulty(value: Difficulty) = save { repository.setDifficulty(value) }
    fun setWallMode(value: WallMode) = save { repository.setWallMode(value) }
    fun setObstacles(value: Boolean) = save { repository.setObstacles(value) }
    fun setSound(value: Boolean) = save { repository.setSound(value) }
    fun setVibration(value: Boolean) = save { repository.setVibration(value) }
    fun setShowControls(value: Boolean) = save { repository.setShowControls(value) }
    fun setThemeMode(value: ThemeMode) = save { repository.setThemeMode(value) }
    fun resetHighScores() = save { repository.resetHighScores() }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SnakeApplication
                SettingsViewModel(app.settingsRepository)
            }
        }
    }
}
