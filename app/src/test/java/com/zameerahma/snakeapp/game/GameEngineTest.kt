package com.zameerahma.snakeapp.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val engine = GameEngine(Random(42))

    /** A running game with a hand-placed snake, no obstacles and food far away. */
    private fun runningState(
        snake: List<Point> = listOf(Point(5, 5), Point(4, 5), Point(3, 5)),
        direction: Direction = Direction.RIGHT,
        food: Point = Point(0, 0),
        wallMode: WallMode = WallMode.SOLID,
        difficulty: Difficulty = Difficulty.EASY,
    ) = GameState(
        config = GameConfig(columns = 10, rows = 10, difficulty = difficulty, wallMode = wallMode, obstaclesEnabled = false),
        snake = snake,
        direction = direction,
        food = food,
        status = GameStatus.RUNNING,
    )

    @Test
    fun newGame_isReadyWithSnakeAndFood() {
        val state = engine.newGame(GameConfig())
        assertEquals(GameStatus.READY, state.status)
        assertEquals(GameEngine.START_LENGTH, state.snake.size)
        assertNotNull(state.food)
        assertFalse(state.food in state.snake)
        assertFalse(state.food in state.obstacles)
    }

    @Test
    fun newGame_obstaclesNeverBlockStartingLane() {
        repeat(50) { seed ->
            val state = GameEngine(Random(seed)).newGame(GameConfig(difficulty = Difficulty.HARD))
            val row = state.head.y
            assertTrue(state.obstacles.isNotEmpty())
            assertTrue(state.obstacles.none { kotlin.math.abs(it.y - row) <= 2 })
        }
    }

    @Test
    fun step_movesSnakeForward() {
        val result = engine.step(runningState())
        assertEquals(listOf(Point(6, 5), Point(5, 5), Point(4, 5)), result.state.snake)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun step_eatingFoodGrowsSnakeAndAddsScore() {
        val result = engine.step(runningState(food = Point(6, 5), difficulty = Difficulty.MEDIUM))
        assertEquals(4, result.state.snake.size)
        assertEquals(GameEngine.FOOD_POINTS * 2, result.state.score)
        assertEquals(1, result.state.foodsEaten)
        assertTrue(GameEvent.ATE_FOOD in result.events)
        assertNotNull(result.state.food)
        assertFalse(result.state.food in result.state.snake)
    }

    @Test
    fun solidWalls_hittingEdgeEndsGame() {
        val state = runningState(snake = listOf(Point(9, 5), Point(8, 5), Point(7, 5)))
        val result = engine.step(state)
        assertEquals(GameStatus.GAME_OVER, result.state.status)
        assertTrue(GameEvent.CRASHED in result.events)
    }

    @Test
    fun wrapMode_snakeComesOutOtherSide() {
        val state = runningState(snake = listOf(Point(9, 5), Point(8, 5), Point(7, 5)), wallMode = WallMode.WRAP)
        val result = engine.step(state)
        assertEquals(GameStatus.RUNNING, result.state.status)
        assertEquals(Point(0, 5), result.state.head)
    }

    @Test
    fun runningIntoOwnBodyEndsGame() {
        // Snake shaped like a hook; turning up runs into its own body.
        val snake = listOf(Point(5, 5), Point(4, 5), Point(4, 4), Point(5, 4), Point(6, 4))
        val result = engine.step(runningState(snake = snake, direction = Direction.UP))
        assertEquals(GameStatus.GAME_OVER, result.state.status)
    }

    @Test
    fun movingIntoTailCellIsAllowed() {
        // A 2x2 loop: the head moves where the tail is leaving.
        val snake = listOf(Point(5, 5), Point(5, 6), Point(4, 6), Point(4, 5))
        val result = engine.step(runningState(snake = snake, direction = Direction.LEFT))
        assertEquals(GameStatus.RUNNING, result.state.status)
        assertEquals(Point(4, 5), result.state.head)
    }

    @Test
    fun obstacleCollisionEndsGame() {
        val state = runningState().copy(obstacles = setOf(Point(6, 5)))
        assertEquals(GameStatus.GAME_OVER, engine.step(state).state.status)
    }

    @Test
    fun reverseDirectionIsIgnored() {
        val state = engine.queueDirection(runningState(), Direction.LEFT)
        assertTrue(state.queuedDirections.isEmpty())
    }

    @Test
    fun quickDoubleTurnIsQueued() {
        var state = engine.queueDirection(runningState(), Direction.UP)
        state = engine.queueDirection(state, Direction.LEFT)
        assertEquals(listOf(Direction.UP, Direction.LEFT), state.queuedDirections)
        state = engine.step(state).state
        assertEquals(Direction.UP, state.direction)
        state = engine.step(state).state
        assertEquals(Direction.LEFT, state.direction)
    }

    @Test
    fun speedIncreasesAsFoodIsEaten() {
        val slow = runningState()
        val fast = slow.copy(foodsEaten = 10)
        assertTrue(fast.tickMs < slow.tickMs)
        assertEquals(1, slow.speedLevel)
        val max = slow.copy(foodsEaten = 1000)
        assertEquals(Difficulty.EASY.minTickMs, max.tickMs)
        assertEquals(10, max.speedLevel)
    }

    @Test
    fun bonusAppearsEveryFiveFoodsAndExpires() {
        val state = runningState(food = Point(6, 5)).copy(foodsEaten = GameEngine.BONUS_EVERY - 1)
        val result = engine.step(state)
        assertTrue(GameEvent.BONUS_APPEARED in result.events)
        val bonus = assertNotNull(result.state.bonus)
        assertEquals(GameEngine.BONUS_LIFETIME_TICKS, bonus.ticksLeft)

        val expiring = runningState(snake = listOf(Point(1, 1), Point(0, 1), Point(0, 0)), food = Point(9, 9))
            .copy(bonus = BonusFood(Point(8, 8), ticksLeft = 1))
        assertEquals(null, engine.step(expiring).state.bonus)
    }

    @Test
    fun eatingBonusGivesBonusPoints() {
        val state = runningState().copy(bonus = BonusFood(Point(6, 5), 10))
        val result = engine.step(state)
        assertEquals(GameEngine.BONUS_POINTS, result.state.score)
        assertEquals(null, result.state.bonus)
        assertTrue(GameEvent.ATE_BONUS in result.events)
    }

    @Test
    fun pauseAndResume() {
        val paused = engine.pause(runningState())
        assertEquals(GameStatus.PAUSED, paused.status)
        assertEquals(paused, engine.step(paused).state)
        assertEquals(GameStatus.RUNNING, engine.resume(paused).status)
    }

    private fun <T> assertNotNull(value: T?): T {
        org.junit.Assert.assertNotNull(value)
        return value!!
    }
}
