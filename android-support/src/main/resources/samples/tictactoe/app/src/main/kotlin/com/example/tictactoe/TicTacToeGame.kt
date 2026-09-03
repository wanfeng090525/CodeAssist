package com.example.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 两个玩家，各自携带其绘制的符号。 */
enum class Player(val symbol: String) { X("X"), O("O") }

/**
 * 3x3 井字棋的棋盘状态与规则。九个 [cells]（按行优先，`null` = 空）是一个快照状态列表，
 * 因此写入单个格子即可重组棋盘；各玩家累计的获胜次数在 [resetScores] 之前会跨轮保持。
 * Compose 会观察这些由 [mutableStateOf] 支撑的属性并重组。
 */
class TicTacToeState {
    val cells = mutableStateListOf<Player?>(null, null, null, null, null, null, null, null, null)

    var current by mutableStateOf(Player.X)
        private set
    var winner by mutableStateOf<Player?>(null)
        private set
    var winningLine by mutableStateOf<List<Int>?>(null)
        private set
    var xWins by mutableStateOf(0)
        private set
    var oWins by mutableStateOf(0)
        private set

    val isDraw: Boolean get() = winner == null && cells.all { it != null }
    val isOver: Boolean get() = winner != null || isDraw

    /** 在当前玩家 [index] 处落子；如果该格已被占用或本轮已结束则忽略。 */
    fun play(index: Int) {
        if (isOver || cells[index] != null) return
        cells[index] = current
        val line = winningLineFor(current)
        if (line != null) {
            winner = current
            winningLine = line
            if (current == Player.X) xWins++ else oWins++
        } else {
            current = if (current == Player.X) Player.O else Player.X
        }
    }

    /** 清空棋盘开始新一轮，同时保留比分。始终由 X 先手。 */
    fun newRound() {
        for (i in cells.indices) cells[i] = null
        winner = null
        winningLine = null
        current = Player.X
    }

    /** 重置计分板并开始崭新的一轮。 */
    fun resetScores() {
        xWins = 0
        oWins = 0
        newRound()
    }

    private fun winningLineFor(player: Player): List<Int>? =
        LINES.firstOrNull { line -> line.all { cells[it] == player } }

    companion object {
        /** 八条获胜连线：三行、三列、两条对角线。 */
        val LINES = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6),
        )
    }
}
