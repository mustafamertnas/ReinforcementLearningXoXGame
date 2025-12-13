package com.mmertnas.winnn
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random


enum class Player { NONE, X, O }

data class GameState(
    val board: Array<Player> = Array(9) { Player.NONE },
    val currentPlayer: Player = Player.X,
    val winner: Player? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false
) {

    fun toStateKey(): String = board.joinToString("") {
        when (it) {
            Player.NONE -> "-"
            Player.X -> "X"
            Player.O -> "O"
        }
    }

    // HashCode ve Equals override edilmesi performans için önemlidir
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        return board.contentEquals(other.board)
    }
    override fun hashCode(): Int = board.contentHashCode()
}

class QLearningAgent {
    // StateKey -> { ActionIndex -> QValue }
    private val qTable = HashMap<String, MutableMap<Int, Double>>()

    private val alpha = 0.5
    private val gamma = 0.95


    fun getAction(state: GameState, epsilon: Double): Int {
        val availableMoves = state.board.indices.filter { state.board[it] == Player.NONE }
        if (availableMoves.isEmpty()) return -1


        if (Random.nextDouble() < epsilon) {
            return availableMoves.random()
        }

        val stateKey = state.toStateKey()
        val actions = qTable.getOrDefault(stateKey, mutableMapOf())


        if (actions.isEmpty()) return availableMoves.random()

        return availableMoves.shuffled().maxByOrNull { actions[it] ?: 0.0 } ?: availableMoves.random()
    }


    fun updateQValue(state: GameState, action: Int, reward: Double, nextState: GameState) {
        val stateKey = state.toStateKey()
        val nextStateKey = nextState.toStateKey()

        val currentQ = qTable.getOrPut(stateKey) { mutableMapOf() }.getOrDefault(action, 0.0)


        val maxNextQ = if (nextState.isGameOver) 0.0 else {
            qTable[nextStateKey]?.values?.maxOrNull() ?: 0.0
        }


        val newQ = currentQ + alpha * (reward + (gamma * maxNextQ) - currentQ)

        qTable.getOrPut(stateKey) { mutableMapOf() }[action] = newQ
    }


    fun reset() {
        qTable.clear()
    }
}


class GameViewModel : ViewModel() {
    private val agent = QLearningAgent()

    var gameState by mutableStateOf(GameState())
        private set

    var trainingProgress by mutableStateOf(0f)
        private set

    var isTraining by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf("Başlamak için oyunu başlatın")
        private set


    fun onUserMove(index: Int) {
        if (gameState.isGameOver || gameState.board[index] != Player.NONE || isTraining) return


        makeMove(index)


        if (!gameState.isGameOver) {
            viewModelScope.launch {

                val aiMove = agent.getAction(gameState, epsilon = 0.0)
                if (aiMove != -1) {
                    makeMove(aiMove)
                }
            }
        }
    }

    private fun makeMove(index: Int) {
        val currentPlayer = gameState.currentPlayer
        val newBoard = gameState.board.clone()
        newBoard[index] = currentPlayer

        val (winner, isDraw) = checkWinner(newBoard)
        val isGameOver = winner != null || isDraw

        gameState = gameState.copy(
            board = newBoard,
            currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X,
            winner = winner,
            isDraw = isDraw,
            isGameOver = isGameOver
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
        gameState = GameState(currentPlayer = Player.X)
        updateStatus()


        if (gameState.currentPlayer == Player.X) {
            val aiMove = agent.getAction(gameState, epsilon = 0.0)
            if (aiMove != -1) makeMove(aiMove)
        }
    }


    fun startTraining(iterations: Int = 50000) {
        viewModelScope.launch(Dispatchers.Default) {
            isTraining = true
            agent.reset()

            for (i in 1..iterations) {
                var currentState = GameState()

                val epsilon = max(0.05, 1.0 - (i.toDouble() / (iterations * 0.8)))

                while (!currentState.isGameOver) {
                    val currentPlayer = currentState.currentPlayer

                    val action = agent.getAction(currentState, epsilon)


                    val nextBoard = currentState.board.clone()
                    nextBoard[action] = currentPlayer
                    val (winner, isDraw) = checkWinner(nextBoard)

                    val nextState = GameState(
                        board = nextBoard,
                        currentPlayer = if (currentPlayer == Player.X) Player.O else Player.X,
                        winner = winner,
                        isDraw = isDraw,
                        isGameOver = winner != null || isDraw
                    )

                    var reward = 0.0
                    if (nextState.winner == currentPlayer) {
                        reward = 10.0
                    } else if (nextState.winner != null) {
                        reward = -10.0
                    } else if (nextState.isDraw) {
                        reward = 2.0
                    } else {
                        reward = -0.1
                    }


                    agent.updateQValue(currentState, action, reward, nextState)



                    currentState = nextState
                }

                if (i % 500 == 0) {
                    withContext(Dispatchers.Main) {
                        trainingProgress = i.toFloat() / iterations
                        statusMessage = "Eğitiliyor... %${(trainingProgress * 100).toInt()}"
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isTraining = false
                trainingProgress = 1f
                statusMessage = "Eğitim Bitti! Artık yenilmez."
                resetGame()
            }
        }
    }

    // Yardımcı: Kazanan kontrolü
    private fun checkWinner(board: Array<Player>): Pair<Player?, Boolean> {
        val winPatterns = listOf(
            listOf(0,1,2), listOf(3,4,5), listOf(6,7,8),
            listOf(0,3,6), listOf(1,4,7), listOf(2,5,8),
            listOf(0,4,8), listOf(2,4,6)
        )
        for (p in winPatterns) {
            if (board[p[0]] != Player.NONE && board[p[0]] == board[p[1]] && board[p[0]] == board[p[2]]) {
                return board[p[0]] to false
            }
        }
        return null to board.none { it == Player.NONE }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TicTacToeScreen()
                }
            }
        }
    }
}

@Composable
fun TicTacToeScreen(viewModel: GameViewModel = viewModel()) {
    val state = viewModel.gameState

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Yapay Zeka XOX",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))


        Text(
            text = viewModel.statusMessage,
            style = MaterialTheme.typography.titleMedium,
            color = if (viewModel.isTraining) Color.Red else Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.isTraining) {
            LinearProgressIndicator(
                progress = { viewModel.trainingProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))


        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.Black)
                .padding(4.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0..2) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cellValue = state.board[index]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .background(Color.White)
                                    .clickable { viewModel.onUserMove(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (cellValue) {
                                        Player.X -> "X"
                                        Player.O -> "O"
                                        else -> ""
                                    },
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (cellValue) {
                                        Player.X -> Color(0xFFD32F2F) // Kırmızı
                                        Player.O -> Color(0xFF1976D2) // Mavi
                                        else -> Color.Black
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.startTraining(50000) },
                enabled = !viewModel.isTraining,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Eğit (50k)")
            }

            Button(
                onClick = { viewModel.resetGame() },
                enabled = !viewModel.isTraining,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sıfırla")
            }
        }
    }
}