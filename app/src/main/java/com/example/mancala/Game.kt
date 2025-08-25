package com.example.mancala

// Pick 5–6 emojis; you can change these and it still works.
val stoneSymbols = listOf("\uD83D\uDC27", "💎", "\uD83D\uDC0B", "🦑", "\uD83E\uDD95", "\uD83D\uDC20")

data class Stone(val symbol: String = stoneSymbols.random())

enum class Player { A, B; fun opponent() = if (this == A) B else A }

/**
 * Pits 0..5  -> Player A side
 * Pit  6     -> Player A store
 * Pits 7..12 -> Player B side
 * Pit  13    -> Player B store
 */
data class GameState(
    val pits: List<MutableList<Stone>>,
    val currentPlayer: Player,
    val extraTurn: Boolean = false
) {
    // Game over when one side is empty
    val isGameOver: Boolean
        get() = (0..5).all { pits[it].isEmpty() } || (7..12).all { pits[it].isEmpty() }

    // Safe copy helper (avoid name 'copy' to not confuse with data class copy)
    fun copyWith(
        pits: List<MutableList<Stone>> = this.pits.map { it.toMutableList() },
        currentPlayer: Player = this.currentPlayer,
        extraTurn: Boolean = this.extraTurn
    ): GameState = GameState(pits, currentPlayer, extraTurn)

    /**
     * Apply rules AFTER sowing has visually happened:
     * - extra turn if last stone landed in your store
     * - capture if last stone lands in an empty pit on your side
     * - end game sweep when a side is empty
     */
    fun afterAnimatedSowing(newPits: List<MutableList<Stone>>, lastIndex: Int): GameState {
        val pits = newPits.map { it.toMutableList() }.toMutableList()

        val myStore = if (currentPlayer == Player.A) 6 else 13
        val extra = lastIndex == myStore

        // Capture (only if not extra turn)
        if (!extra) {
            val mySide = if (currentPlayer == Player.A) (0..5) else (7..12)
            if (lastIndex in mySide && pits[lastIndex].size == 1) {
                val opposite = 12 - lastIndex
                if (pits[opposite].isNotEmpty()) {
                    // Android-safe: do not call List.removeLast() (Java 21 default).
                    val lastStone = pits[lastIndex].removeAt(pits[lastIndex].lastIndex)
                    val captured = pits[opposite].toList()
                    pits[opposite].clear()
                    pits[myStore].add(lastStone)
                    pits[myStore].addAll(captured)
                }
            }
        }

        // End game sweep if one side is empty
        val aEmpty = (0..5).all { pits[it].isEmpty() }
        val bEmpty = (7..12).all { pits[it].isEmpty() }
        if (aEmpty || bEmpty) {
            for (i in 0..5) { pits[6].addAll(pits[i]); pits[i].clear() }
            for (i in 7..12) { pits[13].addAll(pits[i]); pits[i].clear() }
            return GameState(pits, currentPlayer, extraTurn = false)
        }

        val next = if (extra) currentPlayer else currentPlayer.opponent()
        return GameState(pits, next, extraTurn = extra)
    }

    fun endMessage(): String {
        val a = pits[6].size
        val b = pits[13].size
        return when {
            a > b -> "Game over. Player A wins $a–$b!"
            b > a -> "Game over. Player B wins $b–$a!"
            else -> "Game over. It's a draw $a–$b!"
        }
    }

    fun withHintMove(): GameState = this

    companion object {
        fun initial(stonesPerPit: Int = 4): GameState {
            val pits = List(14) { mutableListOf<Stone>() }
            for (i in 0..5) repeat(stonesPerPit) { pits[i].add(Stone()) }
            for (i in 7..12) repeat(stonesPerPit) { pits[i].add(Stone()) }
            return GameState(pits, Player.A)
        }
    }
}
