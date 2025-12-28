package com.mmertnas.winnn.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmertnas.winnn.ai.QLearningAgent
import com.mmertnas.winnn.model.GameMode
import com.mmertnas.winnn.model.GameState
import com.mmertnas.winnn.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class GameViewModel : ViewModel() {
    private val agent = QLearningAgent()
    private val boardSize = 3 // Changed to 3x3

    var gameState by mutableStateOf(GameState(boardSize = boardSize))
        private set

    var trainingProgress by mutableStateOf(0f)
        private set

    var isTraining by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf("Mod Seçin")
        private set

    var currentEpsilon by mutableStateOf(0.0)
        private set

    var currentEpisode by mutableStateOf(0)
        private set

    var testResults by mutableStateOf("")
        private set

    var isTesting by mutableStateOf(false)
        private set

    var isGameScreenVisible by mutableStateOf(false)
        private set

    fun startGame(mode: GameMode) {
        agent.reset() // Reset agent for new mode
        gameState = GameState(boardSize = boardSize, gameMode = mode)
        isGameScreenVisible = true
        statusMessage = "Oyun Başladı: ${if (mode == GameMode.STANDARD) "Klasik" else "XOX"}"
        
        // If AI starts (optional logic, usually X starts)
        viewModelScope.launch { 
            checkAiTurn() 
        }
    }

    fun exitGame() {
        isGameScreenVisible = false
        isTraining = false
        statusMessage = "Mod Seçin"
    }

    fun onUserMove(index: Int) {
        if (gameState.isGameOver || gameState.board[index] != Player.NONE || isTraining) return

        makeMove(index)

        if (!gameState.isGameOver) {
            viewModelScope.launch {
                checkAiTurn()
            }
        }
    }

    private suspend fun checkAiTurn() {
        // Simple delay for UX
        // delay(500) 
        
        // If board is empty (AI starts), play randomly to ensure variety
        val isFirstMove = gameState.board.all { it == Player.NONE }
        val epsilon = if (isFirstMove) 0.9 else 0.05
        
        val aiMove = agent.getAction(gameState, epsilon = epsilon)
        if (aiMove != -1) {
            makeMove(aiMove)
        }
    }

    private fun makeMove(index: Int) {
        val currentPlayer = gameState.currentPlayer
        val newBoard = gameState.board.clone()
        newBoard[index] = currentPlayer

        val hasWon = checkWinner(newBoard, boardSize, gameState.gameMode)
        val isDraw = !hasWon && newBoard.none { it == Player.NONE }

        val winner = if (hasWon) currentPlayer else null

        gameState = gameState.copy(
            board = newBoard,
            currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X,
            winner = winner,
            isDraw = isDraw,
            isGameOver = hasWon || isDraw
        )

        updateStatus()
    }

    private fun updateStatus() {
        statusMessage = when {
            gameState.winner == Player.X -> "Yapay Zeka (X) Kazandı!"
            gameState.winner == Player.O -> "Sen (O) Kazandın!"
            gameState.isDraw -> "Berabere!"
            else -> if (gameState.currentPlayer == Player.X) "Sıra: Yapay Zeka" else "Sıra: Sen"
        }
    }

    fun resetGame() {
        gameState = GameState(boardSize = boardSize, currentPlayer = Player.X, gameMode = gameState.gameMode)
        updateStatus()
        
        // AI starts if it's AI's turn (Player.X is usually first)
        if (gameState.currentPlayer == Player.X) {
             viewModelScope.launch { checkAiTurn() }
        }
    }


    fun startTraining(iterations: Int = 10000) {
        if (isTraining) return
        viewModelScope.launch(Dispatchers.Default) {
            isTraining = true
            agent.reset()

            for (i in 1..iterations) {
                var currentState = GameState(boardSize = boardSize, gameMode = gameState.gameMode)
                // Decay epsilon
                val epsilon = max(0.05, 1.0 - (i.toDouble() / (iterations * 0.8)))

                while (!currentState.isGameOver) {
                    val currentPlayer = currentState.currentPlayer

                    val action = agent.getAction(currentState, epsilon)

                    if (action == -1) break 

                    val nextBoard = currentState.board.clone()
                    nextBoard[action] = currentPlayer
                    
                    val hasWon = checkWinner(nextBoard, boardSize, currentState.gameMode)
                    val isDraw = !hasWon && nextBoard.none { it == Player.NONE }
                    val winner = if (hasWon) currentPlayer else null

                    val nextState = GameState(
                        boardSize = boardSize,
                        board = nextBoard,
                        currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X,
                        winner = winner,
                        isDraw = isDraw,
                        isGameOver = hasWon || isDraw,
                        gameMode = currentState.gameMode
                    )

                    val reward = agent.calculateReward(nextState, currentPlayer)
                    
                    agent.updateQValue(currentState, action, reward, nextState)

                    currentState = nextState
                }

                if (i % 500 == 0) {
                    withContext(Dispatchers.Main) {
                        trainingProgress = i.toFloat() / iterations
                        currentEpsilon = epsilon
                        currentEpisode = i
                        statusMessage = "Eğitiliyor... %${(trainingProgress * 100).toInt()}"
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isTraining = false
                trainingProgress = 1f
                statusMessage = "Eğitim Bitti!"
                resetGame()
            }
        }
    }

    fun runPerformanceTest(iterations: Int = 5000) {
        if (isTraining || isTesting) return
        viewModelScope.launch(Dispatchers.Default) {
             withContext(Dispatchers.Main) {
                 isTesting = true
                 testResults = "Test Ediliyor..."
             }

             var wins = 0
             var losses = 0
             var draws = 0

             val testAgent = agent // Use current trained agent
             val mode = gameState.gameMode

             for (i in 0 until iterations) {
                 // Agent plays X (First), Random plays O
                 var simState = GameState(boardSize = boardSize, gameMode = mode)
                 
                 // If XOX, maybe different starting player? Standard logic usually assumes X starts.
                 // We will stick to X starts for consistency.

                 while (!simState.isGameOver) {
                     val cp = simState.currentPlayer
                     
                     var moveIndex = -1
                     if (cp == Player.X) {
                         // AI Turn (Greedy - exploitation only)
                         moveIndex = testAgent.getAction(simState, epsilon = 0.0)
                     } else {
                         // Random Opponent
                         val emptyIndices = simState.board.indices.filter { simState.board[it] == Player.NONE }
                         if (emptyIndices.isNotEmpty()) {
                             moveIndex = emptyIndices.random()
                         }
                     }

                     if (moveIndex != -1) {
                         val newBoard = simState.board.clone()
                         newBoard[moveIndex] = cp
                         val hasWon = checkWinner(newBoard, boardSize, mode)
                         val isDraw = !hasWon && newBoard.none { it == Player.NONE }
                         val winner = if (hasWon) cp else null
                         
                         simState = simState.copy(
                             board = newBoard, 
                             currentPlayer = if (cp == Player.X) Player.O else Player.X,
                             winner = winner,
                             isDraw = isDraw,
                             isGameOver = hasWon || isDraw
                         )
                     } else {
                         break // Should not happen
                     }
                 }

                 if (simState.winner == Player.X) wins++
                 else if (simState.winner == Player.O) losses++
                 else draws++
             }

             withContext(Dispatchers.Main) {
                 val winRate = (wins.toFloat() / iterations) * 100
                 testResults = "Sonuçlar ($iterations Oyun):\nKazanma: $wins (%${"%.1f".format(winRate)})\nKaybetme: $losses\nBeraberlik: $draws"
                 isTesting = false
             }
        }
    }
    
    /**
     * Checks if the *last move* resulted in a win.
     * Returns true if a winning pattern exists.
     * Logic assumes that if a pattern exists, the current player (who just moved) caused it.
     */
    private fun checkWinner(board: Array<Player>, size: Int, mode: GameMode): Boolean {
        return if (mode == GameMode.STANDARD) {
            checkStandardWin(board, size)
        } else {
            checkXOXWin(board, size)
        }
    }

    private fun checkStandardWin(board: Array<Player>, size: Int): Boolean {
        val lines = getAllLines(board, size)
        for (line in lines) {
            if (line[0] != Player.NONE && line.all { it == line[0] }) {
                return true
            }
        }
        return false
    }

    private fun checkXOXWin(board: Array<Player>, size: Int): Boolean {
        val lines = getAllLines(board, size)
        for (line in lines) {
             if (line.size == 3) {
                 // Check X-O-X
                 if (line[0] == Player.X && line[1] == Player.O && line[2] == Player.X) return true
                 // Check O-X-O
                 if (line[0] == Player.O && line[1] == Player.X && line[2] == Player.O) return true
             }
        }
        return false
    }

    private fun getAllLines(board: Array<Player>, size: Int): List<List<Player>> {
        val lines = mutableListOf<List<Player>>()
        // Rows
        for (r in 0 until size) lines.add((0 until size).map { c -> board[r * size + c] })
        // Cols
        for (c in 0 until size) lines.add((0 until size).map { r -> board[r * size + c] })
        // Diag 1
        lines.add((0 until size).map { i -> board[i * size + i] })
        // Diag 2
        lines.add((0 until size).map { i -> board[i * size + (size - 1 - i)] })
        return lines
    }
}
