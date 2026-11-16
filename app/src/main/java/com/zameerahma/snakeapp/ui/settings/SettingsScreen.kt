package com.zameerahma.snakeapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zameerahma.snakeapp.data.Settings
import com.zameerahma.snakeapp.data.ThemeMode
import com.zameerahma.snakeapp.game.Difficulty
import com.zameerahma.snakeapp.game.WallMode
import com.zameerahma.snakeapp.ui.SettingsViewModel
import com.zameerahma.snakeapp.ui.components.ChoiceRow
import com.zameerahma.snakeapp.ui.components.ScreenHeader
import com.zameerahma.snakeapp.ui.components.SectionTitle
import com.zameerahma.snakeapp.ui.components.SettingSwitch

@Composable
fun SettingsScreen(settings: Settings, viewModel: SettingsViewModel, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            ScreenHeader("Settings", onBack)

            SectionTitle("Gameplay")
            Text("Difficulty", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            ChoiceRow(Difficulty.entries, settings.difficulty, { it.label }, viewModel::setDifficulty)
            Text(
                settings.difficulty.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )

            Text("Walls", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            ChoiceRow(WallMode.entries, settings.wallMode, { it.label }, viewModel::setWallMode)
            Text(
                settings.wallMode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )

            SettingSwitch(
                title = "Obstacles",
                subtitle = "Stone blocks on the board (more on harder levels)",
                checked = settings.obstaclesEnabled,
                onChange = viewModel::setObstacles,
            )

            SectionTitle("Controls")
            SettingSwitch(
                title = "On-screen arrow buttons",
                subtitle = "Swiping on the screen always works too",
                checked = settings.showControls,
                onChange = viewModel::setShowControls,
            )

            SectionTitle("Sound & vibration")
            SettingSwitch("Sound effects", "Beeps when eating and on game over", settings.soundEnabled, viewModel::setSound)
            SettingSwitch("Vibration", "Short buzz when eating and crashing", settings.vibrationEnabled, viewModel::setVibration)

            SectionTitle("Appearance")
            ChoiceRow(ThemeMode.entries, settings.themeMode, { it.label }, viewModel::setThemeMode)

            Text(
                "Changes to gameplay apply to the next game.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}
