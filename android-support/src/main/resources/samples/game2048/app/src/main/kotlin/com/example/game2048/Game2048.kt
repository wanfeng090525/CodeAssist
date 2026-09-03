package com.example.game2048

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** 一个滑动方向。每次移动都被表达为对重新定向后的棋盘做一次向左折叠（参见 [plan]）。 */
enum class Direction { LEFT, RIGHT, UP, DOWN }

/**
 * 一个带有**稳定的 [id]**、在多次移动之间保持不变的方块，这样 UI 就可以让同一个方块从旧格
 * 动画滑到新格。两个合并的方块会滑到同一格；一个保留其 id（其值翻倍），另一个在滑动
 * 结束后被移除。
 */
data class Tile(val id: Int, val value: Int, val row: Int, val col: Int)

/**
 * 2048 的状态与规则，以 [Tile] 列表（而非扁平的网格）建模，以便可以动画化移动过程。
 * 一次移动分两个由 UI 驱动的阶段：
 *
 *  1. [beginMove] 计算结果并发布**滑动中**的方块 —— 每个当前方块移动到其目标格
 *     （合并的一对落到同一格），值保持不变。UI 对这些偏移做动画。
 *  2. 滑动结束后，[endMove] 发布**已落定**的棋盘 —— 合并的格子显示翻倍后的值、被吸收的
 *     方块消失、生成了一个新的方块 —— 并更新分数 / 游戏结束状态。
 *
 * 在两者之间 [animating] 为 true，因此滑动结束前会忽略输入。
 */
class Game2048State {
    var tiles by mutableStateOf(emptyList<Tile>())
        private set
    var score by mutableStateOf(0)
        private set
    var best by mutableStateOf(0)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var hasWon by mutableStateOf(false)
        private set

    /** 每次接受的移动都会递增，以便 UI 的落定效果重新执行。 */
    var moveToken by mutableStateOf(0)
        private set

    val animating: Boolean get() = pending != null

    private var pending: Plan? = null
    private var nextId = 0

    init {
        newGame()
    }

    fun newGame() {
        pending = null
        tiles = spawn(spawn(emptyList()))
        score = 0
        isGameOver = false
        hasWon = false
        moveToken++
    }

    /** 阶段 1：发布滑动中的方块。如果棋盘无法朝该方向移动则返回 false（无操作）。 */
    fun beginMove(direction: Direction): Boolean {
        if (pending != null || isGameOver) return false
        val plan = plan(direction) ?: return false
        pending = plan
        tiles = plan.slide
        moveToken++
        return true
    }

    /** 阶段 2：让棋盘落定（合并值、生成方块）、更新分数与结束状态。 */
    fun endMove() {
        val plan = pending ?: return
        pending = null
        tiles = plan.settled
        score += plan.gained
        best = maxOf(best, score)
        if (!hasWon && plan.settled.any { it.value >= 2048 }) hasWon = true
        if (!hasMove(plan.settled)) isGameOver = true
    }

    private class Plan(val slide: List<Tile>, val settled: List<Tile>, val gained: Int)

    private fun plan(direction: Direction): Plan? {
        val grid = arrayOfNulls<Tile>(SIZE * SIZE)
        for (t in tiles) grid[t.row * SIZE + t.col] = t

        val slide = ArrayList<Tile>()
        val settled = ArrayList<Tile>()
        var gained = 0
        var moved = false

        for (line in lineCoords(direction)) {
            val lineTiles = line.mapNotNull { (r, c) -> grid[r * SIZE + c] }
            var outIndex = 0
            var canMerge = false // 最后一个已填充槽位上的方块是否还能再吸收一个
            for (t in lineTiles) {
                if (canMerge && settled.last().value == t.value) {
                    val (dr, dc) = line[outIndex - 1]
                    slide.add(t.copy(row = dr, col = dc)) // 被吸收的方块滑到幸存方块上
                    settled[settled.lastIndex] = settled.last().copy(value = settled.last().value * 2)
                    gained += settled.last().value
                    canMerge = false
                    moved = true
                } else {
                    val (dr, dc) = line[outIndex]
                    slide.add(t.copy(row = dr, col = dc))
                    settled.add(t.copy(row = dr, col = dc))
                    if (dr != t.row || dc != t.col) moved = true
                    canMerge = true
                    outIndex++
                }
            }
        }
        if (!moved) return null
        return Plan(slide, spawn(settled), gained)
    }

    /** 每条线的棋盘格子，从前沿（方块滑动朝向的边）向内排序。 */
    private fun lineCoords(direction: Direction): List<List<Pair<Int, Int>>> {
        val idx = 0 until SIZE
        return when (direction) {
            Direction.LEFT -> idx.map { r -> idx.map { c -> r to c } }
            Direction.RIGHT -> idx.map { r -> idx.reversed().map { c -> r to c } }
            Direction.UP -> idx.map { c -> idx.map { r -> r to c } }
            Direction.DOWN -> idx.map { c -> idx.reversed().map { r -> r to c } }
        }
    }

    private fun spawn(tiles: List<Tile>): List<Tile> {
        val occupied = tiles.mapTo(HashSet()) { it.row to it.col }
        val free = ArrayList<Pair<Int, Int>>()
        for (r in 0 until SIZE) for (c in 0 until SIZE) if ((r to c) !in occupied) free.add(r to c)
        if (free.isEmpty()) return tiles
        val (r, c) = free[Random.nextInt(free.size)]
        val value = if (Random.nextInt(10) == 0) 4 else 2
        return tiles + Tile(nextId++, value, r, c)
    }

    private fun hasMove(tiles: List<Tile>): Boolean {
        if (tiles.size < SIZE * SIZE) return true
        val grid = arrayOfNulls<Int>(SIZE * SIZE)
        for (t in tiles) grid[t.row * SIZE + t.col] = t.value
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val v = grid[r * SIZE + c] ?: return true
            if (c + 1 < SIZE && grid[r * SIZE + c + 1] == v) return true
            if (r + 1 < SIZE && grid[(r + 1) * SIZE + c] == v) return true
        }
        return false
    }

    companion object {
        /** 棋盘为 SIZE x SIZE 个方块。 */
        const val SIZE = 4
    }
}
