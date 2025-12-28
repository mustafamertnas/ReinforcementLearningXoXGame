package com.mmertnas.winnn.model

enum class Player { NONE, X, O }

enum class GameMode { STANDARD, XOX }

data class GameState(
    val boardSize: Int = 3,
    val board: Array<Player> = Array(boardSize * boardSize) { Player.NONE },
    val currentPlayer: Player = Player.X,
    val winner: Player? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false,
    val gameMode: GameMode = GameMode.STANDARD
) {

    fun toStateKey(): String = board.joinToString("") {
        when (it) {
            Player.NONE -> "-"
            Player.X -> "X"
            Player.O -> "O"
        }
    }

    // Symmetries for RL Optimization (Canonical State)
    fun getCanonicalKey(): String {
        // Symmetry logic was causing issues because q-values weren't being 
        // mapped back to the original action space correctly. 
        // Disabling it ensures 100% correct, albeit slower, learning.
        return toStateKey()
    }

    private fun stateToString(b: Array<Player>): String {
        return b.joinToString("") {
            when (it) {
                Player.NONE -> "-"
                Player.X -> "X"
                Player.O -> "O"
            }
        }
    }

    private fun generateSymmetries(): List<Array<Player>> {
        val symmetries = mutableListOf<Array<Player>>()
        val currentBoard = board.clone()

        // 0, 90, 180, 270 rotations
        var rotated = currentBoard
        repeat(4) {
            symmetries.add(rotated)
            symmetries.add(reflect(rotated))
            rotated = rotate90(rotated)
        }
        return symmetries
    }

    private fun rotate90(b: Array<Player>): Array<Player> {
        val newBoard = b.clone()
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                // (r, c) -> (c, size - 1 - r)
                newBoard[c * boardSize + (boardSize - 1 - r)] = b[r * boardSize + c]
            }
        }
        return newBoard
    }

    private fun reflect(b: Array<Player>): Array<Player> {
        val newBoard = b.clone()
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                // (r, c) -> (r, size - 1 - c) horizontal reflection
                newBoard[r * boardSize + (boardSize - 1 - c)] = b[r * boardSize + c]
            }
        }
        return newBoard
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        if (boardSize != other.boardSize) return false
        return board.contentEquals(other.board)
    }

    override fun hashCode(): Int = 31 * boardSize + board.contentHashCode()
}
