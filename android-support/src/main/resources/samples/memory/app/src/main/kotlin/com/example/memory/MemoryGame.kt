package com.example.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 棋盘上的一张卡片：稳定的 [id]、正面的 [emoji]，以及它是否已翻起 / 已配对。 */
data class MemoryCard(
    val id: Int,
    val emoji: String,
    val faceUp: Boolean = false,
    val matched: Boolean = false,
)

/**
 * 记忆（配对）游戏的状态与规则。[newGame] 会以随机顺序发放 [PAIRS] 对、背面朝上的 emoji。
 * 翻起两张卡片算一次移动；配对成功则保持翻开，不匹配则短暂显示后翻回背面。
 * 当不匹配的卡片显示在屏幕上时，棋盘处于 [locked] 状态，且 [pendingMismatch] 保存那两张卡片，
 * 供 UI 在短暂延迟后隐藏。Compose 会观察这些由 [mutableStateOf] 支撑的属性并重组。
 */
class MemoryGameState {
    var cards by mutableStateOf(emptyList<MemoryCard>())
        private set
    var moves by mutableStateOf(0)
        private set
    var matchedPairs by mutableStateOf(0)
        private set
    var locked by mutableStateOf(false)
        private set
    var pendingMismatch by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    // 当前正面朝上且未配对的卡片的索引（0、1 或 2 张）。
    private val flipped = mutableListOf<Int>()

    val totalPairs: Int get() = cards.size / 2
    val isWon: Boolean get() = totalPairs > 0 && matchedPairs == totalPairs

    init {
        newGame()
    }

    /** 发一副洗好的新棋盘。 */
    fun newGame() {
        val chosen = EMOJIS.shuffled().take(PAIRS)
        cards = (chosen + chosen).shuffled().mapIndexed { index, emoji -> MemoryCard(id = index, emoji = emoji) }
        flipped.clear()
        moves = 0
        matchedPairs = 0
        locked = false
        pendingMismatch = null
    }

    /** 将 [index] 处的卡片翻到正面朝上。翻起本回合的第二张卡时，判定配对成功或触发不匹配。 */
    fun flip(index: Int) {
        if (locked) return
        val card = cards[index]
        if (card.faceUp || card.matched) return

        setFaceUp(index, true)
        flipped.add(index)
        if (flipped.size == 2) {
            moves++
            val first = flipped[0]
            val second = flipped[1]
            if (cards[first].emoji == cards[second].emoji) {
                setMatched(first)
                setMatched(second)
                matchedPairs++
                flipped.clear()
            } else {
                locked = true
                pendingMismatch = first to second
            }
        }
    }

    /** 把不匹配的那对卡片翻回背面并解锁棋盘。由 UI 在显示延迟结束后调用。 */
    fun hideMismatch() {
        val pair = pendingMismatch ?: return
        setFaceUp(pair.first, false)
        setFaceUp(pair.second, false)
        flipped.clear()
        pendingMismatch = null
        locked = false
    }

    private fun setFaceUp(index: Int, up: Boolean) {
        cards = cards.mapIndexed { i, card -> if (i == index) card.copy(faceUp = up) else card }
    }

    private fun setMatched(index: Int) {
        cards = cards.mapIndexed { i, card -> if (i == index) card.copy(matched = true, faceUp = true) else card }
    }

    companion object {
        /** 每副棋盘包含多少对（PAIRS * 2 = 16 张卡，即 4x4 网格）。 */
        const val PAIRS = 8

        val EMOJIS = listOf(
            "🍎", "🚀", "🎧", "🐱", "🌟", "🍕", "⚽", "🎸",
            "🌈", "🔥", "🍩", "🦄", "🎲", "🐙", "🌵", "🎯",
        )
    }
}
