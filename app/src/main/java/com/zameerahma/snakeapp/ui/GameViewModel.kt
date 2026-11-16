package com.zameerahma.snakeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zameerahma.snakeapp.SnakeApplication
import com.zameerahma.snakeapp.audio.Haptics
import com.zameerahma.snakeapp.audio.Sfx
import com.zameerahma.snakeapp.audio.SoundPlayer
import com.zameerahma.snakeapp.data.Settings
import com.zameerahma.snakeapp.data.SettingsRepository
import com.zameerahma.snakeapp.game.Direction
import com.zameerahma.snakeapp.game.GameEngine
import com.zameerahma.snakeapp.game.GameEvent
import com.zameerahma.snakeapp.game.GameState
import com.zameerahma.snakeapp.game.GameStatus
import com.zameerahma.snakeapp.game.StepResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs the game loop: every tick it asks the [GameEngine] to move the snake,
 * then plays sounds / vibrations and saves high scores.
 * It survives screen rotation, so the game continues where it was.
 */
class GameViewModel(
    private val engine: GameEngine,
    private val repository: SettingsRepository,
    private val sound: SoundPlayer,
    private val haptics: Haptics,
) : ViewModel() {

    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game.asStateFlow()

    private val _newRecord = MutableStateFlow(false)
    val newRecord: StateFlow<Boolean> = _newRecord.asStateFlow()

    private val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private var loopJob: Job? = null

    /** Creates a fresh game using the current settings. */
    fun newGame() {
        loopJob?.cancel()
        _newRecord.value = false
        viewModelScope.launch {
            val current = repository.settings.first()
            _game.value = engine.newGame(current.toGameConfig())
        }
    }

    fun onDirection(direction: Direction) {
        val state = _game.value ?: return
        when (state.status) {
            GameStatus.READY -> {
                _game.value = engine.queueDirection(state, direction)
                start()
            }
            GameStatus.RUNNING -> _game.value = engine.queueDirection(state, direction)
            else -> Unit
        }
    }

    fun start() {
        val state = _game.value ?: return
        if (state.status != GameStatus.READY) return
        _game.value = engine.start(state)
        playSound(Sfx.START)
        runLoop()
    }

    fun pause() {
        val state = _game.value ?: return
        if (state.status == GameStatus.RUNNING) {
            loopJob?.cancel()
            _game.value = engine.pause(state)
        }
    }

    fun resume() {
        val state = _game.value ?: return
        if (state.status == GameStatus.PAUSED) {
            _game.value = engine.resume(state)
            runLoop()
        }
    }

    /** Pause button / space key: pauses a running game, resumes a paused one, starts a ready one. */
    fun togglePause() {
        when (_game.value?.status) {
            GameStatus.RUNNING -> pause()
            GameStatus.PAUSED -> resume()
            GameStatus.READY -> start()
            else -> Unit
        }
    }

    fun exitGame() {
        loopJob?.cancel()
        _game.value = null
    }

    private fun runLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                val before = _game.value ?: break
                if (before.status != GameStatus.RUNNING) break
                delay(before.tickMs)
                val current = _game.value ?: break
                if (current.status != GameStatus.RUNNING) break
                val result = engine.step(current)
                _game.value = result.state
                handleEvents(result)
            }
        }
    }

    private fun handleEvents(result: StepResult) {
        val events = result.events
        when {
            GameEvent.CRASHED in events -> {
                playSound(Sfx.GAME_OVER)
                vibrate { haptics.crash() }
            }
            GameEvent.WON in events -> playSound(Sfx.WIN)
            GameEvent.ATE_BONUS in events -> {
                playSound(Sfx.BONUS)
                vibrate { haptics.bonus() }
            }
            GameEvent.BONUS_APPEARED in events -> {
                playSound(Sfx.BONUS_APPEAR)
                vibrate { haptics.eat() }
            }
            GameEvent.ATE_FOOD in events -> {
                playSound(Sfx.EAT)
                vibrate { haptics.eat() }
            }
        }
        val state = result.state
        if (state.status == GameStatus.GAME_OVER) {
            viewModelScope.launch {
                _newRecord.value = repository.submitScore(state.config.difficulty, state.score)
            }
        }
    }

    private fun playSound(sfx: Sfx) {
        if (settings.value.soundEnabled) sound.play(sfx)
    }

    private inline fun vibrate(block: () -> Unit) {
        if (settings.value.vibrationEnabled) block()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SnakeApplication
                GameViewModel(GameEngine(), app.settingsRepository, app.soundPlayer, app.haptics)
            }
        }
    }
}
