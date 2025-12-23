package com.thanhng224.app.feature.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.abs

@Suppress("DEPRECATION")
@Composable
fun SnakeGameScreen(viewModel: SnakeViewModel = hiltViewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val snakeBody by viewModel.snakeBody.collectAsState()
    val food by viewModel.food.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState(initial = 0)

    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
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

        SnakeBoard(
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            onDirectionChange = { viewModel.changeDirection(it) },
            onGameAction = { viewModel.startGame() }
        )
    }
}

@Composable
fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    onDirectionChange: (Direction) -> Unit,
    onGameAction: () -> Unit
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
                            abs(x) > abs(y) -> {
                                if (x > 0) onDirectionChange(Direction.RIGHT) else onDirectionChange(Direction.LEFT)
                            }
                            else -> {
                                if (y > 0) onDirectionChange(Direction.DOWN) else onDirectionChange(Direction.UP)
                            }
                        }
                    }
                } else {
                    detectTapGestures { onGameAction() }
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
                    color = Color(0xFF2E7D32), // Verde oscuro para cabeza
                    topLeft = Offset(head.first * cellSize, head.second * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }

            // Dibuja la comida
            drawCircle(
                color = Color.Red,
                radius = cellSize / 2,
                center = Offset(
                    (food.first * cellSize) + (cellSize / 2),
                    (food.second * cellSize) + (cellSize / 2)
                )
            )
        }
        
        if (gameState != GameState.PLAYING){
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                val text = if (gameState == GameState.GAME_OVER) "¡Has perdido!\nToca para reiniciar" else "Toca para Empezar"
                 Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
             }
        }
    }
}
