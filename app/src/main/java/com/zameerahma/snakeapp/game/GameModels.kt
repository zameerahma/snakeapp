package com.zameerahma.snakeapp.game

/** A cell on the game grid. (0, 0) is the top-left corner. */
data class Point(val x: Int, val y: Int)

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}

/**
 * Difficulty controls how fast the snake starts, how fast it can get,
 * how quickly it speeds up, the score multiplier and how many obstacles appear.
 * Tick = time in milliseconds between two snake moves (smaller = faster).
 */
enum class Difficulty(
    val label: String,
    val description: String,
    val startTickMs: Long,
    val minTickMs: Long,
    val speedUpPerFoodMs: Long,
    val scoreMultiplier: Int,
    val obstacleSegments: Int,
) {
    EASY(
        label = "Easy",
        description = "Slow start and gentle speed-up. Great for beginners.",
        startTickMs = 220, minTickMs = 120, speedUpPerFoodMs = 4,
        scoreMultiplier = 1, obstacleSegments = 2,
    ),
    MEDIUM(
        label = "Medium",
        description = "Faster snake and double points (x2).",
        startTickMs = 160, minTickMs = 85, speedUpPerFoodMs = 4,
        scoreMultiplier = 2, obstacleSegments = 4,
    ),
    HARD(
        label = "Hard",
        description = "Very fast with triple points (x3). For experts!",
        startTickMs = 115, minTickMs = 55, speedUpPerFoodMs = 3,
        scoreMultiplier = 3, obstacleSegments = 7,
    ),
}

enum class WallMode(val label: String, val description: String) {
    SOLID("Solid walls", "Hitting the edge ends the game"),
    WRAP("Wrap around", "Leave one side, appear on the other"),
}

enum class GameStatus { READY, RUNNING, PAUSED, GAME_OVER }

data class GameConfig(
    val columns: Int = 20,
    val rows: Int = 20,
    val difficulty: Difficulty = Difficulty.EASY,
    val wallMode: WallMode = WallMode.SOLID,
    val obstaclesEnabled: Boolean = true,
)

/** Golden bonus food that disappears after [ticksLeft] moves. */
data class BonusFood(val position: Point, val ticksLeft: Int)

data class GameState(
    val config: GameConfig,
    /** Snake body, head first. */
    val snake: List<Point>,
    val direction: Direction,
    /** Turns the player pressed that have not been applied yet (lets quick double-turns work). */
    val queuedDirections: List<Direction> = emptyList(),
    val food: Point?,
    val bonus: BonusFood? = null,
    val obstacles: Set<Point> = emptySet(),
    val score: Int = 0,
    val foodsEaten: Int = 0,
    val status: GameStatus = GameStatus.READY,
    /** True when the snake filled the whole board. */
    val won: Boolean = false,
) {
    val head: Point get() = snake.first()

    /** Current delay between moves. Gets shorter (faster) as more food is eaten. */
    val tickMs: Long
        get() {
            val d = config.difficulty
            return (d.startTickMs - foodsEaten * d.speedUpPerFoodMs).coerceAtLeast(d.minTickMs)
        }

    /** Speed shown to the player, from 1 (start) to 10 (max speed). */
    val speedLevel: Int
        get() {
            val d = config.difficulty
            val range = d.startTickMs - d.minTickMs
            return 1 + ((d.startTickMs - tickMs) * 9 / range).toInt()
        }
}

enum class GameEvent { ATE_FOOD, ATE_BONUS, BONUS_APPEARED, CRASHED, WON }

data class StepResult(val state: GameState, val events: Set<GameEvent>)
