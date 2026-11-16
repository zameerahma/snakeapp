package com.zameerahma.snakeapp.ui.game

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zameerahma.snakeapp.data.Settings
import com.zameerahma.snakeapp.game.Direction
import com.zameerahma.snakeapp.game.GameState
import com.zameerahma.snakeapp.game.GameStatus
import com.zameerahma.snakeapp.ui.GameViewModel
import com.zameerahma.snakeapp.ui.components.PauseIcon
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: Settings,
    highScores: Map<com.zameerahma.snakeapp.game.Difficulty, Int>,
    onExit: () -> Unit,
) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val newRecord by viewModel.newRecord.collectAsStateWithLifecycle()
    val state = game ?: return

    // Pause automatically when the app goes to the background (or the screen rotates).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }
    // Back button: pause first, then leave.
    BackHandler {
        if (state.status == GameStatus.RUNNING) viewModel.pause() else onExit()
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val best = maxOf(highScores[state.config.difficulty] ?: 0, state.score)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isRunning = state.status == GameStatus.RUNNING

    Box(
        Modifier
            .fillMaxSize()
            .swipeToTurn(viewModel::onDirection)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val direction = when (event.key) {
                    Key.DirectionUp, Key.W -> Direction.UP
                    Key.DirectionDown, Key.S -> Direction.DOWN
                    Key.DirectionLeft, Key.A -> Direction.LEFT
                    Key.DirectionRight, Key.D -> Direction.RIGHT
                    else -> null
                }
                when {
                    direction != null -> { viewModel.onDirection(direction); true }
                    event.key == Key.Spacebar || event.key == Key.P -> { viewModel.togglePause(); true }
                    else -> false
                }
            },
    ) {
        val board: @Composable (Modifier) -> Unit = { mod ->
            Box(mod, contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(1f)) {
                    GameBoard(
                        state,
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) { detectTapGestures { viewModel.start() } },
                    )
                    if (state.status == GameStatus.READY) ReadyHint(settings.showControls)
                }
            }
        }
        val pad: @Composable () -> Unit = {
            if (settings.showControls) {
                DirectionPad(
                    isRunning = isRunning,
                    onDirection = viewModel::onDirection,
                    onCenter = viewModel::togglePause,
                    buttonSize = if (landscape) 56.dp else 64.dp,
                )
            }
        }

        val content = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)
        if (landscape) {
            Row(content, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.fillMaxHeight().weight(1.4f))
                Column(
                    Modifier.fillMaxHeight().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Hud(state, best, onPause = viewModel::togglePause)
                    pad()
                }
            }
        } else {
            Column(content, horizontalAlignment = Alignment.CenterHorizontally) {
                Hud(state, best, onPause = viewModel::togglePause)
                Spacer(Modifier.height(12.dp))
                board(Modifier.fillMaxWidth().weight(1f))
                if (settings.showControls) {
                    Spacer(Modifier.height(16.dp))
                    pad()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        when (state.status) {
            GameStatus.PAUSED -> PausedOverlay(
                onResume = viewModel::resume,
                onRestart = viewModel::newGame,
                onMenu = onExit,
            )
            GameStatus.GAME_OVER -> GameOverOverlay(
                state = state,
                best = best,
                newRecord = newRecord,
                onPlayAgain = viewModel::newGame,
                onMenu = onExit,
            )
            else -> Unit
        }
    }
}

/** Turns a finger swipe anywhere on the screen into a direction. Several turns fit in one swipe. */
private fun Modifier.swipeToTurn(onSwipe: (Direction) -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        val threshold = 24.dp.toPx()
        var accumulated = Offset.Zero
        detectDragGestures(onDragStart = { accumulated = Offset.Zero }) { change, drag ->
            change.consume()
            accumulated += drag
            if (accumulated.getDistance() >= threshold) {
                val direction = if (abs(accumulated.x) > abs(accumulated.y)) {
                    if (accumulated.x > 0) Direction.RIGHT else Direction.LEFT
                } else {
                    if (accumulated.y > 0) Direction.DOWN else Direction.UP
                }
                onSwipe(direction)
                accumulated = Offset.Zero
            }
        }
    }
)

@Composable
private fun Hud(state: GameState, best: Int, onPause: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text("SCORE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${state.score}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        HudStat("BEST", "$best")
        HudStat("SPEED", "${state.speedLevel}")
        HudStat("LEVEL", state.config.difficulty.label)
        FilledTonalIconButton(onClick = onPause, enabled = state.status != GameStatus.GAME_OVER) {
            Icon(
                if (state.status == GameStatus.RUNNING) PauseIcon else Icons.Filled.PlayArrow,
                contentDescription = if (state.status == GameStatus.RUNNING) "Pause" else "Play",
            )
        }
    }
}

@Composable
private fun HudStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReadyHint(showControls: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ready?", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (showControls) "Swipe, press an arrow\nor tap the board to start" else "Swipe or tap the board to start",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // Swallow touches so the game underneath is not affected.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.padding(16.dp).widthIn(max = 360.dp),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit) {
    OverlayScrim {
        Text("Paused", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.padding(4.dp)); Text("Resume")
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Refresh, null); Spacer(Modifier.padding(4.dp)); Text("Restart")
        }
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Home, null); Spacer(Modifier.padding(4.dp)); Text("Main menu")
        }
    }
}

@Composable
private fun GameOverOverlay(
    state: GameState,
    best: Int,
    newRecord: Boolean,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val playAgain by rememberUpdatedState(onPlayAgain)
    OverlayScrim {
        Text(
            if (state.won) "You win!" else "Game over",
            style = MaterialTheme.typography.headlineMedium,
        )
        if (newRecord) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.padding(3.dp))
                    Text("New high score!", color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("${state.score}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
        Text(
            "Best (${state.config.difficulty.label}): $best  ·  Length: ${state.snake.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = playAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Icon(Icons.Filled.Refresh, null); Spacer(Modifier.padding(4.dp)); Text("Play again")
        }
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Home, null); Spacer(Modifier.padding(4.dp)); Text("Main menu")
        }
    }
}
