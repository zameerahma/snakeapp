package com.zameerahma.snakeapp

import android.app.Application
import com.zameerahma.snakeapp.audio.Haptics
import com.zameerahma.snakeapp.audio.SoundPlayer
import com.zameerahma.snakeapp.data.SettingsRepository

class SnakeApplication : Application() {
    /** Simple app-wide objects shared by the ViewModels. */
    val settingsRepository by lazy { SettingsRepository(this) }
    val soundPlayer by lazy { SoundPlayer(this) }
    val haptics by lazy { Haptics(this) }
}
