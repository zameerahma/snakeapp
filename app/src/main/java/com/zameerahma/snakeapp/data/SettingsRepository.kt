package com.zameerahma.snakeapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zameerahma.snakeapp.game.Difficulty
import com.zameerahma.snakeapp.game.GameConfig
import com.zameerahma.snakeapp.game.WallMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snake_settings")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class Settings(
    val difficulty: Difficulty = Difficulty.EASY,
    val wallMode: WallMode = WallMode.SOLID,
    val obstaclesEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showControls: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    fun toGameConfig() = GameConfig(
        difficulty = difficulty,
        wallMode = wallMode,
        obstaclesEnabled = obstaclesEnabled,
    )
}

/** Saves settings and high scores on the device using Jetpack DataStore. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val WALL_MODE = stringPreferencesKey("wall_mode")
        val OBSTACLES = booleanPreferencesKey("obstacles")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
        val THEME = stringPreferencesKey("theme")
        fun highScore(difficulty: Difficulty) = intPreferencesKey("high_score_${difficulty.name}")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        val defaults = Settings()
        Settings(
            difficulty = enumOrDefault(p[Keys.DIFFICULTY], defaults.difficulty),
            wallMode = enumOrDefault(p[Keys.WALL_MODE], defaults.wallMode),
            obstaclesEnabled = p[Keys.OBSTACLES] ?: defaults.obstaclesEnabled,
            soundEnabled = p[Keys.SOUND] ?: defaults.soundEnabled,
            vibrationEnabled = p[Keys.VIBRATION] ?: defaults.vibrationEnabled,
            showControls = p[Keys.SHOW_CONTROLS] ?: defaults.showControls,
            themeMode = enumOrDefault(p[Keys.THEME], defaults.themeMode),
        )
    }

    val highScores: Flow<Map<Difficulty, Int>> = store.data.map { p ->
        Difficulty.entries.associateWith { p[Keys.highScore(it)] ?: 0 }
    }

    suspend fun setDifficulty(value: Difficulty) = store.edit { it[Keys.DIFFICULTY] = value.name }
    suspend fun setWallMode(value: WallMode) = store.edit { it[Keys.WALL_MODE] = value.name }
    suspend fun setObstacles(value: Boolean) = store.edit { it[Keys.OBSTACLES] = value }
    suspend fun setSound(value: Boolean) = store.edit { it[Keys.SOUND] = value }
    suspend fun setVibration(value: Boolean) = store.edit { it[Keys.VIBRATION] = value }
    suspend fun setShowControls(value: Boolean) = store.edit { it[Keys.SHOW_CONTROLS] = value }
    suspend fun setThemeMode(value: ThemeMode) = store.edit { it[Keys.THEME] = value.name }

    /** Saves [score] if it beats the best for [difficulty]. Returns true when it is a new record. */
    suspend fun submitScore(difficulty: Difficulty, score: Int): Boolean {
        var isRecord = false
        store.edit { prefs ->
            val key = Keys.highScore(difficulty)
            if (score > (prefs[key] ?: 0)) {
                prefs[key] = score
                isRecord = true
            }
        }
        return isRecord
    }

    suspend fun resetHighScores() = store.edit { prefs ->
        Difficulty.entries.forEach { prefs.remove(Keys.highScore(it)) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
