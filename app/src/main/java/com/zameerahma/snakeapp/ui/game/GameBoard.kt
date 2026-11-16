package com.zameerahma.snakeapp.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zameerahma.snakeapp.game.Direction
import com.zameerahma.snakeapp.game.GameState
import com.zameerahma.snakeapp.game.GameStatus
import com.zameerahma.snakeapp.game.Point
import com.zameerahma.snakeapp.ui.theme.BoardColors
import com.zameerahma.snakeapp.ui.theme.LocalBoardColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Draws the grid, obstacles, food, bonus star and the snake. */
@Composable
fun GameBoard(state: GameState, modifier: Modifier = Modifier) {
    val colors = LocalBoardColors.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "pulse",
    )
    Canvas(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.board)
            .semantics { contentDescription = "Game board. Score ${state.score}" }
    ) {
        val cell = size.width / state.config.columns
        drawChecker(state, cell, colors)
        state.obstacles.forEach { drawObstacle(it, cell, colors) }
        state.food?.let { drawApple(it, cell, colors, pulse) }
        state.bonus?.let { bonus ->
            // Blink during the last few moves so the player knows it is about to vanish.
            val visible = bonus.ticksLeft > 10 || bonus.ticksLeft % 2 == 0
            if (visible) drawStar(bonus.position, cell, colors.bonus, pulse)
        }
        drawSnake(state, cell, colors)
    }
}

private fun DrawScope.drawChecker(state: GameState, cell: Float, colors: BoardColors) {
    for (y in 0 until state.config.rows) {
        for (x in 0 until state.config.columns) {
            if ((x + y) % 2 == 0) {
                drawRect(colors.boardAlt, topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
            }
        }
    }
}

private fun DrawScope.drawObstacle(p: Point, cell: Float, colors: BoardColors) {
    val inset = cell * 0.06f
    drawRoundRect(
        colors.obstacleEdge,
        topLeft = Offset(p.x * cell + inset, p.y * cell + inset),
        size = Size(cell - inset * 2, cell - inset * 2),
        cornerRadius = CornerRadius(cell * 0.2f),
    )
    val inner = cell * 0.16f
    drawRoundRect(
        colors.obstacle,
        topLeft = Offset(p.x * cell + inner, p.y * cell + inner),
        size = Size(cell - inner * 2, cell - inner * 2),
        cornerRadius = CornerRadius(cell * 0.15f),
    )
}

private fun DrawScope.drawApple(p: Point, cell: Float, colors: BoardColors, pulse: Float) {
    val center = Offset((p.x + 0.5f) * cell, (p.y + 0.55f) * cell)
    val r = cell * 0.36f * (0.9f + 0.1f * pulse)
    drawCircle(colors.food, radius = r, center = center)
    drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 0.3f, center = center + Offset(-r * 0.35f, -r * 0.35f))
    // Leaf
    rotate(-35f, pivot = center + Offset(r * 0.35f, -r * 1.05f)) {
        drawOval(
            colors.leaf,
            topLeft = center + Offset(r * 0.05f, -r * 1.3f),
            size = Size(r * 0.7f, r * 0.4f),
        )
    }
}

private fun DrawScope.drawStar(p: Point, cell: Float, color: Color, pulse: Float) {
    val center = Offset((p.x + 0.5f) * cell, (p.y + 0.5f) * cell)
    val outer = cell * 0.5f * pulse
    val inner = outer * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val point = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawCircle(color.copy(alpha = 0.25f), radius = outer * 1.1f, center = center)
    drawPath(path, color)
}

private fun DrawScope.drawSnake(state: GameState, cell: Float, colors: BoardColors) {
    val snake = state.snake
    val crashed = state.status == GameStatus.GAME_OVER && !state.won
    val inset = cell * 0.08f
    val segment = cell - inset * 2
    val last = (snake.size - 1).coerceAtLeast(1)

    // Draw from the tail to the head so the head is on top.
    for (i in snake.indices.reversed()) {
        val p = snake[i]
        val color = when {
            i == 0 && crashed -> colors.crashed
            else -> lerp(colors.snakeHead, colors.snakeTail, i / last.toFloat())
        }
        // Fill the gap to the next segment so the body looks continuous
        // (skipped when the snake wraps around the board edge).
        if (i > 0) {
            val towardHead = snake[i - 1]
            if (abs(towardHead.x - p.x) + abs(towardHead.y - p.y) == 1) {
                val left = minOf(p.x, towardHead.x) * cell + inset
                val top = minOf(p.y, towardHead.y) * cell + inset
                val width = (abs(towardHead.x - p.x) + 1) * cell - inset * 2
                val height = (abs(towardHead.y - p.y) + 1) * cell - inset * 2
                drawRect(color, topLeft = Offset(left, top), size = Size(width, height))
            }
        }
        val radius = if (i == 0) cell * 0.38f else cell * 0.3f
        drawRoundRect(
            color,
            topLeft = Offset(p.x * cell + inset, p.y * cell + inset),
            size = Size(segment, segment),
            cornerRadius = CornerRadius(radius),
        )
    }
    if (snake.isNotEmpty()) drawEyes(snake.first(), state.direction, cell, crashed)
}

private fun DrawScope.drawEyes(head: Point, direction: Direction, cell: Float, crashed: Boolean) {
    val center = Offset((head.x + 0.5f) * cell, (head.y + 0.5f) * cell)
    val forward = Offset(direction.dx.toFloat(), direction.dy.toFloat())
    val side = Offset(-forward.y, forward.x)
    val eyeR = cell * 0.13f
    listOf(1f, -1f).forEach { s ->
        val eye = center + forward * (cell * 0.12f) + side * (s * cell * 0.2f)
        drawCircle(Color.White, radius = eyeR, center = eye)
        if (crashed) {
            val d = eyeR * 0.6f
            drawLine(Color.Black, eye + Offset(-d, -d), eye + Offset(d, d), strokeWidth = eyeR * 0.4f)
            drawLine(Color.Black, eye + Offset(-d, d), eye + Offset(d, -d), strokeWidth = eyeR * 0.4f)
        } else {
            drawCircle(Color(0xFF111111), radius = eyeR * 0.55f, center = eye + forward * (eyeR * 0.35f))
        }
    }
}
