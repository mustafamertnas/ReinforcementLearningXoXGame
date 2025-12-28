package com.mmertnas.winnn

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmertnas.winnn.model.GameMode
import com.mmertnas.winnn.model.Player
import com.mmertnas.winnn.viewmodel.GameViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC5),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: GameViewModel = viewModel()) {
    if (viewModel.isGameScreenVisible) {
        TicTacToeScreen(viewModel)
    } else {
        MenuScreen(viewModel)
    }
}

@Composable
fun MenuScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pekiştirmeli Öğrenme İle\nXOX ve XXX Oyunu",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(
            text = "Klasik (Standard)",
            subText = "3'lü Eşleştirme (XXX - OOO)",
            color = Color(0xFF1976D2),
            onClick = { viewModel.startGame(GameMode.STANDARD) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        MenuButton(
            text = "XOX Modu",
            subText = "Desen Oluşturma (X-O-X)",
            color = Color(0xFFD32F2F),
            onClick = { viewModel.startGame(GameMode.XOX) }
        )
    }
}

@Composable
fun MenuButton(text: String, subText: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TicTacToeScreen(viewModel: GameViewModel) {
    val state = viewModel.gameState
    val boardSize = state.boardSize

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.exitGame() },
                modifier = Modifier.background(Color.White, CircleShape).shadow(4.dp, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (state.gameMode == GameMode.STANDARD) "Klasik Mod" else "XOX Modu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = viewModel.statusMessage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isTraining) Color(0xFFD32F2F) else Color(0xFF1976D2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                AnimatedVisibility(visible = viewModel.isTraining) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { viewModel.trainingProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bölüm: ${viewModel.currentEpisode}", style = MaterialTheme.typography.bodySmall)
                            Text("Keşif (Epsilon): ${String.format(Locale.US, "%.3f", viewModel.currentEpsilon)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Game Board
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (row in 0 until boardSize) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until boardSize) {
                            val index = row * boardSize + col
                            val cellValue = state.board[index]
                            
                            GameCell(cellValue = cellValue) {
                                viewModel.onUserMove(index)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.startTraining(50000) },
                enabled = !viewModel.isTraining,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eğit (50k)")
            }

            Button(
                onClick = { viewModel.runPerformanceTest(5000) },
                enabled = !viewModel.isTraining && !viewModel.isTesting,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)) // Amber
            ) {
                Text("Test (5k)")
            }

            Button(
                onClick = { viewModel.resetGame() },
                enabled = !viewModel.isTraining,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
//                Spacer(modifier = Modifier.width(8.dp))
                // Text("Sıfırla", color = Color.Black) // Shortened for space
            }
        }
        
        if (viewModel.testResults.isNotEmpty()) {
             Spacer(modifier = Modifier.height(16.dp))
             Card(
                 modifier = Modifier.fillMaxWidth(),
                 colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)) // Light Amber
             ) {
                 Text(
                     text = viewModel.testResults,
                     modifier = Modifier.padding(16.dp),
                     style = MaterialTheme.typography.bodyMedium,
                     color = Color.Black,
                     textAlign = TextAlign.Center
                 )
             }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GameCell(cellValue: Player, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEEEEE))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (cellValue) {
                Player.X -> "X"
                Player.O -> "O"
                else -> ""
            },
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when (cellValue) {
                Player.X -> Color(0xFFD32F2F)
                Player.O -> Color(0xFF1976D2)
                else -> Color.Transparent
            }
        )
    }
}