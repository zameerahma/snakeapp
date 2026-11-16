package com.zameerahma.snakeapp.game

import kotlin.random.Random

/**
 * All snake game rules live here. Every function takes a [GameState] and returns a new one,
 * so the rules are easy to read and to unit test (see GameEngineTest).
 */
class GameEngine(private val random: Random = Random.Default) {

    companion object {
        const val START_LENGTH = 3
        const val FOOD_POINTS = 10
        const val BONUS_POINTS = 50
        /** A bonus appears after every N normal foods. */
        const val BONUS_EVERY = 5
        const val BONUS_LIFETIME_TICKS = 40
        const val MAX_QUEUED_TURNS = 2
    }

    fun newGame(config: GameConfig): GameState {
        val y = config.rows / 2
        val headX = config.columns / 4 + START_LENGTH - 1
        val snake = List(START_LENGTH) { Point(headX - it, y) }
        val obstacles = if (config.obstaclesEnabled) generateObstacles(config, y) else emptySet()
        val state = GameState(
            config = config,
            snake = snake,
            direction = Direction.RIGHT,
            food = null,
            obstacles = obstacles,
        )
        return state.copy(food = randomFreeCell(state))
    }

    fun start(state: GameState): GameState =
        if (state.status == GameStatus.READY) state.copy(status = GameStatus.RUNNING) else state

    fun pause(state: GameState): GameState =
        if (state.status == GameStatus.RUNNING) state.copy(status = GameStatus.PAUSED) else state

    fun resume(state: GameState): GameState =
        if (state.status == GameStatus.PAUSED) state.copy(status = GameStatus.RUNNING) else state

    /** Queues a turn. Reversing straight into yourself (e.g. RIGHT -> LEFT) is ignored. */
    fun queueDirection(state: GameState, direction: Direction): GameState {
        if (state.status == GameStatus.GAME_OVER) return state
        val last = state.queuedDirections.lastOrNull() ?: state.direction
        if (direction == last || direction == last.opposite) return state
        if (state.queuedDirections.size >= MAX_QUEUED_TURNS) return state
        return state.copy(queuedDirections = state.queuedDirections + direction)
    }

    /** Moves the snake one cell. Called once per game tick. */
    fun step(state: GameState): StepResult {
        if (state.status != GameStatus.RUNNING) return StepResult(state, emptySet())

        val config = state.config
        val direction = state.queuedDirections.firstOrNull() ?: state.direction
        val moved = state.copy(direction = direction, queuedDirections = state.queuedDirections.drop(1))

        var next = Point(state.head.x + direction.dx, state.head.y + direction.dy)
        val outside = next.x !in 0 until config.columns || next.y !in 0 until config.rows
        if (outside) {
            if (config.wallMode == WallMode.SOLID) return crash(moved)
            next = Point(next.x.mod(config.columns), next.y.mod(config.rows))
        }

        val eatsFood = next == state.food
        val eatsBonus = next == state.bonus?.position
        val grows = eatsFood || eatsBonus
        // When the snake does not grow, its tail moves away, so moving into the tail cell is safe.
        val body = if (grows) state.snake else state.snake.dropLast(1)
        if (next in state.obstacles || next in body) return crash(moved)

        val events = mutableSetOf<GameEvent>()
        val multiplier = config.difficulty.scoreMultiplier
        var score = state.score
        var foodsEaten = state.foodsEaten

        var bonus = state.bonus
            ?.let { it.copy(ticksLeft = it.ticksLeft - 1) }
            ?.takeIf { it.ticksLeft > 0 }
        if (eatsBonus) {
            score += BONUS_POINTS * multiplier
            bonus = null
            events += GameEvent.ATE_BONUS
        }
        if (eatsFood) {
            score += FOOD_POINTS * multiplier
            foodsEaten++
            events += GameEvent.ATE_FOOD
        }

        var newState = moved.copy(
            snake = listOf(next) + body,
            score = score,
            foodsEaten = foodsEaten,
            bonus = bonus,
            food = if (eatsFood) null else state.food,
        )

        if (eatsFood) {
            val food = randomFreeCell(newState)
                ?: return StepResult(
                    newState.copy(status = GameStatus.GAME_OVER, won = true),
                    events + GameEvent.WON,
                )
            newState = newState.copy(food = food)

            if (foodsEaten % BONUS_EVERY == 0 && newState.bonus == null) {
                randomFreeCell(newState)?.let {
                    newState = newState.copy(bonus = BonusFood(it, BONUS_LIFETIME_TICKS))
                    events += GameEvent.BONUS_APPEARED
                }
            }
        }
        return StepResult(newState, events)
    }

    private fun crash(state: GameState) =
        StepResult(state.copy(status = GameStatus.GAME_OVER), setOf(GameEvent.CRASHED))

    private fun randomFreeCell(state: GameState): Point? {
        val taken = HashSet<Point>(state.snake.size + state.obstacles.size + 2).apply {
            addAll(state.snake)
            addAll(state.obstacles)
            state.food?.let(::add)
            state.bonus?.let { add(it.position) }
        }
        val free = ArrayList<Point>()
        for (y in 0 until state.config.rows) {
            for (x in 0 until state.config.columns) {
                val p = Point(x, y)
                if (p !in taken) free += p
            }
        }
        return if (free.isEmpty()) null else free[random.nextInt(free.size)]
    }

    /**
     * Places short wall pieces (2-3 cells) inside the board, keeping the snake's
     * starting lane (and the rows next to it) clear so the start is always safe.
     */
    private fun generateObstacles(config: GameConfig, startRow: Int): Set<Point> {
        val result = mutableSetOf<Point>()
        var placed = 0
        var attempts = 0
        while (placed < config.difficulty.obstacleSegments && attempts < 500) {
            attempts++
            val length = 2 + random.nextInt(2)
            val horizontal = random.nextBoolean()
            val startX = random.nextInt(1, config.columns - 1)
            val startY = random.nextInt(1, config.rows - 1)
            val cells = List(length) {
                if (horizontal) Point(startX + it, startY) else Point(startX, startY + it)
            }
            val valid = cells.all { c ->
                c.x in 1 until config.columns - 1 &&
                    c.y in 1 until config.rows - 1 &&
                    kotlin.math.abs(c.y - startRow) > 2 &&
                    result.none { o -> kotlin.math.abs(o.x - c.x) <= 1 && kotlin.math.abs(o.y - c.y) <= 1 }
            }
            if (valid) {
                result += cells
                placed++
            }
        }
        return result
    }
}
