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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    // Solo permite iniciar si el juego está en IDLE
                    detectTapGestures {
                        if (gameState == GameState.IDLE) onStartGame()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / GRID_SIZE

            // Dibuja el cuerpo
            snakeBody.drop(1).forEach {
                drawRect(
                    color = Color(0xFF4CAF50), // Verde
                    topLeft = Offset(it.first * cellSize, it.second * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }

            // Dibuja la cabeza
            snakeBody.firstOrNull()?.let { head ->
                drawRect(
                    color = Color(0xFF2E7D32), // Verde oscuro
                    topLeft = Offset(head.first * cellSize, head.second * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }

            // Dibuja la comida
            drawCircle(
                color = Color.Red,
                radius = cellSize / 2,
                center = Offset((food.first * cellSize) + (cellSize / 2), (food.second * cellSize) + (cellSize / 2))
            )
        }

        // Mensaje inicial para empezar
        if (gameState == GameState.IDLE) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Toca para Empezar",
                    style = MaterialTheme.typography.headlineMedium,
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
