package com.zameerahma.snakeapp.ui.game

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zameerahma.snakeapp.game.Direction
import com.zameerahma.snakeapp.ui.components.PauseIcon

/** On-screen arrow buttons. The middle button pauses / resumes. */
@Composable
fun DirectionPad(
    isRunning: Boolean,
    onDirection: (Direction) -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 64.dp,
) {
    val gap = 6.dp
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        PadButton(Icons.Filled.KeyboardArrowUp, 0f, "Up", buttonSize) { onDirection(Direction.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            PadButton(Icons.Filled.KeyboardArrowUp, -90f, "Left", buttonSize) { onDirection(Direction.LEFT) }
            PadButton(
                icon = if (isRunning) PauseIcon else Icons.Filled.PlayArrow,
                rotation = 0f,
                description = if (isRunning) "Pause" else "Play",
                size = buttonSize,
                primary = false,
                onPress = onCenter,
            )
            PadButton(Icons.Filled.KeyboardArrowUp, 90f, "Right", buttonSize) { onDirection(Direction.RIGHT) }
        }
        PadButton(Icons.Filled.KeyboardArrowUp, 180f, "Down", buttonSize) { onDirection(Direction.DOWN) }
    }
}

/** Reacts the moment the finger touches (not on release) so turning feels instant. */
@Composable
private fun PadButton(
    icon: ImageVector,
    rotation: Float,
    description: String,
    size: Dp,
    primary: Boolean = true,
    onPress: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val currentOnPress by rememberUpdatedState(onPress)
    val colors = MaterialTheme.colorScheme
    val container = when {
        pressed -> colors.primary
        primary -> colors.secondaryContainer
        else -> colors.surfaceContainerHigh
    }
    val content = if (pressed) colors.onPrimary else colors.onSecondaryContainer
    Surface(
        shape = CircleShape,
        color = container,
        shadowElevation = if (pressed) 0.dp else 2.dp,
        modifier = Modifier
            .size(size)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { currentOnPress(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    currentOnPress()
                    tryAwaitRelease()
                    pressed = false
                })
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.55f).rotate(rotation))
        }
    }
}
