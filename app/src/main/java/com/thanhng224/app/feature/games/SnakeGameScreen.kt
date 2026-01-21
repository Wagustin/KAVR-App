package com.thanhng224.app.feature.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.abs

@Suppress("DEPRECATION")
@Composable
fun SnakeGameScreen(
    navController: NavController, // Parámetro añadido para la navegación
    viewModel: SnakeViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val snakeBody by viewModel.snakeBody.collectAsState()
    val food by viewModel.food.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Marcadores ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: $score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "High Score: $highScore",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Tablero de Juego ---
        SnakeBoard(
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            onDirectionChange = { viewModel.changeDirection(it) },
            onStartGame = { viewModel.startGame() }
        )
    }

    // --- POPUP FINAL (Game Over / Win) ---
    if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
        FinalScoreDialog(
            gameState = gameState,
            finalScore = score,
            onRestart = { viewModel.startGame() },
            onExit = { navController.popBackStack() } // Usa el NavController
        )
    }
}

@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    onDirectionChange: (Direction) -> Unit,
    onStartGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp)) // Rounded board
            .background(Color(0xFF81C784)) // Base grass color
            .pointerInput(gameState) {
                if (gameState == GameState.PLAYING) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        when {
                            abs(x) > abs(y) -> if (x > 0) onDirectionChange(Direction.RIGHT) else onDirectionChange(Direction.LEFT)
                            else -> if (y > 0) onDirectionChange(Direction.DOWN) else onDirectionChange(Direction.UP)
                        }
                    }
                } else {
                    detectTapGestures {
                        if (gameState == GameState.IDLE) onStartGame()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / GRID_SIZE
            val cellPx = size.width / GRID_SIZE

            // 1. CHECKERBOARD BACKGROUND
            val darkGrass = Color(0xFF66BB6A)
            val lightGrass = Color(0xFF81C784)
            
            for (i in 0 until GRID_SIZE) {
                for (j in 0 until GRID_SIZE) {
                    val color = if ((i + j) % 2 == 0) lightGrass else darkGrass
                    drawRect(
                        color = color,
                        topLeft = Offset(i * cellPx, j * cellPx),
                        size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
                    )
                }
            }

            // 2. SNAKE BODY
            // Draw from tail to head
            snakeBody.asReversed().forEachIndexed { index, point ->
                val isHead = index == snakeBody.lastIndex
                val color = if (isHead) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                
                val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                val center = Offset(topLeft.x + cellPx/2, topLeft.y + cellPx/2)
                
                if (isHead) {
                    // Head shape (Rounded)
                    drawRoundRect(
                        color = color,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(cellPx, cellPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellPx/2, cellPx/2)
                    )
                    
                    // Eyes (White + Black pupil)
                    // Determine direction based on previous body part or default?
                    // Simplified: Just put two eyes on top
                    drawCircle(Color.White, radius = cellPx * 0.15f, center = Offset(center.x - cellPx*0.2f, center.y - cellPx*0.1f))
                    drawCircle(Color.White, radius = cellPx * 0.15f, center = Offset(center.x + cellPx*0.2f, center.y - cellPx*0.1f))
                    drawCircle(Color.Black, radius = cellPx * 0.07f, center = Offset(center.x - cellPx*0.2f, center.y - cellPx*0.1f))
                    drawCircle(Color.Black, radius = cellPx * 0.07f, center = Offset(center.x + cellPx*0.2f, center.y - cellPx*0.1f))
                    
                } else {
                    // Body segment (Slightly smaller for style)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                        size = androidx.compose.ui.geometry.Size(cellPx - 4f, cellPx - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }

            // 3. FOOD (Apple)
            val foodCenter = Offset((food.first * cellPx) + (cellPx / 2), (food.second * cellPx) + (cellPx / 2))
            drawCircle(
                color = Color(0xFFE53935), // Red Apple
                radius = cellPx * 0.4f,
                center = foodCenter
            )
            // Leaf
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = cellPx * 0.15f,
                center = Offset(foodCenter.x + cellPx*0.2f, foodCenter.y - cellPx*0.3f)
            )
        }

        // INITIAL MESSAGE
        if (gameState == GameState.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TAP TO START",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FinalScoreDialog(
    gameState: GameState,
    finalScore: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* No se puede cerrar tocando fuera */ },
        title = {
            Text(
                text = if (gameState == GameState.WON) "¡Ganaste!" else "Fin del Juego",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Puntaje final: $finalScore",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("Reiniciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("Salir")
            }
        }
    )
}
