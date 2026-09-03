package com.example.snake

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** 游戏网格上的一个格子。 */
data class Cell(val x: Int, val y: Int)

/** 四个移动方向，携带它们在网格上的位移量。 */
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: Direction): Boolean = dx == -other.dx && dy == -other.dy
}

/**
 * 贪吃蛇的全部游戏状态与规则，都从 UI 中分离出来，这样 composable 组件只需渲染它。棋盘是
 * [GRID] x [GRID] 个格子；每个 tick 蛇沿当前方向前进一格，吃掉 [food] 会成长并加分，
 * 撞到墙或自身时游戏结束。这些属性都由 [mutableStateOf] 支撑，因此每当它们变化时
 * Compose 都会重组。
 */
class SnakeGameState {
    var snake by mutableStateOf(listOf(Cell(GRID / 2, GRID / 2)))
        private set
    var direction by mutableStateOf(Direction.RIGHT)
        private set
    var food by mutableStateOf(Cell(GRID / 4, GRID / 2))
        private set
    var score by mutableStateOf(0)
        private set
    var bestScore by mutableStateOf(0)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var isRunning by mutableStateOf(false)
        private set

    // 自上一个 tick 以来请求的方向。在 [step] 开头应用，以免一个 tick 内的两次快速滑动
    // 让蛇径直折回自己的身上。
    private var queued: Direction = Direction.RIGHT

    /** 开始（或恢复）游戏。如果上一局已结束，则会自动开始一局新游戏。 */
    fun start() {
        if (isGameOver) reset()
        isRunning = true
    }

    fun pause() {
        isRunning = false
    }

    /** 让棋盘回到初始、尚未开始运行的状态。 */
    fun reset() {
        snake = listOf(Cell(GRID / 2, GRID / 2))
        direction = Direction.RIGHT
        queued = Direction.RIGHT
        score = 0
        isGameOver = false
        isRunning = false
        food = randomFood(snake)
    }

    /** 将转向加入队列，并忽略任何会折回蛇身的方向。 */
    fun turn(next: Direction) {
        if (!next.isOpposite(direction)) queued = next
    }

    /** 让模拟向前推进一格。除非游戏正在运行，否则不执行任何操作。 */
    fun step() {
        if (!isRunning || isGameOver) return
        direction = queued
        val head = snake.first()
        val next = Cell(head.x + direction.dx, head.y + direction.dy)

        val hitWall = next.x < 0 || next.y < 0 || next.x >= GRID || next.y >= GRID
        val willEat = !hitWall && next == food
        // 当蛇即将吃到食物时会保留尾巴（变长）；否则尾巴所在格会在同一 tick 内空出，
        // 因此可以移动进去。
        val bodyAfter = if (willEat) snake else snake.dropLast(1)
        if (hitWall || next in bodyAfter) {
            isGameOver = true
            isRunning = false
            bestScore = maxOf(bestScore, score)
            return
        }

        snake = listOf(next) + bodyAfter
        if (willEat) {
            score += 10
            food = randomFood(snake)
        }
    }

    /** 为下一个食物随机挑选一个空格子。 */
    private fun randomFood(occupied: List<Cell>): Cell {
        val free = ArrayList<Cell>(GRID * GRID)
        for (x in 0 until GRID) {
            for (y in 0 until GRID) {
                val cell = Cell(x, y)
                if (cell !in occupied) free.add(cell)
            }
        }
        return if (free.isEmpty()) occupied.first() else free[Random.nextInt(free.size)]
    }

    companion object {
        /** 棋盘是一个 GRID x GRID 个格子的正方形。 */
        const val GRID = 20
    }
}
