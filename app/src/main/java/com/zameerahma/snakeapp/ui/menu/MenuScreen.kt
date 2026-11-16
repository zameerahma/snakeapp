package com.zameerahma.snakeapp.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zameerahma.snakeapp.data.Settings
import com.zameerahma.snakeapp.game.Difficulty
import com.zameerahma.snakeapp.ui.theme.LocalBoardColors

@Composable
fun MenuScreen(
    settings: Settings,
    highScores: Map<Difficulty, Int>,
    onSelectDifficulty: (Difficulty) -> Unit,
    onPlay: () -> Unit,
    onHighScores: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            SnakeLogo()
            Text("SNAKE", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Text("Eat, grow, don't bite yourself!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            Text(
                "Choose difficulty",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            Difficulty.entries.forEach { difficulty ->
                DifficultyCard(
                    difficulty = difficulty,
                    selected = difficulty == settings.difficulty,
                    best = highScores[difficulty] ?: 0,
                    onClick = { onSelectDifficulty(difficulty) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                "${settings.wallMode.label} · Obstacles ${if (settings.obstaclesEnabled) "on" else "off"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("PLAY", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onHighScores, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Star, null); Spacer(Modifier.width(6.dp)); Text("High scores")
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(6.dp)); Text("Settings")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, selected: Boolean, best: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(difficulty.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(difficulty.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BEST", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("$best", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** A small snake chasing an apple, drawn with the same colors as the game board. */
@Composable
private fun SnakeLogo() {
    val board = LocalBoardColors.current
    Canvas(Modifier.size(width = 200.dp, height = 40.dp)) {
        val cell = size.height
        val segments = 4
        for (i in 0 until segments) {
            val color = lerp(board.snakeHead, board.snakeTail, (segments - 1 - i) / (segments - 1f))
            drawRoundRect(
                color,
                topLeft = Offset(i * cell, cell * 0.1f),
                size = Size(cell * 1.02f, cell * 0.8f),
                cornerRadius = CornerRadius(cell * 0.3f),
            )
        }
        val head = Offset((segments - 0.5f) * cell, cell / 2)
        drawCircle(Color.White, cell * 0.12f, head + Offset(cell * 0.12f, -cell * 0.16f))
        drawCircle(Color.White, cell * 0.12f, head + Offset(cell * 0.12f, cell * 0.16f))
        drawCircle(Color(0xFF111111), cell * 0.06f, head + Offset(cell * 0.16f, -cell * 0.16f))
        drawCircle(Color(0xFF111111), cell * 0.06f, head + Offset(cell * 0.16f, cell * 0.16f))
        val apple = Offset(size.width - cell * 0.6f, cell / 2 + cell * 0.04f)
        drawCircle(board.food, cell * 0.34f, apple)
        drawOval(board.leaf, topLeft = apple + Offset(cell * 0.02f, -cell * 0.52f), size = Size(cell * 0.28f, cell * 0.16f))
    }
    Spacer(Modifier.height(8.dp))
}
