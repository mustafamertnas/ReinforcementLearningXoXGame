package com.mmertnas.winnn.ai

import com.mmertnas.winnn.model.GameState
import com.mmertnas.winnn.model.Player
import kotlin.collections.set
import kotlin.random.Random

class QLearningAgent {

    private val qTable = HashMap<String, MutableMap<Int, Double>>()

    private val alpha = 0.5
    private val gamma = 0.95


    fun getAction(state: GameState, epsilon: Double): Int {
        val availableMoves = state.board.indices.filter { state.board[it] == Player.NONE }
        if (availableMoves.isEmpty()) return -1


        if (Random.nextDouble() < epsilon) {
            return availableMoves.random()
        }

        // Use Canonical Key for Symmetry Reduction
        val stateKey = state.getCanonicalKey()
        val actions = qTable.getOrDefault(stateKey, mutableMapOf())


        if (actions.isEmpty()) return availableMoves.random()

        return availableMoves.shuffled().maxByOrNull { actions[it] ?: 0.0 } ?: availableMoves.random()
    }


    fun updateQValue(state: GameState, action: Int, reward: Double, nextState: GameState) {
        val stateKey = state.getCanonicalKey()
        val nextStateKey = nextState.getCanonicalKey()

        val currentQ = qTable.getOrPut(stateKey) { mutableMapOf() }.getOrDefault(action, 0.0)


        val maxNextQ = if (nextState.isGameOver) 0.0 else {
            qTable[nextStateKey]?.values?.maxOrNull() ?: 0.0
        }

        val newQ = currentQ + alpha * (reward + (gamma * maxNextQ) - currentQ)

        qTable.getOrPut(stateKey) { mutableMapOf() }[action] = newQ
    }

    // Helper to calculate smarter rewards (Reward Shaping)
    fun calculateReward(stateAfter: GameState, player: Player): Double {
        if (stateAfter.winner == player) return 10.0
        if (stateAfter.winner != null) return -10.0
        if (stateAfter.isDraw) return 0.5 // Draw is neutral/slightly positive to avoid losing

        var reward = 0.0

        if (stateAfter.gameMode == com.mmertnas.winnn.model.GameMode.STANDARD) {
             // Reward for creating a threat (2 in a row with 1 empty)
            if (hasLineOf2(stateAfter.board, stateAfter.boardSize, player)) {
                reward += 0.2
            }
        } else {
             // XOX Mode: purely terminal rewards are safer to avoid reward hacking.
             // The state space of 3x3 is small enough (approx 5k states) that 50k episodes 
             // will solve it perfectly without shaping.
             // Shaping often introduces bugs or local optima if not careful.
        }
        
        return reward
    }

    private fun hasLineOf2(board: Array<Player>, size: Int, player: Player): Boolean {
        // Check for 2 of 'player' and 1 empty in a line (Threat)
        val lines = getAllLines(board, size)
        for (line in lines) {
            val countP = line.count { it == player }
            val countE = line.count { it == Player.NONE }
            // For 3x3: Size is 3. Threat is 2 P and 1 Empty.
            if (countP == size - 1 && countE == 1) return true
        }
        return false
    }

    private fun getAllLines(board: Array<Player>, size: Int): List<List<Player>> {
        val lines = mutableListOf<List<Player>>()
        // Rows
        for (r in 0 until size) {
            lines.add((0 until size).map { c -> board[r * size + c] })
        }
        // Cols
        for (c in 0 until size) {
            lines.add((0 until size).map { r -> board[r * size + c] })
        }
        // Diag 1
        lines.add((0 until size).map { i -> board[i * size + i] })
        // Diag 2
        lines.add((0 until size).map { i -> board[i * size + (size - 1 - i)] })
        return lines
    }


    fun reset() {
        qTable.clear()
    }
}
