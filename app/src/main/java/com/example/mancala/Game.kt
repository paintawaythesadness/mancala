package com.example.mancala

enum class Player { A, B }

data class GameState(
    val pits: IntArray,
    val currentPlayer: Player,
    val isGameOver: Boolean = false,
    val extraTurn: Boolean = false,
) {
    companion object {
        fun initial(stonesPerPit: Int = 4): GameState {
            val pits = IntArray(14)
            for (i in 0..5) pits[i] = stonesPerPit    // A pits
            for (i in 7..12) pits[i] = stonesPerPit   // B pits
            return GameState(pits = pits, currentPlayer = Player.A)
        }
    }
}

/**
 * Apply post-sowing rules (capture, endgame) after we visually dropped stones.
 * Accepts the pits state after sowing and the index of the last drop.
 */
fun GameState.afterAnimatedSowing(animatedPits: IntArray, lastIndex: Int): GameState {
    if (isGameOver) return this
    val pits = animatedPits.copyOf()

    val myStore = if (currentPlayer == Player.A) 6 else 13
    val oppStore = if (currentPlayer == Player.A) 13 else 6

    // Determine next player based on last index landing in my store
    var nextPlayer = if (lastIndex == myStore) currentPlayer else currentPlayer.opponent()
    var gotExtra = lastIndex == myStore

    // Capture rule: if last stone lands in an empty pit on player's side, capture opposite
    val isMySide = if (currentPlayer == Player.A) lastIndex in 0..5 else lastIndex in 7..12
    if (!gotExtra && isMySide && pits[lastIndex] == 1) {
        val opposite = 12 - lastIndex
        val captured = pits[opposite]
        if (captured > 0) {
            pits[myStore] += captured + 1
            pits[lastIndex] = 0
            pits[opposite] = 0
        }
    }

    // End game check
    val aEmpty = (0..5).all { pits[it] == 0 }
    val bEmpty = (7..12).all { pits[it] == 0 }
    var gameOver = false
    if (aEmpty || bEmpty) {
        pits[6] += (0..5).sumOf { pits[it] }
        pits[13] += (7..12).sumOf { pits[it] }
        for (i in 0..5) pits[i] = 0
        for (i in 7..12) pits[i] = 0
        gameOver = true
        nextPlayer = currentPlayer
        gotExtra = false
    }

    return copy(pits = pits, currentPlayer = nextPlayer, isGameOver = gameOver, extraTurn = gotExtra)
}

fun GameState.makeMove(startPit: Int): GameState {
    if (isGameOver) return this
    val pits = this.pits.copyOf()
    var stones = pits[startPit]
    pits[startPit] = 0

    val myStore = if (currentPlayer == Player.A) 6 else 13
    val oppStore = if (currentPlayer == Player.A) 13 else 6

    var index = startPit
    while (stones > 0) {
        index = (index + 1) % 14
        if (index == oppStore) continue
        pits[index]++
        stones--
    }

    var nextPlayer = if (index == myStore) currentPlayer else currentPlayer.opponent()
    var gotExtra = index == myStore

    val isMySide = if (currentPlayer == Player.A) index in 0..5 else index in 7..12
    if (isMySide && pits[index] == 1) {
        val opposite = 12 - index
        val captured = pits[opposite]
        if (captured > 0) {
            pits[myStore] += captured + 1
            pits[index] = 0
            pits[opposite] = 0
        }
    }

    val aEmpty = (0..5).all { pits[it] == 0 }
    val bEmpty = (7..12).all { pits[it] == 0 }
    var gameOver = false
    if (aEmpty || bEmpty) {
        pits[6] += (0..5).sumOf { pits[it] }
        pits[13] += (7..12).sumOf { pits[it] }
        for (i in 0..5) pits[i] = 0
        for (i in 7..12) pits[i] = 0
        gameOver = true
        nextPlayer = currentPlayer
        gotExtra = false
    }

    return copy(pits = pits, currentPlayer = nextPlayer, isGameOver = gameOver, extraTurn = gotExtra)
}

fun GameState.withHintMove(): GameState {
    if (isGameOver) return this
    val range = if (currentPlayer == Player.A) 0..5 else 7..12
    val best = range.filter { pits[it] > 0 }.maxByOrNull { simulateScoreGain(it) }
    return if (best != null) makeMove(best) else this
}

private fun GameState.simulateScoreGain(move: Int): Int {
    val after = this.makeMove(move)
    val myStore = if (currentPlayer == Player.A) 6 else 13
    return after.pits[myStore] - this.pits[myStore]
}

fun GameState.endMessage(): String {
    val a = pits[6]
    val b = pits[13]
    return when {
        a > b -> "Game over. Player A wins $a–$b!"
        b > a -> "Game over. Player B wins $b–$a!"
        else -> "Game over. It's a draw $a–$b!"
    }
}

private fun Player.opponent(): Player = if (this == Player.A) Player.B else Player.A
